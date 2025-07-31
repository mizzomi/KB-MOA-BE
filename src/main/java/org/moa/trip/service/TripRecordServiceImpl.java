package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripRecordServiceImpl implements TripRecordService {
    private final TripRecordMapper tripRecordMapper;
    private final TripRecordImageMapper tripRecordImageMapper;
    private final TripMemberMapper tripMemberMapper;

    // 여행 기록 생성
    @Override
    @Transactional
    public TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto) {
        // 요청 DTO를 엔티티로 변환
        TripRecord newRecord = TripRecord.builder()
                .tripId(tripId)
                .memberId(memberId)
                .title(dto.getTitle())
                .recordDate(dto.getRecordDate())
                .content(dto.getContent())
                .build();

        tripRecordMapper.insertTripRecord(newRecord);

        // 이미지가 있으면 이미지 저장
        List<String> imageUrls = dto.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                TripRecordImage recordImage = TripRecordImage.builder()
                        .recordId(newRecord.getRecordId())
                        .imageUrl(url)
                        .build();
                tripRecordImageMapper.insertTripRecordImage(recordImage);
            }
        }

        // createdAt같은 DB 자동 생성 값을 가져오기 위해서 방금 생성한 완전한 값을 다시 조회
        TripRecord savedRecord = tripRecordMapper.findRecordById(newRecord.getRecordId());

        return TripRecordResponseDto.fromEntity(savedRecord);
    }


    // 여행 기록 일자별로 조회
    @Override
    public Page<TripRecordCardDto> getRecordsByDate(Long tripId, LocalDate date, Pageable pageable) {
        List<TripRecordCardDto> records = tripRecordMapper.findRecordsByDate(tripId, date, pageable);

        int total = tripRecordMapper.countRecordsByDate(tripId, date);

        return new PageImpl<>(records, pageable, total);
    }


    // 여행기록 상세 조회
    @Override
    @Transactional(readOnly = true)
    public TripRecordDetailResponseDto getRecordDetail(Long tripId, Long recordId) {
        // tripId와 recordId를 모두 사용해서 조회
        TripRecord tripRecord = tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        if (tripRecord == null) {
            throw new RuntimeException("해당 여행 기록을 찾을 수 없습니다.");
        }
        // recordId로 연관된 이미지 URL 목록 조회
        List<String> imageUrls = tripRecordMapper.findImageUrlsByRecordId(recordId);
        // 조회된 정보들을 DTO로 조합하여 반환
        return TripRecordDetailResponseDto.of(tripRecord, imageUrls);
    }


    // 여행기록 수정 - TODO: [REFACTOR] 예외 처리들을 RecordNotFoundException과 같은 구체적인 예외로 변경
    @Override
    @Transactional
    public TripRecordResponseDto updateRecord(Long tripId, Long recordId, Long memberId, TripRecordRequestDto dto) {
        // 수정하려는 기록이 존재하는지 확인
        TripRecord existingRecord = tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        if (existingRecord == null) {
            throw new RuntimeException("해당 여행 기록을 찾을 수 없습니다.");
        }
        // 여행 멤버인지 확인
        if (tripMemberMapper.isMemberOfTrip(tripId, memberId)== 0) {
            throw new RuntimeException("수정할 권한이 없습니다.");
        }

        // DTO 내용으로 기존 엔티티 정보 업데이트
        TripRecord updateRecord = TripRecord.builder()
                .recordId(recordId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .recordDate(dto.getRecordDate())
                .build();
        tripRecordMapper.updateTripRecord(updateRecord);

        // 이미지 정보 업데이트( 기존 이미지 모두 삭제 후 새로 추가 )
        tripRecordImageMapper.deleteImagesByRecordId(recordId);

        List<String> imageUrls = dto.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                TripRecordImage recordImage = TripRecordImage.builder()
                        .recordId(recordId)
                        .imageUrl(url)
                        .build();
                tripRecordImageMapper.insertTripRecordImage(recordImage);

            }
        }

        // 업데이트된 정보를 다시 조회하여 반환
        TripRecord finalRecord = tripRecordMapper.findRecordById(recordId);
        return TripRecordResponseDto.fromEntity(finalRecord);
    }


    // 여행 기록 삭제 - TODO: [REFACTOR] 예외 처리들을 RecordNotFoundException과 같은 구체적인 예외로 변경
    @Override
    @Transactional
    public void deleteRecord(Long tripId, Long recordId, Long memberId) {
        // 삭제하려는 기록이 존재하는지 확인
        TripRecord existingRecord = tripRecordMapper.findRecordByTripIdAndRecordId(tripId, recordId);
        if (existingRecord == null) {
            throw new RuntimeException("해당 여행 기록을 찾을 수 없습니다.");
            // return; // 그냥 종료
        }

        // 기록의 작성자와 현재 로그인한 사용자가 같은지 확인
        if (!existingRecord.getMemberId().equals(memberId)) {
            throw new RuntimeException("삭제할 권한이 없습니다.");
        }

        // 자식 테이블(이미지) 데이터 먼저 삭제
        // ON DELETE CASCADE 때문에 안해도 될듯
        // tripRecordImageMapper.deleteImagesByRecordId(recordId);

        // 부모 테이블(기록) 데이터 삭제
        tripRecordMapper.deleteTripRecord(recordId);
    }
}
