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
import com.zyyyys.culinarywhispers.module.user.dto.UserUpdateDTO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.mapper.UserMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;
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
    private final UserProfileMapper profileMapper;

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 用户ID
     */
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

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return Token
     */
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

    /**
     * 获取用户画像
     * @param userId 用户ID
     * @return 画像VO
     */
    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        UserProfile profile = profileMapper.selectById(userId);
        // 如果没有画像，返回空对象或初始化
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            // 这里可以选择插入一条默认记录，或者直接返回空
        }
        
        return UserProfileVO.from(user, profile);
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param updateDTO 更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserUpdateDTO updateDTO) {
        // 1. 更新基础信息 (User)
        User user = new User();
        user.setId(userId);
        boolean updateUser = false;
        
        if (StrUtil.isNotBlank(updateDTO.getNickname())) {
            user.setNickname(updateDTO.getNickname());
            updateUser = true;
        }
        if (StrUtil.isNotBlank(updateDTO.getAvatarUrl())) {
            user.setAvatarUrl(updateDTO.getAvatarUrl());
            updateUser = true;
        }
        
        if (updateUser) {
            this.updateById(user);
        }
        
        // 2. 更新扩展信息 (UserProfile)
        UserProfile profile = profileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            // 初始化其他字段
            profile.setGender(updateDTO.getGender() != null ? updateDTO.getGender() : 0);
            profile.setSignature(updateDTO.getSignature());
            profile.setCity(updateDTO.getCity());
            profile.setOccupation(updateDTO.getJob());
            profileMapper.insert(profile);
        } else {
            if (updateDTO.getGender() != null) {
                profile.setGender(updateDTO.getGender());
            }
            if (updateDTO.getSignature() != null) {
                profile.setSignature(updateDTO.getSignature());
            }
            if (updateDTO.getCity() != null) {
                profile.setCity(updateDTO.getCity());
            }
            if (updateDTO.getJob() != null) {
                profile.setOccupation(updateDTO.getJob());
            }
            profileMapper.updateById(profile);
        }
    }

    /**
     * 更新用户总消费金额
     * @param userId 用户ID
     * @param amount 消费金额
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTotalSpend(Long userId, java.math.BigDecimal amount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        UserProfile profile = profileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setTotalSpend(amount);
            profileMapper.insert(profile);
        } else {
            java.math.BigDecimal current = profile.getTotalSpend() == null ? java.math.BigDecimal.ZERO : profile.getTotalSpend();
            profile.setTotalSpend(current.add(amount));
            profileMapper.updateById(profile);
        }
    }
}
