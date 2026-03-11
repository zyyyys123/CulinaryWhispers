package com.zyyyys.culinarywhispers.module.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.notify.entity.Notification;
import com.zyyyys.culinarywhispers.module.notify.vo.NotificationVO;

import java.util.Map;

public interface NotificationService extends IService<Notification> {
    Page<NotificationVO> pageMyNotifications(Long userId, int page, int size, Integer type);

    long countUnread(Long userId, Integer type);

    Map<Integer, Long> countUnreadByType(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId, Integer type);
}
