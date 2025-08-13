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
            CompletableFuture<Void> deleteStorageFuture = CompletableFuture.allOf(
                    imagesToDelete.stream()
                            .map(fileName -> CompletableFuture.runAsync(() -> firebaseStorageService.deleteFile(fileName), ioTaskExecutor))
                            .toArray(CompletableFuture[]::new)
            );

            // DB에서 이미지 정보 삭제
            tripRecordImageMapper.deleteImagesByRecordIdAndFileNames(recordId, imagesToDelete);

            deleteStorageFuture.join();
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
            CompletableFuture.allOf(imageFileNamesToDelete.stream()
                    .map(fileName -> CompletableFuture.runAsync(() -> firebaseStorageService.deleteFile(fileName), ioTaskExecutor))
                    .toArray(CompletableFuture[]::new)
            ).join();
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
                .filter(file -> file != null && !file.isEmpty()) // 유효한 파일만 필터링
                .map(imageFile -> CompletableFuture.runAsync(() -> {
                    try {
                        // 이미지 리사이징 및 압축
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        Thumbnails.of(imageFile.getInputStream())
                                .size(1024, 1024)
                                .outputQuality(0.85)
                                .toOutputStream(outputStream);

                        byte[] resizedImageBytes = outputStream.toByteArray();
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImageBytes);

                        // 리사이징된 이미지를 Firebase에 업로드
                        String storedFileName = firebaseStorageService.uploadAndGetFileName(
                                inputStream, imageFile.getOriginalFilename(), imageFile.getContentType(), resizedImageBytes.length);

                        TripRecordImage recordImage = TripRecordImage.builder()
                                .recordId(recordId)
                                .imageUrl(storedFileName)
                                .build();
                        tripRecordImageMapper.insertTripRecordImage(recordImage);
                    } catch (IOException e) {
                        // 비동기 작업 내에서 발생하는 체크 예외를 런타임 예외로 래핑하여 전파
                        throw new FileUploadException("파일 업로드 중 오류 발생: " + imageFile.getOriginalFilename(), e);
                    }
                }, ioTaskExecutor)).toList();

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
    }
}
