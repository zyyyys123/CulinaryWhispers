package com.zyyyys.culinarywhispers.module.user.service.impl;

import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.module.user.entity.PointsRecord;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.mapper.PointsRecordMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPointsServiceImplTest {

    @Mock
    private UserStatsMapper statsMapper;
    @Mock
    private PointsRecordMapper pointsRecordMapper;

    private UserPointsServiceImpl pointsService;

    @BeforeEach
    void setUp() {
        pointsService = spy(new UserPointsServiceImpl(statsMapper));
        ReflectionTestUtils.setField(pointsService, "baseMapper", pointsRecordMapper);
    }

    @Test
    void signIn_FirstTime_Success() {
        Long userId = 1L;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setPoints(0);
        stats.setContinueSignDays(0);
        stats.setLastSignDate(LocalDate.now().minusDays(2)); // Not yesterday

        when(statsMapper.selectById(userId)).thenReturn(stats);
        when(pointsRecordMapper.insert(any(PointsRecord.class))).thenReturn(1);

        Integer points = pointsService.signIn(userId);

        // 10 base points
        assertEquals(10, points);
        
        // Verify stats update
        ArgumentCaptor<UserStats> statsCaptor = ArgumentCaptor.forClass(UserStats.class);
        verify(statsMapper).updateById(statsCaptor.capture());
        assertEquals(1, statsCaptor.getValue().getContinueSignDays());
        assertEquals(10, statsCaptor.getValue().getPoints());
        assertEquals(LocalDate.now(), statsCaptor.getValue().getLastSignDate());
    }

    @Test
    void signIn_Continuous_Success() {
        Long userId = 1L;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setPoints(100);
        stats.setContinueSignDays(2);
        stats.setLastSignDate(LocalDate.now().minusDays(1)); // Yesterday

        when(statsMapper.selectById(userId)).thenReturn(stats);

        Integer points = pointsService.signIn(userId);

        // Base 10 + Bonus (2 days * 5 = 10) = 20
        // Wait, logic is: (continueDays - 1) * 5
        // New continueDays will be 3.
        // Bonus = (3 - 1) * 5 = 10.
        // Total = 10 + 10 = 20.
        assertEquals(20, points);

        verify(statsMapper).updateById(stats);
        assertEquals(3, stats.getContinueSignDays());
        assertEquals(120, stats.getPoints());
    }

    @Test
    void signIn_AlreadySigned_Fail() {
        Long userId = 1L;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLastSignDate(LocalDate.now());

        when(statsMapper.selectById(userId)).thenReturn(stats);

        assertThrows(BusinessException.class, () -> pointsService.signIn(userId));
    }
    
    @Test
    void addPoints_Success() {
        Long userId = 1L;
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setPoints(100);
        
        when(statsMapper.selectById(userId)).thenReturn(stats);
        
        pointsService.addPoints(userId, 50, 2, "Test");
        
        assertEquals(150, stats.getPoints());
        verify(statsMapper).updateById(stats);
        verify(pointsRecordMapper).insert(any(PointsRecord.class));
    }
}
