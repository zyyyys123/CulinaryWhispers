package com.zyyyys.culinarywhispers.module.user.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.utils.JwtUtil;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.mapper.UserMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

/**
 * 用户服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterDTO registerDTO) {
        String lockKey = "lock:register:" + registerDTO.getUsername();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(ResultCode.ERROR);
        }
        try {
            User existUser = this.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, registerDTO.getUsername()));
            if (existUser != null) {
                throw new BusinessException(ResultCode.USER_EXIST);
            }
            User user = new User();
            user.setUsername(registerDTO.getUsername());
            user.setNickname(registerDTO.getNickname());
            String salt = BCrypt.gensalt();
            String passwordHash = BCrypt.hashpw(registerDTO.getPassword(), salt);
            user.setSalt(salt);
            user.setPasswordHash(passwordHash);
            user.setStatus(1);
            this.save(user);
            return user.getId();
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public String login(UserLoginDTO loginDTO) {
        // 1. 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 3. 生成Token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }
}
