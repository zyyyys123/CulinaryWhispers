package com.zyyyys.culinarywhispers.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.vo.UserStatsVO;

/**
 * 用户统计与成长服务接口
 * @author zyyyys
 */
public interface UserStatsService extends IService<UserStats> {

    /**
     * 获取用户成长数据
     * @param userId 用户ID
     * @return 统计VO
     */
    UserStatsVO getUserStats(Long userId);

    /**
     * 增加经验值 (可能会触发升级)
     * @param userId 用户ID
     * @param exp 增加的经验值
     */
    void addExperience(Long userId, int exp);

    /**
     * 增加发布食谱数
     * @param userId 用户ID
     */
    void incrementRecipeCount(Long userId);

    /**
     * 初始化用户统计数据
     * @param userId 用户ID
     */
    void initStats(Long userId);
}
