package org.moa.trip.service;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.moa.global.handler.BusinessException;
import org.moa.global.service.FirebaseStorageService;
import org.moa.trip.dto.record.*;
import org.moa.trip.entity.TripRecord;
import org.moa.trip.mapper.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Explicitly opt in using a disposable loopback MySQL database named moa_image_test*. */
@EnabledIfEnvironmentVariable(named = "MOA_IMAGE_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TripRecordImageMysqlTest {
    JdbcTemplate jdbc;
    TrackedDataSource dataSource;
    DataSourceTransactionManager transactions;
    TripRecordMapper records;
    TripRecordImageMapper images;
    TripMemberMapper members;
    FirebaseStorageService storage;
    TripRecordService service;
    StatementCounts statements;

    @BeforeAll
    void connectOnlyToDisposableLocalDatabase() throws Exception {
        String url = System.getenv("MOA_IMAGE_TEST_JDBC_URL");
        assertThat(url).matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/moa_image_test[a-zA-Z0-9_]*(\\?.*)?");
        DriverManagerDataSource raw = new DriverManagerDataSource(url,
                System.getenv("MOA_IMAGE_TEST_DB_USER"), System.getenv("MOA_IMAGE_TEST_DB_PASSWORD"));
        dataSource = new TrackedDataSource(raw);
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        transactions.setRollbackOnCommitFailure(true);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
        factory.setMapperLocations(new org.springframework.core.io.Resource[]{new ClassPathResource("org/moa/trip/mapper/TripRecordMapper.xml"),
                new ClassPathResource("org/moa/trip/mapper/TripRecordImageMapper.xml"),
                new ClassPathResource("org/moa/trip/mapper/TripMemberMapper.xml")});
        statements = new StatementCounts();
        factory.setPlugins(new Interceptor[]{statements});
        SqlSessionTemplate sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        records = sessions.getMapper(TripRecordMapper.class);
        images = sessions.getMapper(TripRecordImageMapper.class);
        members = sessions.getMapper(TripMemberMapper.class);
        // Only the relevant production columns/FKs, independent of credentials or shared schema.
        jdbc.execute("CREATE TABLE IF NOT EXISTS trip (trip_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS member (member_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trip_member (trip_id BIGINT, member_id BIGINT, PRIMARY KEY(trip_id,member_id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trip_records (record_id BIGINT AUTO_INCREMENT PRIMARY KEY, trip_id BIGINT NOT NULL, " +
                "member_id BIGINT NOT NULL, title VARCHAR(255) NOT NULL, record_date DATE NOT NULL, content TEXT, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(trip_id) REFERENCES trip(trip_id) ON DELETE CASCADE, FOREIGN KEY(member_id) REFERENCES member(member_id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trip_record_images (image_id BIGINT AUTO_INCREMENT PRIMARY KEY, record_id BIGINT NOT NULL, " +
                "image_url VARCHAR(1024) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(record_id) REFERENCES trip_records(record_id) ON DELETE CASCADE) ENGINE=InnoDB");
    }

    @BeforeEach
    void cleanDisposableData() throws Exception {
        dataSource.failCommit.set(false);
        dataSource.failAfterCommit.set(false);
        dataSource.failRollback.set(false);
        jdbc.update("DELETE FROM trip_record_images"); jdbc.update("DELETE FROM trip_records");
        jdbc.update("DELETE FROM trip_member"); jdbc.update("DELETE FROM trip"); jdbc.update("DELETE FROM member");
        jdbc.update("INSERT INTO trip VALUES (1)"); jdbc.update("INSERT INTO member VALUES (2)");
        jdbc.update("INSERT INTO trip_member VALUES (1,2)");
        statements.bulkInserts.set(0); statements.singleInserts.set(0);
        statements.lockAttempts.set(0); statements.secondLockAttempt = null;
        dataSource.commits.set(0); dataSource.rollbacks.set(0);
        storage = mock(FirebaseStorageService.class);
        AtomicInteger sequence = new AtomicInteger();
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(i -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
            return "new-" + sequence.incrementAndGet() + ".jpg";
        });
        when(storage.deleteFile(anyString())).thenAnswer(i -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return true;
        });
        TripRecordServiceImpl target = new TripRecordServiceImpl(records, images, members, storage, Runnable::run, dataSource);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (TripRecordService) proxy.getProxy();
    }

    MockMultipartFile file() { return new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1,2,3}); }
    TripRecordRequestDto create(int count) {
        TripRecordRequestDto dto = new TripRecordRequestDto(); dto.setTitle("before"); dto.setContent("text");
        dto.setRecordDate(LocalDate.of(2026,1,1)); dto.setImageUrls(Collections.nCopies(count, file())); return dto;
    }
    TripRecordUpdateRequestDto update(List<String> keep, int count) {
        TripRecordUpdateRequestDto dto = new TripRecordUpdateRequestDto(); dto.setTitle("after"); dto.setContent("updated");
        dto.setRecordDate(LocalDate.of(2026,1,2)); dto.setExistingImageFileNames(keep);
        dto.setNewImages(Collections.nCopies(count, file())); return dto;
    }
    long seed() {
        long id = service.createRecord(1L, 2L, create(0)).getRecordId();
        jdbc.update("INSERT INTO trip_record_images(record_id,image_url) VALUES (?, 'old.jpg')", id);
        dataSource.commits.set(0); dataSource.rollbacks.set(0);
        return id;
    }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    String title(long id) { return jdbc.queryForObject("SELECT title FROM trip_records WHERE record_id=?", String.class, id); }

    @Test
    void realBulkInsertAndGeneratedKeyAreCommittedWithOneImageStatement() {
        TripRecordResponseDto result = service.createRecord(1L, 2L, create(3));
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(count("trip_records")).isEqualTo(1);
        assertThat(records.findImageUrlsByRecordId(result.getRecordId()))
                .containsExactlyInAnyOrder("new-1.jpg", "new-2.jpg", "new-3.jpg");
        assertThat(statements.bulkInserts).hasValue(1); assertThat(statements.singleInserts).hasValue(0);
        assertThat(dataSource.commits).hasValue(1); assertThat(dataSource.rollbacks).hasValue(0);
    }

    @Test
    void realBulkConstraintFailureRollsBackParentAndAllImageRows() throws Exception {
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenReturn("good.jpg", "x".repeat(1025));
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(2))).isInstanceOf(RuntimeException.class);
        assertThat(count("trip_records")).isZero(); assertThat(count("trip_record_images")).isZero();
        assertThat(dataSource.rollbacks).hasValue(1);
        verify(storage).deleteFile("good.jpg"); verify(storage).deleteFile("x".repeat(1025));
    }

    @Test
    void jdbcCommitExceptionBeforeCommitStillPreservesFilesBecauseClientCannotKnowOutcome() {
        dataSource.failCommit.set(true);
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(2))).isInstanceOf(RuntimeException.class);
        assertThat(dataSource.rollbacks).hasValue(1);
        assertThat(count("trip_records")).isZero(); assertThat(count("trip_record_images")).isZero();
        verify(storage, never()).deleteFile(anyString());
        assertThat(dataSource.commits).hasValue(0);
    }

    @Test
    void updateCommitsNewLinksBeforeDeletingOldFile() {
        long id = seed();
        doAnswer(i -> {
            assertThat(dataSource.commits).hasValue(1);
            assertThat(title(id)).isEqualTo("after");
            assertThat(records.findImageUrlsByRecordId(id)).containsExactlyInAnyOrder("new-1.jpg", "new-2.jpg");
            return true;
        }).when(storage).deleteFile("old.jpg");
        service.updateRecord(1L, id, 2L, update(List.of(), 2));
        verify(storage).deleteFile("old.jpg");
        assertThat(statements.bulkInserts).hasValue(1);
    }

    @Test
    void updateBulkFailureRollsBackMetadataAndKeepsOldLinksAndFiles() throws Exception {
        long id = seed();
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenReturn("x".repeat(1025));
        assertThatThrownBy(() -> service.updateRecord(1L, id, 2L, update(List.of(),1))).isInstanceOf(RuntimeException.class);
        assertThat(title(id)).isEqualTo("before");
        assertThat(records.findImageUrlsByRecordId(id)).containsExactly("old.jpg");
        verify(storage, never()).deleteFile("old.jpg"); verify(storage).deleteFile("x".repeat(1025));
        assertThat(dataSource.rollbacks).hasValue(1);
    }

    @Test
    void updateCommitFailureKeepsOldMetadataAndFiles() {
        long id = seed(); dataSource.failCommit.set(true);
        assertThatThrownBy(() -> service.updateRecord(1L, id, 2L, update(List.of(),1))).isInstanceOf(RuntimeException.class);
        assertThat(title(id)).isEqualTo("before");
        assertThat(records.findImageUrlsByRecordId(id)).containsExactly("old.jpg");
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void deleteCommitsBeforeExternalCleanupEvenIfCleanupFails() {
        long id = seed();
        doAnswer(i -> {
            assertThat(dataSource.commits).hasValue(1);
            assertThat(count("trip_records")).isZero(); assertThat(count("trip_record_images")).isZero();
            throw new IllegalStateException("storage unavailable");
        }).when(storage).deleteFile("old.jpg");
        service.deleteRecord(1L, id, 2L);
        verify(storage).deleteFile("old.jpg");
    }

    @Test
    void deleteCommitFailureRollsBackAllLinksAndNeverDeletesFiles() {
        long id = seed(); dataSource.failCommit.set(true);
        assertThatThrownBy(() -> service.deleteRecord(1L, id, 2L)).isInstanceOf(RuntimeException.class);
        assertThat(count("trip_records")).isEqualTo(1);
        assertThat(records.findImageUrlsByRecordId(id)).containsExactly("old.jpg");
        verifyNoInteractions(storage); assertThat(dataSource.rollbacks).hasValue(1);
    }

    @Test
    void overlappingUpdateDuringUploadIsRejectedWithoutLosingCommittedEdit() throws Exception {
        long id = seed();
        CountDownLatch uploading = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        when(storage.uploadProcessed(any(), anyString(), anyString())).thenAnswer(i -> {
            uploading.countDown(); assertThat(release.await(5,TimeUnit.SECONDS)).isTrue(); return "stale-new.jpg";
        });
        try {
            Future<?> stale = caller.submit(() -> service.updateRecord(1L,id,2L,update(List.of("old.jpg"),1)));
            assertThat(uploading.await(5,TimeUnit.SECONDS)).isTrue();
            TripRecordUpdateRequestDto latest = update(List.of("old.jpg"),0); latest.setTitle("winner");
            service.updateRecord(1L,id,2L,latest);
            release.countDown();
            assertThatThrownBy(() -> stale.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(BusinessException.class);
            assertThat(title(id)).isEqualTo("winner");
            assertThat(records.findImageUrlsByRecordId(id)).containsExactly("old.jpg");
            verify(storage).deleteFile("stale-new.jpg"); verify(storage,never()).deleteFile("old.jpg");
        } finally { release.countDown(); caller.shutdownNow(); }
    }

    @Test
    void rowLockActuallyBlocksConcurrentDeleteUntilCommit() throws Exception {
        long id = seed();
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1);
        statements.secondLockAttempt = new CountDownLatch(1);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = callers.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                records.findRecordForUpdate(1L,id); locked.countDown();
                try { assertThat(release.await(5,TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                return null;
            }));
            assertThat(locked.await(5,TimeUnit.SECONDS)).isTrue();
            Future<?> deleting = callers.submit(() -> service.deleteRecord(1L,id,2L));
            assertThat(statements.secondLockAttempt.await(5,TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            int waiting;
            do {
                waiting = jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
                if (waiting == 0) Thread.sleep(10);
            } while (waiting == 0 && System.nanoTime() < deadline);
            // 단순한 스레드 스케줄링 지연이 아니라 MySQL의 실제 잠금 대기를 확인한다.
            assertThat(waiting).isGreaterThan(0);
            assertThat(deleting.isDone()).isFalse(); verifyNoInteractions(storage);
            release.countDown(); holder.get(5,TimeUnit.SECONDS); deleting.get(5,TimeUnit.SECONDS);
            assertThat(count("trip_records")).isZero(); verify(storage).deleteFile("old.jpg");
        } finally { release.countDown(); callers.shutdownNow(); }
    }

    @Test
    void emptyImagesSkipBulkAndForeignKeepNamesNeverWrite() {
        long id = seed();
        assertThat(statements.bulkInserts).hasValue(0);
        assertThatThrownBy(() -> service.updateRecord(1L,id,2L,update(List.of("foreign.jpg"),1)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage); assertThat(title(id)).isEqualTo("before");
        service.updateRecord(1L,id,2L,update(null,0));
        assertThat(records.findImageUrlsByRecordId(id)).isEmpty();
        assertThat(statements.bulkInserts).hasValue(0);
    }

    @Test
    void springProxyRejectsAmbientTransactionBeforeUpload() {
        new TransactionTemplate(transactions).execute(status -> {
            assertThatThrownBy(() -> service.createRecord(1L,2L,create(1)))
                    .isInstanceOf(IllegalStateException.class);
            return null;
        });
        verifyNoInteractions(storage); assertThat(count("trip_records")).isZero();
    }

    @Test
    void createResponseLossAfterRealCommitPreservesReferencedFilesDespiteSuccessfulRollbackCall() {
        dataSource.failAfterCommit.set(true);
        assertThatThrownBy(() -> service.createRecord(1L, 2L, create(2)))
                .isInstanceOf(RuntimeException.class).hasRootCauseMessage("Injected response loss AFTER MySQL commit");
        // 별도 연결로 확인: rollback()이 성공해도 이미 커밋된 데이터는 남는다.
        assertThat(dataSource.commits).hasValue(1);
        assertThat(dataSource.rollbacks).hasValue(1);
        assertThat(count("trip_records")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT image_url FROM trip_record_images", String.class))
                .containsExactlyInAnyOrder("new-1.jpg", "new-2.jpg");
        verify(storage, never()).deleteFile(anyString());
    }

    @Test
    void updateResponseLossAfterRealCommitAndFailedRollbackPreservesAllFiles() {
        long id = seed();
        dataSource.failAfterCommit.set(true);
        dataSource.failRollback.set(true);
        assertThatThrownBy(() -> service.updateRecord(1L, id, 2L, update(List.of(), 2)))
                .isInstanceOf(RuntimeException.class);
        assertThat(dataSource.commits).hasValue(1);
        assertThat(dataSource.rollbacks).hasValue(0);
        assertThat(title(id)).isEqualTo("after");
        assertThat(records.findImageUrlsByRecordId(id)).containsExactlyInAnyOrder("new-1.jpg", "new-2.jpg");
        // 새 파일은 DB가 참조 중이다. 기존 파일도 결과 불명확 경로에서 자동 정리하지 않는다.
        verify(storage, never()).deleteFile(anyString());
    }

    static class TrackedDataSource extends DelegatingDataSource {
        final AtomicBoolean failCommit = new AtomicBoolean();
        final AtomicBoolean failAfterCommit = new AtomicBoolean();
        final AtomicBoolean failRollback = new AtomicBoolean();
        final AtomicInteger commits = new AtomicInteger(), rollbacks = new AtomicInteger();
        TrackedDataSource(DataSource delegate) { super(delegate); }
        @Override public Connection getConnection() throws SQLException {
            Connection delegate = super.getConnection();
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("commit") && failCommit.getAndSet(false)) {
                            throw new SQLException("Injected commit failure BEFORE MySQL commit");
                        }
                        if (method.getName().equals("rollback") && failRollback.getAndSet(false)) {
                            throw new SQLException("Injected rollback failure");
                        }
                        try {
                            Object result = method.invoke(delegate,args);
                            if (method.getName().equals("commit")) {
                                commits.incrementAndGet();
                                if (failAfterCommit.getAndSet(false)) {
                                    throw new SQLException("Injected response loss AFTER MySQL commit");
                                }
                            }
                            if (method.getName().equals("rollback")) rollbacks.incrementAndGet();
                            return result;
                        } catch (InvocationTargetException ex) { throw ex.getCause(); }
                    });
        }
    }

    @Intercepts({@Signature(type=Executor.class,method="update",args={MappedStatement.class,Object.class}),
            @Signature(type=Executor.class,method="query",args={MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class})})
    static class StatementCounts implements Interceptor {
        final AtomicInteger bulkInserts = new AtomicInteger(), singleInserts = new AtomicInteger(), lockAttempts = new AtomicInteger();
        volatile CountDownLatch secondLockAttempt;
        @Override public Object intercept(Invocation invocation) throws Throwable {
            String id = ((MappedStatement) invocation.getArgs()[0]).getId();
            if (id.endsWith(".insertTripRecordImages")) bulkInserts.incrementAndGet();
            if (id.endsWith(".insertTripRecordImage")) singleInserts.incrementAndGet();
            if (id.endsWith(".findRecordForUpdate") && lockAttempts.incrementAndGet() == 2 && secondLockAttempt != null) {
                secondLockAttempt.countDown();
            }
            return invocation.proceed();
        }
        @Override public Object plugin(Object target) { return Plugin.wrap(target,this); }
        @Override public void setProperties(Properties properties) { }
    }
}
