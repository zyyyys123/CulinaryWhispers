package com.zyyyys.culinarywhispers.module.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.notify.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "通知", description = "站内通知与已读管理")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    @Operation(summary = "通知列表", description = "分页获取当前登录用户的通知列表")
    public Result<Page<NotificationVO>> list(@Parameter(description = "页码（从 1 开始）", example = "1")
                                             @RequestParam(defaultValue = "1") int page,
                                             @Parameter(description = "每页数量", example = "10")
                                             @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(notificationService.pageMyNotifications(userId, page, size));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "未读数", description = "获取当前登录用户的未读通知数量")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.getUserId();
        return Result.success(notificationService.countUnread(userId));
    }

    @PostMapping("/read/{id}")
    @Operation(summary = "标记已读", description = "将指定通知标记为已读")
    public Result<Void> markRead(@Parameter(description = "通知ID", example = "1001")
                                 @PathVariable("id") Long id) {
        Long userId = SecurityUtil.getUserId();
        notificationService.markRead(userId, id);
        return Result.success(null);
    }
}
