package com.zyyyys.culinarywhispers.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserStatsMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserStatsService;
import com.zyyyys.culinarywhispers.module.user.vo.UserStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户统计与成长服务实现
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl extends ServiceImpl<UserStatsMapper, UserStats> implements UserStatsService {

    private final UserProfileMapper userProfileMapper;

    // 简单的等级经验阈值: Level N 需要 100 * N * (N-1) / 2 * 10 
    // Lv1: 0
    // Lv2: 100
    // Lv3: 300
    // Lv4: 600
    // Lv5: 1000
    private static final int BASE_EXP = 100;

    @Override
    public UserStatsVO getUserStats(Long userId) {
        UserStats stats = this.getById(userId);
        if (stats == null) {
            initStats(userId);
            stats = this.getById(userId);
        }

        UserProfile profile = userProfileMapper.selectById(userId);

        UserStatsVO vo = new UserStatsVO();
        vo.setUserId(userId);
        vo.setLevel(stats.getLevel());
        vo.setExperience(stats.getExperience());
        vo.setTotalRecipes(stats.getTotalRecipes());
        vo.setTotalLikesReceived(stats.getTotalLikesReceived());
        vo.setTotalFans(stats.getTotalFans());

        // 计算下一级经验
        long nextLevelExp = calculateExpForLevel(stats.getLevel() + 1);
        vo.setNextLevelExperience(nextLevelExp);
        
        // 计算当前进度
        long currentLevelBaseExp = calculateExpForLevel(stats.getLevel());
        if (nextLevelExp > currentLevelBaseExp) {
            double progress = (double)(stats.getExperience() - currentLevelBaseExp) / (nextLevelExp - currentLevelBaseExp);
            vo.setLevelProgress(Math.min(1.0, Math.max(0.0, progress)));
        } else {
            vo.setLevelProgress(1.0);
        }

        // 勋章逻辑
        List<String> badges = new ArrayList<>();
        long totalLikesReceived = stats.getTotalLikesReceived() == null ? 0L : stats.getTotalLikesReceived();
        if (totalLikesReceived > 1000) {
            badges.add("人气之星");
        }
        if (profile != null && profile.getCookAge() != null && profile.getCookAge() >= 3) {
            badges.add("资深大厨");
        }
        vo.setBadges(badges);

        if (profile != null) {
            vo.setIsMasterChef(profile.getIsMasterChef());
            vo.setMasterTitle(profile.getMasterTitle());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addExperience(Long userId, int exp) {
        UserStats stats = this.getById(userId);
        if (stats == null) {
            initStats(userId);
            stats = this.getById(userId);
        }

        long newExp = stats.getExperience() + exp;
        stats.setExperience(newExp);

        // 检查升级
        int currentLevel = stats.getLevel();
        int newLevel = calculateLevel(newExp);
        if (newLevel > currentLevel) {
            stats.setLevel(newLevel);
            log.info("User {} leveled up to {}", userId, newLevel);
            // TODO: 发送升级通知/事件
        }

        this.updateById(stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementRecipeCount(Long userId) {
        UserStats stats = this.getById(userId);
        if (stats == null) {
            initStats(userId);
            stats = this.getById(userId);
        }
        stats.setTotalRecipes(stats.getTotalRecipes() + 1);
        this.updateById(stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initStats(Long userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLevel(1);
        stats.setExperience(0L);
        stats.setTotalRecipes(0);
        stats.setTotalMoments(0);
        stats.setTotalLikesReceived(0L);
        stats.setTotalCollectsReceived(0L);
        stats.setTotalFans(0);
        stats.setTotalFollows(0);
        stats.setTotalViews(0L);
        this.save(stats);
    }

    // 辅助方法: 计算达到某等级所需的总经验
    // Formula: Sum(1..L-1) * 100
    private long calculateExpForLevel(int level) {
        if (level <= 1) return 0;
        return (long) level * (level - 1) / 2 * 100;
    }

    // 辅助方法: 根据总经验计算等级
    private int calculateLevel(long exp) {
        // 反解公式: exp = n(n-1)/2 * 100
        // n^2 - n - exp/50 = 0
        // n = (1 + sqrt(1 + 4*exp/50)) / 2
        // 近似计算，或者简单的循环查找 (等级不会太高)
        int level = 1;
        while (calculateExpForLevel(level + 1) <= exp) {
            level++;
        }
        return level;
    }
}
