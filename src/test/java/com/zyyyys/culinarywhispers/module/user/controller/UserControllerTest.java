package com.zyyyys.culinarywhispers.module.user.controller;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserUpdateDTO;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.zyyyys.culinarywhispers.module.user.service.UserStatsService;
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;
import com.zyyyys.culinarywhispers.module.user.vo.UserStatsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void register_login_profile_stats_updateProfile() {
        UserService userService = mock(UserService.class);
        UserStatsService userStatsService = mock(UserStatsService.class);
        UserController controller = new UserController(userService, userStatsService);

        when(userService.register(any(UserRegisterDTO.class))).thenReturn(1L);
        when(userService.login(any(UserLoginDTO.class))).thenReturn("t");

        UserContext.setUserId(1L);
        when(userService.getProfile(1L)).thenReturn(new UserProfileVO());
        when(userStatsService.getUserStats(1L)).thenReturn(new UserStatsVO());

        Result<Long> register = controller.register(new UserRegisterDTO());
        assertEquals(0, register.getCode());

        Result<String> login = controller.login(new UserLoginDTO());
        assertEquals(0, login.getCode());

        Result<UserProfileVO> profile = controller.getProfile();
        assertEquals(0, profile.getCode());

        Result<UserStatsVO> stats = controller.getUserStats();
        assertEquals(0, stats.getCode());

        Result<Void> update = controller.updateProfile(new UserUpdateDTO());
        assertEquals(0, update.getCode());
        verify(userService).updateProfile(eq(1L), any(UserUpdateDTO.class));
    }
}
