package org.moa.trip.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.exception.ForbiddenAccessException;
import org.moa.global.exception.RecordNotFoundException;
import org.moa.global.handler.FileUploadException;
import org.moa.global.handler.BusinessException;
import org.moa.global.type.StatusCode;
import org.moa.global.service.FirebaseStorageService;
import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.*;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.moa.trip.entity.TripRecord;
import org.moa.trip.entity.TripRecordImage;
import org.moa.trip.mapper.TripMemberMapper;
import org.moa.trip.mapper.TripRecordImageMapper;
import org.moa.trip.mapper.TripRecordMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@Slf4j
public class TripRecordServiceImpl implements TripRecordService {
    private final TripRecordMapper tripRecordMapper;
    private final TripRecordImageMapper tripRecordImageMapper;
    private final TripMemberMapper tripMemberMapper;
    private final FirebaseStorageService firebaseStorageService;

    private final Executor ioTaskExecutor;
    private final TransactionTemplate writeTransaction;

    private record ProcessingResult(TripRecordCardDto record, long duration) {
    }

    private record ProcessedImage(byte[] bytes, String format, String contentType,
                                  long originalSize, long finalSize, boolean resized) {}

    @Autowired
    public TripRecordServiceImpl(TripRecordMapper tripRecordMapper,
                                 TripRecordImageMapper tripRecordImageMapper,
                                 TripMemberMapper tripMemberMapper,
                                 FirebaseStorageService firebaseStorageService,
                                 @Qualifier("ioTaskExecutor") Executor ioTaskExecutor,
                                 DataSource dataSource) {
        this(tripRecordMapper, tripRecordImageMapper, tripMemberMapper, firebaseStorageService,
                ioTaskExecutor, imageTransactionManager(dataSource));
    }

    private static PlatformTransactionManager imageTransactionManager(DataSource dataSource) {
        // 커밋이 DB에 도달하기 전에 실패한 경우에도 rollback을 시도한다.
        // 다른 기능이 쓰는 공용 transactionManager 설정은 변경하지 않는다.
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        manager.setRollbackOnCommitFailure(true);
        return manager;
    }

