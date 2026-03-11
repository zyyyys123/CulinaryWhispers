package com.zyyyys.culinarywhispers.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.user.entity.PointsRecord;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户积分控制器
 * @author zyyyys
 */
@Tag(name = "积分", description = "签到与积分明细")
@RestController
@RequestMapping("/api/user/points")
@RequiredArgsConstructor
public class UserPointsController {

    private final UserPointsService pointsService;

    @Operation(summary = "每日签到")
    @PostMapping("/sign-in")
    public Result<Integer> signIn() {
        Long userId = SecurityUtil.getUserId();
        Integer points = pointsService.signIn(userId);
        return Result.success(points);
    }

    @Operation(summary = "获取积分明细")
    @GetMapping("/history")
    public Result<Page<PointsRecord>> getPointsHistory(
            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Long userId = SecurityUtil.getUserId();
        Page<PointsRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
               .orderByDesc(PointsRecord::getGmtCreate);
        return Result.success(pointsService.page(pageParam, wrapper));
    }
}
