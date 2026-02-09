package com.zyyyys.culinarywhispers.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.user.entity.PointsRecord;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.mapper.PointsRecordMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserStatsMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 积分服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPointsServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements UserPointsService {

    private final UserStatsMapper statsMapper;

    /**
     * 基础签到积分
     */
    private static final int BASE_SIGN_POINTS = 10;
    
    /**
     * 连续签到额外奖励 (最大值)
     */
    private static final int MAX_CONTINUE_BONUS = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer signIn(Long userId) {
        UserStats stats = statsMapper.selectById(userId);
        if (stats == null) {
            // 初始化统计信息 (理论上注册时应初始化，这里做兜底)
            stats = new UserStats();
            stats.setUserId(userId);
            stats.setPoints(0);
            stats.setContinueSignDays(0);
            statsMapper.insert(stats);
        }

        LocalDate today = LocalDate.now();
        LocalDate lastSignDate = stats.getLastSignDate();

        if (lastSignDate != null && lastSignDate.isEqual(today)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "今日已签到");
        }

        // 计算连续签到天数
        int continueDays = 1;
        if (lastSignDate != null && lastSignDate.plusDays(1).isEqual(today)) {
            continueDays = (stats.getContinueSignDays() == null ? 0 : stats.getContinueSignDays()) + 1;
        }

        // 计算积分: 基础分 + min(连续天数 * 5, 50)
        int bonus = Math.min((continueDays - 1) * 5, MAX_CONTINUE_BONUS);
        int pointsEarned = BASE_SIGN_POINTS + bonus;

        // 更新用户状态
        stats.setLastSignDate(today);
        stats.setContinueSignDays(continueDays);
        stats.setPoints((stats.getPoints() == null ? 0 : stats.getPoints()) + pointsEarned);
        statsMapper.updateById(stats);

        // 记录流水
        addPointsRecord(userId, pointsEarned, 1, "每日签到 (连续" + continueDays + "天)");

        log.info("User {} signed in. Days: {}, Points: {}", userId, continueDays, pointsEarned);
        return pointsEarned;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer amount, Integer type, String description) {
        UserStats stats = statsMapper.selectById(userId);
        if (stats != null) {
            stats.setPoints((stats.getPoints() == null ? 0 : stats.getPoints()) + amount);
            statsMapper.updateById(stats);
        }
        addPointsRecord(userId, amount, type, description);
    }

    private void addPointsRecord(Long userId, Integer amount, Integer type, String description) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setDescription(description);
        this.save(record);
    }
}
