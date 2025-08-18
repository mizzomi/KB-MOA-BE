package org.moa.trip.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.moa.global.exception.ForbiddenAccessException;
import org.moa.global.exception.RecordNotFoundException;
import org.moa.global.handler.FileUploadException;
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
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TripRecordServiceImpl implements TripRecordService {
    private final TripRecordMapper tripRecordMapper;
    private final TripRecordImageMapper tripRecordImageMapper;
    private final TripMemberMapper tripMemberMapper;
    private final FirebaseStorageService firebaseStorageService;

    private final Executor ioTaskExecutor;

    private record ProcessingResult(TripRecordCardDto record, long duration) {
    }

    private record ProcessedImage(byte[] bytes, String format, String contentType,
                                  long originalSize, long finalSize, boolean resized) {}

    public TripRecordServiceImpl(TripRecordMapper tripRecordMapper,
                                 TripRecordImageMapper tripRecordImageMapper,
                                 TripMemberMapper tripMemberMapper,
                                 FirebaseStorageService firebaseStorageService,
                                 @Qualifier("ioTaskExecutor") Executor ioTaskExecutor) {
        this.tripRecordMapper = tripRecordMapper;
        this.tripRecordImageMapper = tripRecordImageMapper;
        this.tripMemberMapper = tripMemberMapper;
        this.firebaseStorageService = firebaseStorageService;
        this.ioTaskExecutor = ioTaskExecutor;
    }

    /** 여행 기록 생성 **/
    @Override
    @Transactional(rollbackFor = Exception.class) // 모든 예외에 대해 롤백 수행(파일 업로드시 예외에 대해서도 동작하도록)
    public TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto) {
        // 기록 생성 권한 확인(여행 멤버인지)
        if (tripMemberMapper.isMemberOfTrip(tripId, memberId) == 0) {
            throw new ForbiddenAccessException("기록을 생성할 권한이 없습니다.");
        }

        TripRecord newRecord = TripRecord.builder()
                .tripId(tripId)
                .memberId(memberId)
                .title(dto.getTitle())
                .recordDate(dto.getRecordDate())
                .content(dto.getContent())
                .build();

        tripRecordMapper.insertTripRecord(newRecord);

        // 이미지 저장 처리
        uploadAndSaveImages(dto.getImageUrls(), newRecord.getRecordId());

        // createdAt같은 DB 자동 생성 값을 가져오기 위해서 방금 생성한 완전한 값을 다시 조회
        TripRecord savedRecord = tripRecordMapper.findRecordById(newRecord.getRecordId());

        return TripRecordResponseDto.fromEntity(savedRecord);
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

    
    /** 여행 기록 수정 **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripRecordResponseDto updateRecord(Long tripId, Long recordId, Long memberId, TripRecordUpdateRequestDto dto) {
        // 수정 권한 확인
        checkRecordAuthority(tripId, recordId, memberId, "수정");

        // DTO 내용으로 기존 엔티티 정보 업데이트
        TripRecord updateRecord = TripRecord.builder()
                .recordId(recordId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .recordDate(dto.getRecordDate())
                .build();
        tripRecordMapper.updateTripRecord(updateRecord);

        // DB에 저장된 현재 이미지 파일 목록 조회
        List<String> currentDbImages = tripRecordMapper.findImageUrlsByRecordId(recordId);

        Set<String> imagesToKeep = dto.getExistingImageFileNames() != null ? Set.copyOf(dto.getExistingImageFileNames()) : Collections.emptySet();

        // 삭제할 이미지 목록 계산 (현재 이미지 - 유지할 이미지)
        List<String> imagesToDelete = currentDbImages.stream()
                .filter(dbImage -> !imagesToKeep.contains(dbImage))
                .toList();

        // 삭제할 이미지가 있으면 삭제 실행
        if (!imagesToDelete.isEmpty()) {
            List<CompletableFuture<Boolean>> deleteFutures = imagesToDelete.stream()
                    .map(fileName -> CompletableFuture.supplyAsync(() -> firebaseStorageService.deleteFile(fileName), ioTaskExecutor))
                    .toList();

            // DB에서 이미지 정보 삭제
            tripRecordImageMapper.deleteImagesByRecordIdAndFileNames(recordId, imagesToDelete);

            CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();

            // 삭제 실패 건수 확인 및 로깅
            long failedCount = deleteFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(success -> !success)
                    .count();
            if (failedCount > 0) {
                log.warn("여행 기록 수정 중, {}개의 이미지 파일 삭제에 실패했습니다. (recordId: {})", failedCount, recordId);
            }
        }

        // 새로 추가할 이미지가 있으면 업로드 및 DB 저장
        uploadAndSaveImages(dto.getNewImages(), recordId);

        // 업데이트된 정보를 다시 조회하여 반환
        TripRecord finalRecord = tripRecordMapper.findRecordById(recordId);
        return TripRecordResponseDto.fromEntity(finalRecord);
    }

    
    /** 여행 기록 삭제 **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long tripId, Long recordId, Long memberId) {
        // 삭제 권한 확인
        checkRecordAuthority(tripId, recordId, memberId, "삭제");

        // 여러 파일을 병렬로 삭제
        List<String> imageFileNamesToDelete = tripRecordMapper.findImageUrlsByRecordId(recordId);
        if (imageFileNamesToDelete != null && !imageFileNamesToDelete.isEmpty()) {
            List<CompletableFuture<Boolean>> deleteFutures = imageFileNamesToDelete.stream()
                    .map(fileName -> CompletableFuture.supplyAsync(() -> firebaseStorageService.deleteFile(fileName), ioTaskExecutor))
                    .toList();

            CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();

            // 삭제 실패 건수 확인 및 로깅
            long failedCount = deleteFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(success -> !success)
                    .count();
            if (failedCount > 0) {
                log.warn("여행 기록 삭제 중, {}개의 이미지 파일 삭제에 실패했습니다. (recordId: {})", failedCount, recordId);
            }
        }

        // 자식 테이블(이미지) 데이터 명시적 삭제
        tripRecordImageMapper.deleteImagesByRecordId(recordId);
        tripRecordMapper.deleteTripRecord(recordId);
    }

    
    /** 여행 기록에 대한 사용자의 권한을 확인하는 메서드 **/
    private void checkRecordAuthority(Long tripId, Long recordId, Long memberId, String action) {
        if (tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId) == null) {
            throw new RecordNotFoundException();
        }
        // 수정/삭제 권한 정책 통일: 여행 멤버면 수정/삭제 가능
        if (tripMemberMapper.isMemberOfTrip(tripId, memberId) == 0) {
            throw new ForbiddenAccessException("해당 기록을 " + action + "할 권한이 없습니다.");
        }
    }

    /** 이미지 파일들을 업로드하고 DB에 저장하는 메서드 **/
    private void uploadAndSaveImages(List<MultipartFile> imageFiles, Long recordId) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> uploadFutures = imageFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(imageFile -> CompletableFuture.runAsync(() -> {
                    try {
                        ProcessedImage processedImage = processImage(imageFile);

                        String storedFileName = firebaseStorageService.uploadProcessed(
                                processedImage.bytes(),
                                processedImage.format(),
                                processedImage.contentType());

                        // DB에 저장
                        TripRecordImage recordImage = TripRecordImage.builder()
                                .recordId(recordId)
                                .imageUrl(storedFileName)
                                .build();
                        tripRecordImageMapper.insertTripRecordImage(recordImage);

                    } catch (IOException e) {
                        throw new FileUploadException("파일 업로드 중 오류 발생: " + imageFile.getOriginalFilename(), e);
                    }
                }, ioTaskExecutor))
                .toList();

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
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
