package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecipeStatsListenerTest {

    private RecipeStatsMapper statsMapper;
    private StringRedisTemplate redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private SetOperations<String, String> setOperations;

    private RecipeStatsListener listener;

    @BeforeEach
    void setUp() {
        statsMapper = mock(RecipeStatsMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        listener = new RecipeStatsListener(statsMapper, redisTemplate);
    }

    @Test
    void handleRecipePublished_initsDbAndRedis() {
        RecipePublishedEvent event = new RecipePublishedEvent(this, 100L, 1L, null);
        listener.handleRecipePublished(event);

        ArgumentCaptor<RecipeStats> captor = ArgumentCaptor.forClass(RecipeStats.class);
        verify(statsMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRecipeId());

        verify(hashOperations).putAll(eq(RedisKeyConstant.RECIPE_STATS_PREFIX + 100L), anyMap());
    }

    @Test
    void handleRecipeDeleted_deletesDbAndRedis() {
        RecipeDeletedEvent event = new RecipeDeletedEvent(this, 100L);
        listener.handleRecipeDeleted(event);

        verify(statsMapper).deleteById(100L);
        verify(redisTemplate).delete(RedisKeyConstant.RECIPE_STATS_PREFIX + 100L);
    }

    @Test
    void handleInteraction_like_updatesRedisAndDirtySet() {
        InteractionEvent event = new InteractionEvent(this, 1L, 1, 100L, 1, true);
        listener.handleInteraction(event);

        verify(hashOperations).increment(RedisKeyConstant.RECIPE_STATS_PREFIX + 100L, "like_count", 1L);
        verify(setOperations).add(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, "100");
    }

    @Test
    void handleComment_updatesRedisAndDirtySet() {
        CommentEvent event = new CommentEvent(this, 100L, true);
        listener.handleComment(event);

        verify(hashOperations).increment(RedisKeyConstant.RECIPE_STATS_PREFIX + 100L, "comment_count", 1L);
        verify(setOperations).add(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, "100");
    }
}
