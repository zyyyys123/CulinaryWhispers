package com.zyyyys.culinarywhispers.module.user.service;

import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.entity.UserStats;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserStatsMapper;
import com.zyyyys.culinarywhispers.module.user.service.impl.UserStatsServiceImpl;
import com.zyyyys.culinarywhispers.module.user.vo.UserStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserStatsServiceTest {

    @Mock
    private UserStatsMapper userStatsMapper;
    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserStatsServiceImpl userStatsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userStatsService, "baseMapper", userStatsMapper);
        lenient().when(userStatsMapper.insert(any(UserStats.class))).thenReturn(1);
        lenient().when(userStatsMapper.updateById(any(UserStats.class))).thenReturn(1);
    }

    @Test
    public void testGetUserStats_InitWhenNull() {
        UserStats afterInit = new UserStats();
        afterInit.setUserId(1L);
        afterInit.setLevel(1);
        afterInit.setExperience(0L);

        when(userStatsMapper.selectById(1L)).thenReturn(null).thenReturn(afterInit);
        when(userProfileMapper.selectById(1L)).thenReturn(null);

        UserStatsVO vo = userStatsService.getUserStats(1L);
        assertNotNull(vo);
        assertEquals(1, vo.getLevel());
        verify(userStatsMapper).insert(any(UserStats.class));
    }

    @Test
    public void testGetUserStats_Existing() {
        UserStats stats = new UserStats();
        stats.setUserId(1L);
        stats.setLevel(2);
        stats.setExperience(150L);
        stats.setTotalLikesReceived(1200L); // Should trigger badge

        UserProfile profile = new UserProfile();
        profile.setUserId(1L);
        profile.setCookAge(5); // Should trigger badge
        profile.setIsMasterChef(true);

        when(userStatsMapper.selectById(1L)).thenReturn(stats);
        when(userProfileMapper.selectById(1L)).thenReturn(profile);

        UserStatsVO vo = userStatsService.getUserStats(1L);

        assertNotNull(vo);
        assertEquals(2, vo.getLevel());
        assertEquals(150L, vo.getExperience());
        assertTrue(vo.getIsMasterChef());
        
        List<String> badges = vo.getBadges();
        assertTrue(badges.contains("人气之星"));
        assertTrue(badges.contains("资深大厨"));
    }

    @Test
    public void testAddExperience_LevelUp() {
        UserStats stats = new UserStats();
        stats.setUserId(1L);
        stats.setLevel(1);
        stats.setExperience(0L);

        when(userStatsMapper.selectById(1L)).thenReturn(stats);
        
        // Add 150 exp. Level 1 -> 2 needs 100 exp.
        // 150 > 100, so should be Level 2.
        
        userStatsService.addExperience(1L, 150);

        assertEquals(150L, stats.getExperience());
        assertEquals(2, stats.getLevel());
        
        verify(userStatsMapper, times(1)).updateById(stats);
    }
    
    @Test
    public void testCalculateLevelLogic() throws Exception {
        // Reflection to test private method logic if needed, or just test via addExperience
        // Level 1: 0
        // Level 2: 100
        // Level 3: 300
        // Level 4: 600
        
        UserStats stats = new UserStats();
        stats.setUserId(1L);
        stats.setLevel(1);
        stats.setExperience(0L);
        
        when(userStatsMapper.selectById(1L)).thenReturn(stats);
        
        // Add 300 exp -> Should be Level 3
        userStatsService.addExperience(1L, 300);
        assertEquals(3, stats.getLevel());
    }
}
