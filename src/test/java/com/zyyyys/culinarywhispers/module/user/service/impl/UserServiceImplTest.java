package com.zyyyys.culinarywhispers.module.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.utils.JwtUtil;
import com.zyyyys.culinarywhispers.module.user.dto.UserLoginDTO;
import com.zyyyys.culinarywhispers.module.user.dto.UserRegisterDTO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.mapper.UserMapper;
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
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    // Use spy instead of InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // Mock Redis operations
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Initialize and Spy
        UserServiceImpl realService = new UserServiceImpl(jwtUtil, stringRedisTemplate);
        userService = spy(realService);
        
        // Inject baseMapper into Spy
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void register_Success() {
        // Arrange
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        dto.setNickname("Tester");

        // Mock lock acquisition success
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        
        // Mock getOne to return null (user not exist)
        doReturn(null).when(userService).getOne(any());
        
        // Mock insert
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        // Act
        Long userId = userService.register(dto);

        // Assert
        assertNotNull(userId);
        assertEquals(1L, userId);
        verify(stringRedisTemplate, times(1)).delete(anyString());
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void register_UserExists() {
        // Arrange
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("existingUser");
        dto.setPassword("password123");
        dto.setNickname("Existing User");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("existingUser");
        
        // Mock getOne to return existing user
        doReturn(existingUser).when(userService).getOne(any());

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(ResultCode.USER_EXIST.getCode(), exception.getCode());
        verify(stringRedisTemplate, times(1)).delete(anyString());
    }

    @Test
    void register_LockFailed() {
        // Arrange
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("concurrentuser");

        // Mock lock acquisition failure
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(ResultCode.ERROR.getCode(), exception.getCode());
    }

    @Test
    void login_Success() {
        // Arrange
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(BCrypt.hashpw("password123", BCrypt.gensalt()));

        // Mock getOne to return user
        doReturn(user).when(userService).getOne(any());
        
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("mock-token");

        // Act
        String token = userService.login(dto);

        // Assert
        assertEquals("mock-token", token);
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("unknown");
        dto.setPassword("password");

        // Mock getOne to return null
        doReturn(null).when(userService).getOne(any());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    void login_WrongPassword() {
        // Arrange
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(BCrypt.hashpw("password123", BCrypt.gensalt()));

        // Mock getOne to return user
        doReturn(user).when(userService).getOne(any());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(dto));
        assertEquals(ResultCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }
}
