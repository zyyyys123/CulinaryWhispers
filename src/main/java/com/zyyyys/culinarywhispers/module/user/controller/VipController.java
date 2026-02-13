package com.zyyyys.culinarywhispers.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserStatsMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import com.zyyyys.culinarywhispers.module.user.vo.VipPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/vip")
@RequiredArgsConstructor
public class VipController {

    private final UserStatsMapper statsMapper;
    private final UserProfileMapper profileMapper;
    private final UserPointsService pointsService;

    private List<VipPlanVO> buildPlans() {
        VipPlanVO vip1 = new VipPlanVO();
        vip1.setLevel(1);
        vip1.setName("VIP 青铜");
        vip1.setCostPoints(200);
        vip1.setDurationDays(7);
        vip1.setBenefits(new String[]{
                "专属勋章与个人页标识",
                "发帖/评论积分加成",
                "更丰富的推荐权重"
        });

        VipPlanVO vip2 = new VipPlanVO();
        vip2.setLevel(2);
        vip2.setName("VIP 白银");
        vip2.setCostPoints(500);
        vip2.setDurationDays(30);
        vip2.setBenefits(new String[]{
                "专属勋章与个人页标识",
                "发帖/评论积分加成",
                "更丰富的推荐权重",
                "AI 助手高级提示占位"
        });

        VipPlanVO vip3 = new VipPlanVO();
        vip3.setLevel(3);
        vip3.setName("VIP 黄金");
        vip3.setCostPoints(1500);
        vip3.setDurationDays(90);
        vip3.setBenefits(new String[]{
                "专属勋章与个人页标识",
                "发帖/评论积分加成",
                "更丰富的推荐权重",
                "AI 助手高级提示占位",
                "专属活动优先参与"
        });

        return List.of(vip1, vip2, vip3);
    }

    @GetMapping("/plans")
    public Result<List<VipPlanVO>> plans() {
        return Result.success(buildPlans());
    }

    @PostMapping("/exchange")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserProfile> exchange(@RequestParam Integer level) {
        Long userId = SecurityUtil.getUserId();
        if (level == null || level < 1 || level > 3) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        VipPlanVO plan = buildPlans().stream().filter(p -> p.getLevel().equals(level)).findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.VALIDATE_FAILED));

        UserStats stats = statsMapper.selectById(userId);
        if (stats == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        int points = stats.getPoints() == null ? 0 : stats.getPoints();
        if (points < plan.getCostPoints()) {
            throw new BusinessException(400, "积分不足");
        }

        UserProfile profile = profileMapper.selectById(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        Integer currentLevel = profile.getVipLevel() == null ? 0 : profile.getVipLevel();
        LocalDateTime currentExpire = profile.getVipExpireTime();
        boolean currentActive = currentExpire != null && currentExpire.isAfter(now) && currentLevel > 0;

        if (currentActive && currentLevel > level) {
            throw new BusinessException(400, "当前 VIP 等级更高，无法兑换更低等级");
        }

        LocalDateTime baseTime = currentActive && currentLevel.equals(level) ? currentExpire : now;
        profile.setVipLevel(level);
        profile.setVipExpireTime(baseTime.plusDays(plan.getDurationDays()));
        profileMapper.updateById(profile);

        pointsService.addPoints(userId, -plan.getCostPoints(), 10, "积分兑换 " + plan.getName() + "（" + plan.getDurationDays() + "天）");

        return Result.success(profileMapper.selectById(userId));
    }

    @GetMapping("/me")
    public Result<UserProfile> me() {
        Long userId = SecurityUtil.getUserId();
        UserProfile profile = profileMapper.selectById(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return Result.success(profile);
    }
}
