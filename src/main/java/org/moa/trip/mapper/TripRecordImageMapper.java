package org.moa.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.trip.entity.TripRecordImage;

import java.util.List;

@Mapper
public interface TripRecordImageMapper {
    // 이미지 저장
    void insertTripRecordImage(TripRecordImage recordImage);

    // 특정 여행 기록에 속한 모든 이미지를 삭제하는 메소드
    void deleteImagesByRecordId(Long recordId);

    void deleteImagesByRecordIdAndFileNames(@Param("recordId") Long recordId, @Param("fileNames") List<String> fileNames);

}
