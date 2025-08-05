package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.trip.entity.TripRecordImage;

@Mapper
public interface TripRecordImageMapper {
    // 이미지 저장
    void insertTripRecordImage(TripRecordImage recordImage);

    // 특정 여행 기록에 속한 모든 이미지를 삭제하는 메소드
    void deleteImagesByRecordId(Long recordId);

}
