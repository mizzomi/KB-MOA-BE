package org.moa.trip.service;

import lombok.RequiredArgsConstructor;
import org.moa.trip.dto.record.TripRecordCardDto;
import org.moa.trip.dto.record.TripRecordDetailResponseDto;
import org.moa.trip.dto.record.TripRecordRequestDto;
import org.moa.trip.dto.record.TripRecordResponseDto;
import org.moa.trip.entity.TripRecord;
import org.moa.trip.entity.TripRecordImage;
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

    // 여행 기록 생성
    @Override
    @Transactional
    public TripRecordResponseDto createRecord(Long tripId, Long memberId, TripRecordRequestDto dto) {
        // 요청 DTO를 엔티티로 변환
        TripRecord newRecord = TripRecord.builder()
                .tripId(tripId)
                .title(dto.getTitle())
                .recordDate(dto.getRecordDate())
                .content(dto.getContent())
                .build();

        tripRecordMapper.insertTripRecord(newRecord);

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
}
