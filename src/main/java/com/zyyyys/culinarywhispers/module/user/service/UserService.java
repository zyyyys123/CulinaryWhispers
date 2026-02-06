package com.zyyyys.culinarywhispers.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.entity.User;

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
     * @return JWT Token
     */
    String login(UserLoginDTO loginDTO);
}