    // 외부 DB 없는 단위 테스트에서 트랜잭션 생명주기만 대체하는 생성자.
    TripRecordServiceImpl(TripRecordMapper tripRecordMapper,
                                 TripRecordImageMapper tripRecordImageMapper,
                                 TripMemberMapper tripMemberMapper,
                                 FirebaseStorageService firebaseStorageService,
                                 @Qualifier("ioTaskExecutor") Executor ioTaskExecutor,
                                 PlatformTransactionManager transactionManager) {
        this.tripRecordMapper = tripRecordMapper;
        this.tripRecordImageMapper = tripRecordImageMapper;
        this.tripMemberMapper = tripMemberMapper;
        this.firebaseStorageService = firebaseStorageService;
        this.ioTaskExecutor = ioTaskExecutor;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 외부 I/O에는 트랜잭션이 없고 execute 반환 시 실제 커밋까지 완료된다. */
    @Override
    public TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto) {
        requireNoTransaction();
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank() || dto.getRecordDate() == null) {
            throw new BusinessException(StatusCode.INVALID_INPUT, "제목과 여행 날짜를 입력해주세요.");
        }
        checkMembership(tripId, memberId, "생성");
        List<String> uploaded = uploadImages(dto.getImageUrls());
        return executeImageWrite("create", tripId, null, uploaded, List.of(), () -> {
            checkMembership(tripId, memberId, "생성");
            TripRecord record = TripRecord.builder()
                    .tripId(tripId).memberId(memberId).title(dto.getTitle())
                    .recordDate(dto.getRecordDate()).content(dto.getContent()).build();
            tripRecordMapper.insertTripRecord(record);
            saveImages(record.getRecordId(), uploaded);
            return TripRecordResponseDto.fromEntity(tripRecordMapper.findRecordById(record.getRecordId()));
        });
    }

    /** 일자별 여행 기록 조회 **/
    @Override
    @Transactional(readOnly = true)
    public Page<TripRecordCardDto> getRecordsByDate(Long tripId, LocalDate date, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        List<TripRecordCardDto> records = tripRecordMapper.findRecordsByDate(tripId, date, pageable);

        // 각 여행 기록의 이미지 URL을 비동기적으로 가져오고, 그 결과와 시간을 반환하는 작업 목록 생성
        List<CompletableFuture<ProcessingResult>> futures = records.stream()
                .map(record -> CompletableFuture.supplyAsync(() -> {
                    long taskStartTime = System.currentTimeMillis();

                    if (record.getImageUrls() != null && !record.getImageUrls().isEmpty()) {
                        // 파일 이름 리스트를 서명된 URL 리스트로 변환
                        List<String> signedUrls = record.getImageUrls().parallelStream()
                                .filter(Objects::nonNull) // LEFT JOIN으로 인해 null이 포함될 수 있으므로 필터링
                                .map(firebaseStorageService::getSignedUrl)
                                .filter(Objects::nonNull)
                                .toList();
                        record.setImageUrls(signedUrls);
                    }

                    long taskEndTime = System.currentTimeMillis();
                    return new ProcessingResult(record, taskEndTime - taskStartTime);
                }, ioTaskExecutor)) // 커스텀 스레드 풀을 명시적으로 사용
                .toList();

        // 생성된 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // [로그 추가] 각 작업의 결과와 소요 시간을 로그로 남김
        List<TripRecordCardDto> finalRecords = futures.stream()
                .map(CompletableFuture::join)
                .peek(result -> log.info("개별 기록 처리 완료 - recordId: {}, 소요 시간: {}ms", result.record().getRecordId(), result.duration()))
                .map(ProcessingResult::record)
                .toList();

        int total = tripRecordMapper.countRecordsByDate(tripId, date);

        long endTime = System.currentTimeMillis();
        log.info("getRecordsByDate - 총 소요 시간 (비동기 처리): {}ms, 처리된 여행 기록 수: {}", (endTime - startTime), records.size());

        return new PageImpl<>(finalRecords, pageable, total);
    }

    
    /** 여행 기록 상세 조회 **/
    @Override
    @Transactional(readOnly = true)
    public TripRecordDetailResponseDto getRecordDetail(Long tripId, Long recordId) {
        long startTime = System.currentTimeMillis();

        TripRecord tripRecord = tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        
        if (tripRecord == null) {
            throw new RecordNotFoundException();
        }

        List<String> imageFileNames = tripRecordMapper.findImageUrlsByRecordId(recordId);


        List<CompletableFuture<TripRecordDetailResponseDto.ImageInfo>> imageInfoFutures = imageFileNames.stream()
                // I/O 전용 스레드 풀을 사용하도록 명시적으로 지정
                .map(fileName -> CompletableFuture.supplyAsync(() -> {
                    String signedUrl = firebaseStorageService.getSignedUrl(fileName);
                    return signedUrl != null ? new TripRecordDetailResponseDto.ImageInfo(signedUrl, fileName) : null;
                }, ioTaskExecutor))
                .toList();


        List<TripRecordDetailResponseDto.ImageInfo> images = imageInfoFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        long endTime = System.currentTimeMillis();
        log.info("getRecordDetail - 총 소요 시간 (비동기 처리): {}ms, 이미지 수: {}", (endTime - startTime), images.size());


        return TripRecordDetailResponseDto.of(tripRecord, images);
    }

    
    /** 업로드 중 다른 수정이 있었으면 충돌로 거절하고 신규 파일만 정리한다. */
    @Override
    public TripRecordResponseDto updateRecord(Long tripId, Long recordId, Long memberId, TripRecordUpdateRequestDto dto) {
        requireNoTransaction();
        TripRecord before = checkRecordAuthority(tripId, recordId, memberId, "수정", false);
        List<String> beforeImages = tripRecordMapper.findImageUrlsByRecordId(recordId);
        Set<String> keep = validateRetainedImages(dto.getExistingImageFileNames(), beforeImages);
        List<String> removalCandidates = beforeImages.stream().filter(name -> !keep.contains(name)).toList();
        List<String> uploaded = uploadImages(dto.getNewImages());
        UpdateResult result = executeImageWrite("update", tripId, recordId, uploaded, removalCandidates, () -> {
            TripRecord current = checkRecordAuthority(tripId, recordId, memberId, "수정", true);
            List<String> currentImages = tripRecordMapper.findImageUrlsByRecordId(recordId);
            if (!before.equals(current) || !beforeImages.stream().sorted().toList().equals(currentImages.stream().sorted().toList())) {
                throw new BusinessException(StatusCode.CONFLICT, "여행 기록이 변경되었습니다. 새로 조회 후 다시 수정해주세요.");
            }
            validateRetainedImages(dto.getExistingImageFileNames(), currentImages);
            List<String> removed = currentImages.stream().filter(name -> !keep.contains(name)).toList();
            tripRecordMapper.updateTripRecord(TripRecord.builder().recordId(recordId)
                    .title(dto.getTitle()).content(dto.getContent()).recordDate(dto.getRecordDate()).build());
            saveImages(recordId, uploaded);
            if (!removed.isEmpty()) {
                tripRecordImageMapper.deleteImagesByRecordIdAndFileNames(recordId, removed);
            }
            return new UpdateResult(TripRecordResponseDto.fromEntity(tripRecordMapper.findRecordById(recordId)), removed);
        });
        // 커밋 후 정리 실패는 이미 성공한 DB 수정을 실패로 바꾸지 않는다.
        deleteFilesBestEffort(result.removed(), "update after commit");
        return result.response();
    }

    @Override
    public void deleteRecord(Long tripId, Long recordId, Long memberId) {
        requireNoTransaction();
        checkRecordAuthority(tripId, recordId, memberId, "삭제", false);
        List<String> removed = writeTransaction.execute(status -> {
            checkRecordAuthority(tripId, recordId, memberId, "삭제", true);
            List<String> names = tripRecordMapper.findImageUrlsByRecordId(recordId);
            tripRecordImageMapper.deleteImagesByRecordId(recordId);
            tripRecordMapper.deleteTripRecord(recordId);
            return names;
        });
        deleteFilesBestEffort(removed, "delete after commit");
    }

    /** 커밋 응답 유실 시 rollback 성공/ROLLED_BACK 통지만으로 미커밋을 단정하지 않는다. */
    private static class WriteOutcome implements TransactionSynchronization {
        boolean workStarted;
        boolean workCompleted;
        int completionStatus = STATUS_UNKNOWN;

        @Override
        public void afterCompletion(int status) {
            completionStatus = status;
        }

        boolean canCompensate() {
            // 시작 실패는 DB 작업이 없었다. 작업 중 실패는 롤백 완료가 확인돼야 정리한다.
            // 콜백 반환 이후의 실패는 commit 이전/이후인지 확정할 수 없어 모두 보존한다.
            return !workStarted || (!workCompleted && completionStatus == STATUS_ROLLED_BACK);
        }

        String completionName() {
            return switch (completionStatus) {
                case STATUS_COMMITTED -> "COMMITTED";
                case STATUS_ROLLED_BACK -> "ROLLED_BACK";
                default -> "UNKNOWN";
            };
        }
    }

    private <T> T executeImageWrite(String operation, Long tripId, Long recordId,
                                    List<String> uploaded, List<String> removalCandidates, Supplier<T> work) {
        WriteOutcome outcome = new WriteOutcome();
        try {
            return writeTransaction.execute(status -> {
                outcome.workStarted = true;
                TransactionSynchronizationManager.registerSynchronization(outcome);
                T result = work.get();
                outcome.workCompleted = true;
                return result;
            });
        } catch (RuntimeException | Error failure) {
            if (outcome.canCompensate()) {
                deleteFilesBestEffort(uploaded, operation + " compensation");
            } else {
                log.error("Image cleanup deferred: operation={}, tripId={}, recordId={}, newFiles={}, oldFileCandidates={}, "
                                + "dbWorkCompleted={}, completionStatus={}; outcome uncertain; verify database before manual cleanup",
                        operation, tripId, recordId, uploaded, removalCandidates,
                        outcome.workCompleted, outcome.completionName(), failure);
            }
            throw failure;
        }
    }

    private record UpdateResult(TripRecordResponseDto response, List<String> removed) {}

    private void requireNoTransaction() {
        // 직접 호출되더라도 외부 트랜잭션 안에서 업로드하는 것을 허용하지 않는다.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("여행 기록 변경은 외부 트랜잭션 안에서 호출할 수 없습니다.");
        }
    }

    private void checkMembership(Long tripId, Long memberId, String action) {
        if (tripMemberMapper.isMemberOfTrip(tripId, memberId) == 0) {
            throw new ForbiddenAccessException("해당 기록을 " + action + "할 권한이 없습니다.");
        }
    }

    private TripRecord checkRecordAuthority(Long tripId, Long recordId, Long memberId, String action, boolean lock) {
        TripRecord record = lock ? tripRecordMapper.findRecordForUpdate(tripId, recordId)
                : tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        if (record == null) {
            throw new RecordNotFoundException();
        }
        checkMembership(tripId, memberId, action);
        return record;
    }

    private Set<String> validateRetainedImages(List<String> requested, List<String> current) {
        // 기존 계약 유지: null과 [] 모두 기존 이미지를 전부 제거한다.
        if (requested == null) {
            return Collections.emptySet();
        }
        Set<String> keep = new HashSet<>();
        for (String name : requested) {
            if (name == null || name.isBlank() || !keep.add(name) || !current.contains(name)) {
                throw new BusinessException(StatusCode.INVALID_INPUT, "유지할 이미지 목록이 유효하지 않습니다.");
            }
        }
        return keep;
    }

    private void saveImages(Long recordId, List<String> names) {
        if (names.isEmpty()) {
            return;
        }
        List<TripRecordImage> images = names.stream().map(name -> TripRecordImage.builder()
                .recordId(recordId).imageUrl(name).build()).toList();
        int inserted = tripRecordImageMapper.insertTripRecordImages(images);
        if (inserted != images.size()) {
            throw new IllegalStateException("이미지 저장 개수가 업로드 개수와 일치하지 않습니다.");
        }
    }

    private static class Upload {
        // 업로드 직후 먼저 기록한다. 이후 future 완료 처리에 실패해도 정리 대상이 남는다.
        volatile String fileName;
    }

    /** DB 작업 없이 병렬 처리한다. 제출 실패 시에도 이미 실행 중인 작업을 모두 기다린다. */
    private List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<Upload> uploads = new ArrayList<>();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        RuntimeException submissionFailure = null;
        for (MultipartFile file : files) {
            try {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                Upload upload = new Upload();
                uploads.add(upload);
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        ProcessedImage processed = processImage(file);
                        upload.fileName = firebaseStorageService.uploadProcessed(
                                processed.bytes(), processed.format(), processed.contentType());
                        if (upload.fileName == null || upload.fileName.isBlank()) {
                            throw new IllegalStateException("업로드된 파일명이 비어있습니다.");
                        }
                        return upload.fileName;
                    } catch (IOException e) {
                        throw new FileUploadException("파일 업로드 중 오류 발생", e);
                    }
                }, ioTaskExecutor));
            } catch (RuntimeException failure) {
                submissionFailure = failure;
                break;
            }
        }
        // allOf는 실패한 작업이 있어도 모든 작업이 끝난 후 완료된다. 취소하지 않는다.
        Throwable uploadFailure = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((ignored, failure) -> failure).join();
        List<String> uploaded = uploads.stream().map(upload -> upload.fileName)
                .filter(name -> name != null && !name.isBlank()).toList();
        if (submissionFailure != null || uploadFailure != null) {
            deleteFilesBestEffort(uploaded, "upload compensation");
            if (submissionFailure != null) {
                throw submissionFailure;
            }
            throw new FileUploadException("이미지 업로드에 실패했습니다.",
                    uploadFailure instanceof CompletionException ? uploadFailure.getCause() : uploadFailure);
        }
        return uploaded;
    }

    private void deleteFilesBestEffort(List<String> names, String reason) {
        for (String name : names) {
            try {
                if (!firebaseStorageService.deleteFile(name)) {
                    log.warn("Firebase cleanup failed: file={}, reason={}, storage returned false; manual cleanup required", name, reason);
                }
            } catch (Exception failure) {
                // 정리 실패가 업로드/DB 원래 예외 또는 성공한 응답을 덮어쓰지 않는다.
                log.warn("Firebase cleanup failed: file={}, reason={}; manual cleanup required", name, reason, failure);
            }
        }
    }

    /** 리사이즈/압축 + 포맷/헤더 정합성 + 최종 바이트 비교 **/
    private ProcessedImage processImage(MultipartFile imageFile) throws IOException {
        long startTime = System.currentTimeMillis();

        byte[] original = imageFile.getBytes();
        long originalSize = original.length;
        String originalName = imageFile.getOriginalFilename();

        log.debug("이미지 처리 시작 - 파일: {}, 원본 크기: {} bytes", originalName, originalSize);

        // 압축 스킵 조건 체크
        if (shouldSkipCompression(originalSize, originalName, imageFile.getContentType())) {
            String fmt = guessFormatFromNameOrType(originalName, imageFile.getContentType());
            String ct = contentTypeFor(fmt, imageFile.getContentType());

            ProcessedImage result = new ProcessedImage(original, fmt, ct, originalSize, originalSize, false);

            // 스킵 케이스 통합 로그
            log.info("압축 스킵 - {}: {} bytes (크기 작음/GIF/WebP/SVG)", originalName, originalSize);
            logCompressionStats("압축 스킵", startTime, originalSize);

            return result;
        }

        // 디코드 시도 (EXIF 회전 교정 위해)
        BufferedImage src;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(original)) {
            src = ImageIO.read(bais);
        }

        // 디코드 불가능(HEIC/특수 포맷 등) → 원본 그대로 업로드
        if (src == null) {
            String fmt = guessFormatFromNameOrType(originalName, imageFile.getContentType());
            String ct = contentTypeFor(fmt, imageFile.getContentType());
            ProcessedImage result = new ProcessedImage(original, fmt, ct, originalSize, originalSize, false);

            // 디코딩 실패 시 경고 로그
            log.warn("디코딩 실패 - {}: {} bytes (원본 사용)", originalName, originalSize);
            logCompressionStats("디코딩 실패", startTime, originalSize);

            return result;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        boolean hasAlpha = src.getColorModel().hasAlpha();

        // 동적 압축 전략
        CompressionConfig config = determineCompressionConfig(originalSize, width, height, hasAlpha);

        log.debug("압축 설정 - 최대크기: {}px, 품질: {}, 포맷: {}",
                config.maxDimension, config.quality, config.targetFormat);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(1024, original.length / 2));
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(src)
                .useExifOrientation(true); // EXIF 회전 정보 처리

        // 리사이징 결정
        boolean needResize = width > config.maxDimension || height > config.maxDimension;
        if (needResize) {
            builder.size(config.maxDimension, config.maxDimension);
        } else {
            builder.scale(1.0);
        }

        builder.outputFormat(config.targetFormat);

        // 품질 설정 (JPEG만)
        if (!hasAlpha && "jpg".equals(config.targetFormat)) {
            builder.outputQuality(config.quality);
        }

        builder.toOutputStream(bos);
        byte[] processed = bos.toByteArray();

        // 압축 효과 검증
        boolean shouldUseProcessed = shouldUseProcessedImage(original.length, processed.length);

        byte[] finalBytes = shouldUseProcessed ? processed : original;
        String finalFormat = shouldUseProcessed ? config.targetFormat
                : guessFormatFromNameOrType(originalName, imageFile.getContentType());
        String finalContentType = contentTypeFor(finalFormat, imageFile.getContentType());

        ProcessedImage result = new ProcessedImage(finalBytes, finalFormat, finalContentType,
                originalSize, finalBytes.length, needResize);

        // 통합 최종 로그
        if (shouldUseProcessed) {
            double reduction = (100.0 * (originalSize - finalBytes.length)) / originalSize;
            log.info("압축 완료 - {}: {}x{} ({} KB → {} KB, {}% 감소, 리사이즈: {})",
                    originalName,
                    width, height,
                    String.format("%.1f", originalSize / 1024.0),      // 원본 크기 (KB)
                    String.format("%.1f", finalBytes.length / 1024.0), // 최종 크기 (KB)
                    String.format("%.1f", reduction),
                    needResize);
        } else {
            log.info("원본 사용 - {}: {}x{} ({} KB, 압축 효과 미미)",
                    originalName,
                    width, height,
                    String.format("%.1f", originalSize / 1024.0));
        }

        // 성능 로그는 DEBUG 레벨로
        logCompressionStats("이미지 압축", startTime, originalSize);

        return result;
    }

    /** 압축 스킵 조건 **/
    private boolean shouldSkipCompression(long fileSize, String filename, String contentType) {
        // 500KB 이하는 압축 스킵
        if (fileSize <= 300 * 1024) {
            log.debug("압축 스킵 - 파일 크기 작음 ({} bytes): {}", fileSize, filename);
            return true;
        }

        // GIF는 애니메이션 보존을 위해 스킵
        if (filename != null && filename.toLowerCase().endsWith(".gif")) {
            log.debug("압축 스킵 - GIF 파일: {}", filename);
            return true;
        }

        // 이미 최적화된 웹 포맷
        if (contentType != null && contentType.contains("webp")) {
            log.debug("압축 스킵 - 이미 최적화된 WebP: {}", filename);
            return true;
        }

        if (filename != null && filename.toLowerCase().endsWith(".svg")) {
            log.debug("압축 스킵 - SVG 벡터 이미지: {}", filename);
            return true;
        }

        return false;
    }

    /** 동적 압축 설정 **/
    private CompressionConfig determineCompressionConfig(long fileSize, int width, int height, boolean hasAlpha) {
        String format = hasAlpha ? "png" : "jpg";
        int totalPixels = width * height;

        // 초고해상도 이미지 (4K 이상, 8MP+)
        if (totalPixels > 8_000_000 || Math.max(width, height) > 4000) {
            return new CompressionConfig(1920, 0.75, format); // 4K → FHD로 축소
        }
        // 고해상도 + 대용량
        else if (fileSize > 15 * 1024 * 1024) {
            return new CompressionConfig(1600, 0.72, format); // 더 강한 압축
        }
        // 중해상도 + 중용량
        else if (fileSize > 8 * 1024 * 1024) {
            return new CompressionConfig(1600, 0.78, format);
        }
        // 소용량이지만 고해상도 (리사이징 위주)
        else if (Math.max(width, height) > 2500) {
            return new CompressionConfig(2000, 0.85, format); // 크기만 줄이고 품질 유지
        }
        // 중용량
        else if (fileSize > 3 * 1024 * 1024) {
            return new CompressionConfig(1800, 0.82, format);
        }
        // 기본 (소용량 + 적당한 해상도)
        else {
            return new CompressionConfig(1600, 0.85, format);
        }
    }

    /** 압축 효과 검증 **/
    private boolean shouldUseProcessedImage(long originalSize, long processedSize) {
        // 매우 큰 파일 (10MB+)은 5% 감소해도 의미있음
        if (originalSize > 10 * 1024 * 1024) {
            return processedSize < originalSize * 0.95; // 5% 이상 감소
        }
        // 중간 파일 (3MB+)은 8% 감소 필요
        else if (originalSize > 3 * 1024 * 1024) {
            return processedSize < originalSize * 0.92; // 8% 이상 감소
        }
        // 작은 파일은 15% 이상 감소해야 의미있음
        else {
            return processedSize < originalSize * 0.85; // 15% 이상 감소
        }
    }

    /** 압축 설정을 담는 클래스 **/
    private static class CompressionConfig {
        final int maxDimension;
        final double quality;
        final String targetFormat;

        CompressionConfig(int maxDimension, double quality, String targetFormat) {
            this.maxDimension = maxDimension;
            this.quality = quality;
            this.targetFormat = targetFormat;
        }
    }


    private static String guessFormatFromNameOrType(String name, String contentType) {
        // 파일명 우선 검사
        if (name != null && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            switch (ext) {
                case "jpeg", "jpg", "jfif", "jpe" -> { return "jpg"; }
                case "png" -> { return "png"; }
                case "webp" -> { return "webp"; }
                case "gif" -> { return "gif"; }
                case "bmp" -> { return "jpg"; }
                case "tiff", "tif" -> { return "jpg"; } // TIFF도 JPEG로 변환
            }
        }

        // Content-Type 검사
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
            if (contentType.contains("png")) return "png";
            if (contentType.contains("webp")) return "webp";
            if (contentType.contains("gif")) return "gif";
        }

        return "jpg"; // 기본값
    }

    private static String contentTypeFor(String format, String fallback) {
        return switch (format) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "webp"        -> "image/webp";
            default -> (fallback != null ? fallback : "application/octet-stream");
        };
    }


    /** 압축 성능 모니터링 **/
    private void logCompressionStats(String operation, long startTime, long fileSize) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 0) { // 0으로 나누기 방지
            double mbPerSecond = (fileSize / 1024.0 / 1024.0) / (duration / 1000.0);
            log.debug("{} 성능 - 소요시간: {}ms, 처리속도: {} MB/s",
                    operation, duration, String.format("%.2f", mbPerSecond));
        }
    }
}
