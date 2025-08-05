package org.moa.global.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.moa.global.notification.entity.Notification;

@Mapper
public interface NotificationMapper {
    void createNotification(Notification notification);
}
