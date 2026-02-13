package com.zyyyys.culinarywhispers.module.user.controller;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserUpdateDTO;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.zyyyys.culinarywhispers.module.user.service.UserStatsService;
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;
import com.zyyyys.culinarywhispers.module.user.vo.UserStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserStatsService userStatsService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        Long userId = userService.register(registerDTO);
        return Result.success(userId);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        return Result.success(token);
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Long userId = SecurityUtil.getUserId();
        return Result.success(userService.getProfile(userId));
    }

    @GetMapping("/profile/{userId}")
    public Result<UserProfileVO> getPublicProfile(@PathVariable Long userId) {
        return Result.success(userService.getPublicProfile(userId));
    }
    
    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody @Valid UserUpdateDTO updateDTO) {
        Long userId = SecurityUtil.getUserId();
        userService.updateProfile(userId, updateDTO);
        return Result.success();
    }

    /**
     * 获取用户成长数据 (等级、经验、勋章)
     */
    @GetMapping("/stats")
    public Result<UserStatsVO> getUserStats() {
        Long userId = SecurityUtil.getUserId();
        return Result.success(userStatsService.getUserStats(userId));
    }

    @GetMapping("/stats/{userId}")
    public Result<UserStatsVO> getUserStatsById(@PathVariable Long userId) {
        return Result.success(userStatsService.getUserStats(userId));
    }
}
