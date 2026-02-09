package com.zyyyys.culinarywhispers.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserUpdateDTO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;

/**
 * 用户服务接口
 * @author zyyyys
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 用户ID
     */
    Long register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    String login(UserLoginDTO loginDTO);

    /**
     * 更新用户总消费金额
     * @param userId 用户ID
     * @param amount 消费金额
     */
    void updateTotalSpend(Long userId, java.math.BigDecimal amount);

    /**
     * 获取用户画像实体 (内部调用)
     * @param userId 用户ID
     * @return 画像实体
     */
    UserProfile getUserProfile(Long userId);
    /**
     * 获取用户画像
     * @param userId 用户ID
     * @return 画像VO
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param updateDTO 更新信息
     */
    void updateProfile(Long userId, UserUpdateDTO updateDTO);
}
