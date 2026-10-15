package org.moa.trip.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moa.global.handler.BusinessException;
import org.moa.global.handler.FileUploadException;
import org.moa.global.service.FirebaseStorageService;
import org.moa.trip.dto.record.*;
import org.moa.trip.entity.TripRecord;
import org.moa.trip.entity.TripRecordImage;
import org.moa.trip.mapper.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Transaction lifecycle simulation only; real JDBC assertions live in TripRecordImageMysqlTest. */
class TripRecordImageConsistencyTest {
    TripRecordMapper records;
    TripRecordImageMapper images;
    TripMemberMapper members;
    FirebaseStorageService storage;
    RecordingTransactions transactions;
    TripRecordServiceImpl service;
    TripRecord original;

    @BeforeEach
    void setUp() throws Exception {
        records = mock(TripRecordMapper.class);
        images = mock(TripRecordImageMapper.class);
        members = mock(TripMemberMapper.class);
        storage = mock(FirebaseStorageService.class);
        transactions = new RecordingTransactions();
        service = service(Runnable::run);
        original = TripRecord.builder().recordId(10L).tripId(1L).memberId(2L)
                .title("before").recordDate(LocalDate.of(2026, 1, 1)).content("content").build();
        when(members.isMemberOfTrip(1L, 2L)).thenReturn(1);
        when(records.findRecordByTripIdAndRecordId(1L, 10L)).thenReturn(original);
        when(records.findRecordForUpdate(1L, 10L)).thenReturn(original);
        when(records.findRecordById(anyLong())).thenReturn(original);
        when(records.findImageUrlsByRecordId(10L)).thenReturn(List.of("old.jpg"));
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            ((TripRecord) invocation.getArgument(0)).setRecordId(10L);
            return null;
        }).when(records).insertTripRecord(any());
        when(images.insertTripRecordImages(anyList())).thenAnswer(invocation -> {
            return ((List<?>) invocation.getArgument(0)).size();
        });
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "new.jpg";
        });
        when(storage.deleteFile(anyString())).thenReturn(true);
    }

    TripRecordServiceImpl service(Executor executor) {
        return new TripRecordServiceImpl(records, images, members, storage, executor, transactions);
    }

    MockMultipartFile file(String name) {
        return new MockMultipartFile("image", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    TripRecordRequestDto create(org.springframework.web.multipart.MultipartFile... files) {
        TripRecordRequestDto dto = new TripRecordRequestDto();
        dto.setTitle("title"); dto.setRecordDate(LocalDate.of(2026, 1, 1));
        dto.setImageUrls(Arrays.asList(files));
        return dto;
    }

    TripRecordUpdateRequestDto update(List<String> keep, boolean upload) {
        TripRecordUpdateRequestDto dto = new TripRecordUpdateRequestDto();
        dto.setTitle("after"); dto.setContent("after"); dto.setRecordDate(original.getRecordDate());
        dto.setExistingImageFileNames(keep);
        dto.setNewImages(upload ? List.of(file("new.jpg")) : List.of());
        return dto;
    }

    @Test
    void uploadsThenInsertsOnceOnCallingThread() throws Exception {
        AtomicInteger names = new AtomicInteger();
        Thread caller = Thread.currentThread();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            service = service(pool);
            when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(i -> {
                assertThat(Thread.currentThread()).isNotSameAs(caller);
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                return "new" + names.incrementAndGet() + ".jpg";
            });
            when(images.insertTripRecordImages(anyList())).thenAnswer(i -> {
                assertThat(Thread.currentThread()).isSameAs(caller);
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                List<TripRecordImage> rows = i.getArgument(0);
                assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row.getRecordId()).isEqualTo(10L));
                assertThat(rows).extracting(TripRecordImage::getImageUrl).containsExactlyInAnyOrder("new1.jpg", "new2.jpg");
                return 2;
            });
            assertThat(service.createRecord(1L, 2L, create(file("a.jpg"), file("b.jpg"))).getRecordId()).isEqualTo(10L);
            verify(images).insertTripRecordImages(anyList());
            verify(images, never()).insertTripRecordImage(any());
            verify(storage, never()).deleteFile(anyString());
            assertThat(transactions.events).containsExactly("begin", "commit");
        } finally { pool.shutdownNow(); }
    }

    @Test
    void partialFailureWaitsForEveryUploadBeforeCleanup() throws Exception {
        CountDownLatch slowStarted = new CountDownLatch(1), failed = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2), caller = Executors.newSingleThreadExecutor();
        try {
            service = service(pool);
            when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(i -> {
                byte[] bytes = i.getArgument(0);
                if (bytes[0] == 9) {
                    assertThat(slowStarted.await(5, TimeUnit.SECONDS)).isTrue();
                    failed.countDown();
                    throw new IOException("upload failed");
                }
                slowStarted.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                return "late-success.jpg";
            });
            MockMultipartFile bad = new MockMultipartFile("image", "bad.jpg", "image/jpeg", new byte[]{9});
            Future<?> result = caller.submit(() -> service.createRecord(1L, 2L, create(file("slow.jpg"), bad)));
            assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(result.isDone()).isFalse();
            verify(storage, never()).deleteFile(anyString());
            release.countDown();
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(FileUploadException.class);
            verify(storage).deleteFile("late-success.jpg");
            verify(records, never()).insertTripRecord(any());
            assertThat(transactions.events).isEmpty();
        } finally { release.countDown(); pool.shutdownNow(); caller.shutdownNow(); }
    }

    @Test
    void executorRejectionAlsoWaitsForAcceptedUpload() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), rejected = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor(), caller = Executors.newSingleThreadExecutor();
        AtomicInteger accepted = new AtomicInteger();
        service = service(task -> {
            if (accepted.getAndIncrement() > 0) { rejected.countDown(); throw new RejectedExecutionException("full"); }
            pool.execute(task);
        });
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(i -> {
            started.countDown(); assertThat(release.await(5, TimeUnit.SECONDS)).isTrue(); return "accepted.jpg";
        });
        try {
            Future<?> result = caller.submit(() -> service.createRecord(1L, 2L, create(file("a.jpg"), file("b.jpg"))));
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(rejected.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(result.isDone()).isFalse();
            verify(storage, never()).deleteFile(anyString());
            release.countDown();
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(RejectedExecutionException.class);
            verify(storage).deleteFile("accepted.jpg");
        } finally { release.countDown(); pool.shutdownNow(); caller.shutdownNow(); }
    }

    @Test
    void uploadedFilenameSurvivesFailureBeforeFutureIsRegistered() {
        service = service(task -> {
            task.run();
            throw new RejectedExecutionException("executor failed after running task");
        });
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                .isInstanceOf(RejectedExecutionException.class);
        verify(storage).deleteFile("new.jpg");
        verify(records, never()).insertTripRecord(any());
    }

    @Test
    void bulkFailureRollsBackAndPreservesOriginalExceptionWhenCleanupFails() {
        RuntimeException originalFailure = new IllegalStateException("bulk failed");
        when(images.insertTripRecordImages(anyList())).thenThrow(originalFailure);
        when(storage.deleteFile("new.jpg")).thenThrow(new IllegalStateException("cleanup failed"));
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg")))).isSameAs(originalFailure);
        verify(storage).deleteFile("new.jpg");
        assertThat(transactions.events).containsExactly("begin", "rollback");
    }

    @Test
    void rowCountMismatchRollsBackAndCompensates() {
        when(images.insertTripRecordImages(anyList())).thenReturn(0);
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                .isInstanceOf(IllegalStateException.class);
        verify(storage).deleteFile("new.jpg");
        assertThat(transactions.events).containsExactly("begin", "rollback");
    }

    @Test
    void commitExceptionPreservesFilesEvenWhenSubsequentRollbackReturnsSuccessfully() {
        transactions.failCommit = true;
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                .isInstanceOf(TransactionSystemException.class);
        verify(images).insertTripRecordImages(anyList());
        assertThat(transactions.events).containsExactly("begin", "commit", "rollback");
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void failureBuildingResponseAfterUploadStillCompensates() {
        when(records.findRecordById(10L)).thenThrow(new IllegalStateException("read failed"));
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                .hasMessage("read failed");
        verify(storage).deleteFile("new.jpg");
    }

    @Test
    void noImagesNullAndEmptyFilesSkipBulkInsert() {
        TripRecordRequestDto nullImages = create(); nullImages.setImageUrls(null);
        service.createRecord(1L, 2L, nullImages);
        service.createRecord(1L, 2L, create());
        service.createRecord(1L, 2L, create(null, new MockMultipartFile("image", new byte[0])));
        verifyNoInteractions(storage);
        verify(images, never()).insertTripRecordImages(anyList());
    }

    @Test
    void invalidCreateAndUnauthorizedCreateNeverUpload() throws Exception {
        TripRecordRequestDto dto = create(file("a.jpg")); dto.setTitle(" ");
        assertThatThrownBy(() -> service.createRecord(1L, 2L, dto)).isInstanceOf(BusinessException.class);
        when(members.isMemberOfTrip(1L, 2L)).thenReturn(0);
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg")))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void membershipRevokedDuringUploadCompensates() {
        when(members.isMemberOfTrip(1L, 2L)).thenReturn(1, 0);
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg")))).isInstanceOf(RuntimeException.class);
        verify(storage).deleteFile("new.jpg");
        verify(records, never()).insertTripRecord(any());
    }

    @Test
    void updateFailureKeepsOldFilesAndRemovesOnlyNewOnes() {
        doThrow(new IllegalStateException("update failed")).when(records).updateTripRecord(any());
        assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), true))).hasMessage("update failed");
        verify(storage).deleteFile("new.jpg");
        verify(storage, never()).deleteFile("old.jpg");
        assertThat(transactions.events).containsExactly("begin", "rollback");
    }

    @Test
    void updateCommitFailurePreservesBothOldAndNewFiles() {
        transactions.failCommit = true;
        assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), true)))
                .isInstanceOf(TransactionSystemException.class);
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void updateRemovesOldFilesOnlyAfterCommitAndDoesNotFailWhenDeleteThrows() {
        when(storage.deleteFile("old.jpg")).thenAnswer(i -> {
            assertThat(transactions.events).containsExactly("begin", "commit");
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new IllegalStateException("Firebase down");
        });
        assertThat(service.updateRecord(1L, 10L, 2L, update(List.of(), true))).isNotNull();
        verify(storage).deleteFile("old.jpg");
        verify(storage, never()).deleteFile("new.jpg");
        verify(images).deleteImagesByRecordIdAndFileNames(10L, List.of("old.jpg"));
    }

    @Test
    void nullAndEmptyKeepListBothRemoveAllWhileNullNewImagesSkipBulk() {
        TripRecordUpdateRequestDto dto = update(null, false); dto.setNewImages(null);
        service.updateRecord(1L, 10L, 2L, dto);
        service.updateRecord(1L, 10L, 2L, update(List.of(), false));
        verify(storage, times(2)).deleteFile("old.jpg");
        verify(images, never()).insertTripRecordImages(anyList());
    }

    @Test
    void retainedImagesAreNotDeleted() {
        service.updateRecord(1L, 10L, 2L, update(List.of("old.jpg"), false));
        verifyNoInteractions(storage);
        verify(images, never()).deleteImagesByRecordIdAndFileNames(anyLong(), anyList());
    }

    @Test
    void invalidForeignBlankNullAndDuplicateRetainedNamesFailBeforeUpload() {
        for (List<String> keep : List.of(List.of("foreign.jpg"), List.of(" "), Arrays.asList((String) null),
                List.of("old.jpg", "old.jpg"))) {
            assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(keep, true)))
                    .isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(storage);
        verify(records, never()).updateTripRecord(any());
    }

    @Test
    void changedImageSnapshotRejectsUpdateAndCompensates() {
        when(records.findImageUrlsByRecordId(10L)).thenReturn(List.of("old.jpg"), List.of("concurrent.jpg"));
        assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), true)))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getStatusCode().getStatus()).isEqualTo(409));
        verify(storage).deleteFile("new.jpg");
        verify(records, never()).updateTripRecord(any());
    }

    @Test
    void changedRecordOrRemovedRecordRejectsUpdate() {
        when(records.findRecordForUpdate(1L, 10L)).thenReturn(TripRecord.builder().recordId(10L).title("changed").build());
        assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), true))).isInstanceOf(BusinessException.class);
        when(records.findRecordForUpdate(1L, 10L)).thenReturn(null);
        assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), true))).isInstanceOf(RuntimeException.class);
        verify(storage, times(2)).deleteFile("new.jpg");
        verify(storage, never()).deleteFile("old.jpg");
    }

    @Test
    void deleteFilesOnlyAfterCommitAndFalseDeletionDoesNotFailResponse() {
        when(storage.deleteFile("old.jpg")).thenAnswer(i -> {
            assertThat(transactions.events).containsExactly("begin", "commit");
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            verify(records).deleteTripRecord(10L);
            return false;
        });
        service.deleteRecord(1L, 10L, 2L);
        verify(storage).deleteFile("old.jpg");
    }

    @Test
    void deleteRollbackAndCommitFailureNeverDeleteStorage() {
        doThrow(new IllegalStateException("delete failed")).when(records).deleteTripRecord(10L);
        assertThatThrownBy(() -> service.deleteRecord(1L, 10L, 2L)).hasMessage("delete failed");
        doNothing().when(records).deleteTripRecord(10L);
        transactions.failCommit = true;
        assertThatThrownBy(() -> service.deleteRecord(1L, 10L, 2L)).isInstanceOf(TransactionSystemException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void ambientTransactionIsRejectedBeforeExternalIo() {
        new TransactionTemplate(transactions).execute(status -> {
            assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                    .isInstanceOf(IllegalStateException.class);
            return null;
        });
        verifyNoInteractions(storage);
    }

    @Test
    void cleanupFailuresLogFileReasonAndCauseWithoutReplacingOriginal() {
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger)
                org.apache.logging.log4j.LogManager.getLogger(TripRecordServiceImpl.class);
        List<org.apache.logging.log4j.core.LogEvent> events = new ArrayList<>();
        org.apache.logging.log4j.core.appender.AbstractAppender appender =
                new org.apache.logging.log4j.core.appender.AbstractAppender("cleanup-test", null, null, true,
                        org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                    @Override public void append(org.apache.logging.log4j.core.LogEvent event) {
                        events.add(event.toImmutable());
                    }
                };
        appender.start(); logger.addAppender(appender);
        try {
            IllegalStateException cause = new IllegalStateException("cleanup denied");
            when(storage.deleteFile("old.jpg")).thenThrow(cause);
            service.deleteRecord(1L, 10L, 2L);
            assertThat(events).anySatisfy(event -> {
                assertThat(event.getMessage().getFormattedMessage()).contains("old.jpg", "delete after commit", "manual cleanup required");
                assertThat(event.getThrown()).isSameAs(cause);
            });
            doReturn(false).when(storage).deleteFile("old.jpg");
            service.deleteRecord(1L, 10L, 2L);
            assertThat(events).anySatisfy(event -> assertThat(event.getMessage().getFormattedMessage())
                    .contains("old.jpg", "storage returned false"));
        } finally { logger.removeAppender(appender); appender.stop(); }
    }

    @Test
    void uncertainCommitLogsPreservedFilesDespiteReportedRollback() {
        transactions.failCommit = true;
        try (CapturedLogs logs = new CapturedLogs()) {
            Throwable failure = catchThrowable(() -> service.createRecord(1L, 2L, create(file("a.jpg"))));
            assertThat(failure).isInstanceOf(TransactionSystemException.class);
            assertThat(logs.events).anySatisfy(event -> {
                assertThat(event.getMessage().getFormattedMessage()).contains("Image cleanup deferred", "operation=create",
                        "tripId=1", "newFiles=[new.jpg]", "dbWorkCompleted=true", "completionStatus=ROLLED_BACK",
                        "verify database before manual cleanup");
                assertThat(event.getThrown()).isSameAs(failure);
            });
        }
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void failedRollbackPreservesNewFilesAndLogsUnknownOutcome() {
        transactions.failRollback = true;
        when(images.insertTripRecordImages(anyList())).thenThrow(new IllegalStateException("bulk failed"));
        try (CapturedLogs logs = new CapturedLogs()) {
            assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                    .isInstanceOf(TransactionSystemException.class);
            assertThat(logs.events).anySatisfy(event -> assertThat(event.getMessage().getFormattedMessage())
                    .contains("newFiles=[new.jpg]", "dbWorkCompleted=false", "completionStatus=UNKNOWN"));
        }
        assertThat(transactions.events).containsExactly("begin", "rollback");
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void updateWithoutNewFilesLogsExistingRemovalCandidatesWhenCommitIsUncertain() {
        transactions.failCommit = true;
        try (CapturedLogs logs = new CapturedLogs()) {
            assertThatThrownBy(() -> service.updateRecord(1L, 10L, 2L, update(List.of(), false)))
                    .isInstanceOf(TransactionSystemException.class);
            assertThat(logs.events).anySatisfy(event -> assertThat(event.getMessage().getFormattedMessage())
                    .contains("operation=update", "recordId=10", "newFiles=[]", "oldFileCandidates=[old.jpg]"));
        }
        verifyNoInteractions(storage);
    }

    @Test
    void beginFailureCompensatesBecauseNoDatabaseWorkRan() {
        transactions.failBegin = true;
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(file("a.jpg"))))
                .isInstanceOf(TransactionSystemException.class).hasMessage("simulated begin failure");
        verify(records, never()).insertTripRecord(any());
        verify(storage).deleteFile("new.jpg");
    }

    static class CapturedLogs implements AutoCloseable {
        final List<org.apache.logging.log4j.core.LogEvent> events = new ArrayList<>();
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger)
                org.apache.logging.log4j.LogManager.getLogger(TripRecordServiceImpl.class);
        final org.apache.logging.log4j.core.appender.AbstractAppender appender =
                new org.apache.logging.log4j.core.appender.AbstractAppender("uncertain-outcome-test", null, null, true,
                        org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                    @Override public void append(org.apache.logging.log4j.core.LogEvent event) {
                        events.add(event.toImmutable());
                    }
                };
        CapturedLogs() { appender.start(); logger.addAppender(appender); }
        @Override public void close() { logger.removeAppender(appender); appender.stop(); }
    }

    static class RecordingTransactions extends AbstractPlatformTransactionManager {
        final List<String> events = new ArrayList<>();
        boolean failCommit;
        boolean failBegin;
        boolean failRollback;
        RecordingTransactions() { setRollbackOnCommitFailure(true); }
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) {
            events.add("begin");
            if (failBegin) throw new TransactionSystemException("simulated begin failure");
        }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
            if (failCommit) throw new TransactionSystemException("simulated commit failure");
        }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            events.add("rollback");
            if (failRollback) throw new TransactionSystemException("simulated rollback failure");
        }
    }
}
