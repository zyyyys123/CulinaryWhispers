package com.zyyyys.culinarywhispers.module.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.notify.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<Page<NotificationVO>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(notificationService.pageMyNotifications(userId, page, size));
    }

    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.getUserId();
        return Result.success(notificationService.countUnread(userId));
    }

    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserId();
        notificationService.markRead(userId, id);
        return Result.success(null);
    }
}

