package com.zyyyys.culinarywhispers.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.user.entity.PointsRecord;

/**
 * 积分服务接口
 * @author zyyyys
 */
public interface UserPointsService extends IService<PointsRecord> {

    /**
     * 用户每日签到
     * @param userId 用户ID
     * @return 获得的积分
     */
    Integer signIn(Long userId);

    /**
     * 增加积分 (通用方法)
     * @param userId 用户ID
     * @param amount 积分数量
     * @param type 类型
     * @param description 说明
     */
    void addPoints(Long userId, Integer amount, Integer type, String description);
}
