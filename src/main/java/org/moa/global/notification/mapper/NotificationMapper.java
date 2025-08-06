package org.moa.global.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.moa.global.notification.entity.Notification;

import java.util.List;

@Mapper
public interface NotificationMapper {
    void createNotification(Notification notification);

    List<Notification> searchNotificationsByMemberIdAndUnread(@Param("memberId")Long memberId);

    void readNotification(@Param("notificationId") Long notificationId);
}
