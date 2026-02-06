package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.mapper.FollowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowMapper followMapper;

    private FollowServiceImpl followService;

    @BeforeEach
    void setUp() {
        // 使用 Spy 来模拟 ServiceImpl
        followService = spy(new FollowServiceImpl());
        ReflectionTestUtils.setField(followService, "baseMapper", followMapper);
    }

    @Test
    void followUser_Success() {
        Long followerId = 1L;
        Long followingId = 2L;

        // 模拟未关注
        doReturn(null).when(followService).getOne(any());
        doReturn(true).when(followService).save(any(Follow.class));

        followService.followUser(followerId, followingId);

        verify(followService).save(any(Follow.class));
    }

    @Test
    void followUser_Self_Fail() {
        assertThrows(BusinessException.class, () -> 
            followService.followUser(1L, 1L));
    }

    @Test
    void followUser_Reactivate() {
        // 模拟已取消关注
        Follow existing = new Follow();
        existing.setStatus(0);
        
        doReturn(existing).when(followService).getOne(any());
        doReturn(true).when(followService).updateById(any(Follow.class));

        followService.followUser(1L, 2L);

        verify(followService).updateById(existing);
        assertEquals(1, existing.getStatus());
    }

    @Test
    void unfollowUser_Success() {
        // 模拟已关注
        Follow existing = new Follow();
        existing.setStatus(1);
        
        doReturn(existing).when(followService).getOne(any());
        doReturn(true).when(followService).updateById(any(Follow.class));

        followService.unfollowUser(1L, 2L);

        verify(followService).updateById(existing);
        assertEquals(0, existing.getStatus());
    }
}
