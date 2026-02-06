package com.zyyyys.culinarywhispers.module.user.controller;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;
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
        // TODO: 从 Token 获取 userId
        Long userId = 1L; 
        return Result.success(userService.getProfile(userId));
    }
}
