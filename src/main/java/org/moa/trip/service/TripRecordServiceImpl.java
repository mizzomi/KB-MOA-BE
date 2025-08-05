package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import org.moa.global.exception.ForbiddenAccessException;
import org.moa.global.exception.RecordNotFoundException;
import org.moa.global.handler.FileUploadException;
import org.moa.global.service.FirebaseStorageService;
import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.TripRecordDetailResponseDto;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripRecordServiceImpl implements TripRecordService {
    private final TripRecordMapper tripRecordMapper;
    private final TripRecordImageMapper tripRecordImageMapper;
    private final TripMemberMapper tripMemberMapper;
    private final FirebaseStorageService firebaseStorageService;

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
        uploadAndSaveImages(dto.getImages(), newRecord.getRecordId());

        // createdAt같은 DB 자동 생성 값을 가져오기 위해서 방금 생성한 완전한 값을 다시 조회
        TripRecord savedRecord = tripRecordMapper.findRecordById(newRecord.getRecordId());

        return TripRecordResponseDto.fromEntity(savedRecord);
    }

    
    /** 일자별 여행 기록 조회 **/
    @Override
    @Transactional(readOnly = true)
    public Page<TripRecordCardDto> getRecordsByDate(Long tripId, LocalDate date, Pageable pageable) {
        List<TripRecordCardDto> records = tripRecordMapper.findRecordsByDate(tripId, date, pageable);

        // 이미지 불러오기
        records.forEach(record -> {
            // DTO의 imageUrls 리스트가 비어있지 않다면,
            if (record.getImageUrls() != null && !record.getImageUrls().isEmpty()) {
                // 파일 이름 리스트를 서명된 URL 리스트로 변환
                List<String> signedUrls = record.getImageUrls().stream()
                        .filter(Objects::nonNull) // LEFT JOIN으로 인해 null이 포함될 수 있으므로 필터링
                        .map(firebaseStorageService::getSignedUrl)
                        .filter(Objects::nonNull) // URL 생성 실패 시 null을 필터링
                        .collect(Collectors.toList());
                record.setImageUrls(signedUrls); // DTO의 리스트를 교체
            }
        });

        int total = tripRecordMapper.countRecordsByDate(tripId, date);

        return new PageImpl<>(records, pageable, total);
    }

    
    /** 여행 기록 상세 조회 **/
    @Override
    @Transactional(readOnly = true)
    public TripRecordDetailResponseDto getRecordDetail(Long tripId, Long recordId) {
        // tripId와 recordId를 모두 사용해서 조회
        TripRecord tripRecord = tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        
        if (tripRecord == null) {
            throw new RecordNotFoundException();
        }
        
        // DB에서 이미지 파일 이름 목록 조회
        List<String> imageFileNames = tripRecordMapper.findImageUrlsByRecordId(recordId);

        // 파일 이름 목록을 서명된 URL 목록으로 변환 (null은 필터링)
        List<String> imageUrls = imageFileNames.stream()
                .map(firebaseStorageService::getSignedUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 조회된 정보들을 DTO로 조합하여 반환
        return TripRecordDetailResponseDto.of(tripRecord, imageUrls);
    }

    
    /** 여행 기록 수정 **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripRecordResponseDto updateRecord(Long tripId, Long recordId, Long memberId, TripRecordRequestDto dto) {
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

        // DB에서 이미지 정보를 삭제하기 전에, Storage에서 실제 파일을 먼저 삭제
        List<String> oldImageFileNames = tripRecordMapper.findImageUrlsByRecordId(recordId);
        oldImageFileNames.forEach(firebaseStorageService::deleteFile);

        // 이미지 정보 업데이트( 기존 이미지 모두 삭제 후 새로 추가 )
        tripRecordImageMapper.deleteImagesByRecordId(recordId);

        // 이미지 저장 처리
        uploadAndSaveImages(dto.getImages(), recordId);

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

        // DB에서 이미지 정보를 삭제하기 전에, Storage에서 실제 파일을 먼저 삭제
        List<String> imageFileNamesToDelete = tripRecordMapper.findImageUrlsByRecordId(recordId);
        imageFileNamesToDelete.forEach(firebaseStorageService::deleteFile);

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

        for (var imageFile : imageFiles) {
            // 리스트 안에 혹시 모를 빈 파일이 섞여있으면 건너뛰기
            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }
            try {
                String storedFileName = firebaseStorageService.uploadAndGetFileName(imageFile);
                TripRecordImage recordImage = TripRecordImage.builder().recordId(recordId).imageUrl(storedFileName).build();
                tripRecordImageMapper.insertTripRecordImage(recordImage);
            } catch (IOException e) {
                throw new FileUploadException("파일 업로드에 실패했습니다.", e);
            }
        }
    }
}
