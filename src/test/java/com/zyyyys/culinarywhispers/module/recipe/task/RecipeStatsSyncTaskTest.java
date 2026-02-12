package com.zyyyys.culinarywhispers.module.recipe.task;

import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecipeStatsSyncTaskTest {

    private StringRedisTemplate redisTemplate;
    private RecipeStatsMapper statsMapper;
    private SetOperations<String, String> setOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private RecipeStatsSyncTask task;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        statsMapper = mock(RecipeStatsMapper.class);
        setOperations = mock(SetOperations.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        task = new RecipeStatsSyncTask(redisTemplate, statsMapper);
    }

    @Test
    void syncStatsToDb_updatesAndRemovesDirtyId() {
        when(setOperations.members(RedisKeyConstant.RECIPE_STATS_DIRTY_SET)).thenReturn(Set.of("100"));
        when(hashOperations.entries(RedisKeyConstant.RECIPE_STATS_PREFIX + 100L)).thenReturn(Map.of(
                "view_count", "2",
                "like_count", "3",
                "collect_count", "4",
                "comment_count", "5",
                "share_count", "6"
        ));
        when(statsMapper.updateById(any())).thenReturn(1);

        task.syncStatsToDb();

        verify(statsMapper).updateById(argThat(s -> s.getRecipeId().equals(100L) && s.getViewCount().equals(2L)));
        verify(setOperations).remove(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, "100");
    }
}

