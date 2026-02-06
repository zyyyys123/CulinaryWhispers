package com.zyyyys.culinarywhispers.module.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
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
import com.zyyyys.culinarywhispers.module.user.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    // Use spy instead of InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // 模拟 Redis 操作
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 初始化并创建 Spy
        UserServiceImpl realService = new UserServiceImpl(jwtUtil, stringRedisTemplate, profileMapper);
        userService = spy(realService);
        
        // 将 baseMapper 注入 Spy
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void register_Success() {
        // 准备数据
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        dto.setNickname("Tester");

        // 模拟获取锁成功
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        
        // 模拟 getOne 返回 null（用户不存在）
        doReturn(null).when(userService).getOne(any());
        
        // 模拟插入
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        // 执行操作
        Long userId = userService.register(dto);

        // 断言结果
        assertNotNull(userId);
        assertEquals(1L, userId);
        verify(stringRedisTemplate, times(1)).delete(anyString());
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void register_UserExists() {
        // 准备数据
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("existingUser");
        dto.setPassword("password123");
        dto.setNickname("Existing User");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("existingUser");
        
        // 模拟 getOne 返回已存在的用户
        doReturn(existingUser).when(userService).getOne(any());

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // 执行并断言
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(ResultCode.USER_EXIST.getCode(), exception.getCode());
        verify(stringRedisTemplate, times(1)).delete(anyString());
    }

    @Test
    void register_LockFailed() {
        // 准备数据
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("concurrentuser");

        // 模拟获取锁失败
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // 执行并断言
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(ResultCode.ERROR.getCode(), exception.getCode());
    }

    @Test
    void login_Success() {
        // 准备数据
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(BCrypt.hashpw("password123", BCrypt.gensalt()));

        // 模拟 getOne 返回用户
        doReturn(user).when(userService).getOne(any());
        
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("mock-token");

        // 执行操作
        String token = userService.login(dto);

        // 断言结果
        assertEquals("mock-token", token);
    }

    @Test
    void login_UserNotFound() {
        // 准备数据
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("unknown");
        dto.setPassword("password");

        // 模拟 getOne 返回 null
        doReturn(null).when(userService).getOne(any());

        // 执行并断言
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    void login_WrongPassword() {
        // 准备数据
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(BCrypt.hashpw("password123", BCrypt.gensalt()));

        // 模拟 getOne 返回用户
        doReturn(user).when(userService).getOne(any());

        // 执行并断言
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals(ResultCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getProfile_Success() {
        // 准备数据
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setNickname("Tester");

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setGender(1);
        profile.setSignature("Hello");

        doReturn(user).when(userService).getById(userId);
        when(profileMapper.selectById(userId)).thenReturn(profile);

        // 执行操作
        UserProfileVO vo = userService.getProfile(userId);

        // 断言结果
        assertNotNull(vo);
        assertEquals(userId, vo.getId());
        assertEquals("Tester", vo.getNickname());
        assertEquals(1, vo.getGender());
        assertEquals("Hello", vo.getSignature());
    }

    @Test
    void getProfile_UserNotFound() {
        // 准备数据
        Long userId = 99L;
        doReturn(null).when(userService).getById(userId);

        // 执行并断言
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getProfile(userId));
        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    void getProfile_NoProfileData() {
        // 准备数据
        Long userId = 2L;
        User user = new User();
        user.setId(userId);
        
        doReturn(user).when(userService).getById(userId);
        when(profileMapper.selectById(userId)).thenReturn(null);

        // 执行操作
        UserProfileVO vo = userService.getProfile(userId);

        // 断言结果
        assertNotNull(vo);
        assertEquals(userId, vo.getId());
        assertNull(vo.getSignature()); // 默认为 null
    }

    @Test
    void updateProfile_Success() {
        // 准备数据
        Long userId = 1L;
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("NewNick");
        dto.setGender(1);
        dto.setSignature("NewSig");

        // 模拟查询画像
        UserProfile existingProfile = new UserProfile();
        existingProfile.setUserId(userId);
        when(profileMapper.selectById(userId)).thenReturn(existingProfile);

        // 执行操作
        userService.updateProfile(userId, dto);

        // 验证交互
        verify(userService, times(1)).updateById(any(User.class));
        verify(profileMapper, times(1)).updateById(any(UserProfile.class));
    }
}
