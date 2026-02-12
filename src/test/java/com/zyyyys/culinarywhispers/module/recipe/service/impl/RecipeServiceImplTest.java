package com.zyyyys.culinarywhispers.module.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipeQueryDTO;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTag;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeUpdatedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.*;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipeDetailVO;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeInfoMapper infoMapper;
    @Mock
    private RecipeStepMapper stepMapper;
    @Mock
    private RecipeStatsMapper statsMapper;
    @Mock
    private UserService userService;
    @Mock
    private CommentService commentService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    @BeforeEach
    void setUp() throws Exception {
        // 为 MyBatis-Plus ServiceImpl 注入 baseMapper
        ReflectionTestUtils.setField(recipeService, "baseMapper", infoMapper);
        
        // Mock Redis ops
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().doReturn("[]").when(objectMapper).writeValueAsString(any());
    }

    @Test
    void publish_Success() {
        // 准备数据
        RecipePublishDTO dto = new RecipePublishDTO();
        dto.setTitle("Test Recipe");
        dto.setSteps(Collections.singletonList(new RecipePublishDTO.StepDTO()));
        dto.setTags(Collections.singletonList("Tag1"));
        dto.setCategoryId(1);
        dto.setDifficulty(1);
        dto.setTimeCost(30);

        // 模拟 ServiceImpl.save 调用的 baseMapper.insert
        when(infoMapper.insert(any(RecipeInfo.class))).thenAnswer(invocation -> {
            RecipeInfo info = invocation.getArgument(0);
            info.setId(100L);
            return 1;
        });

        // 执行操作
        Long recipeId = recipeService.publish(1L, dto);

        // 断言结果
        assertEquals(100L, recipeId);
        
        // 验证交互
        verify(infoMapper, times(1)).insert(any(RecipeInfo.class));
        verify(stepMapper, times(1)).insert(any());
        
        // 验证事件发布
        ArgumentCaptor<RecipePublishedEvent> eventCaptor = ArgumentCaptor.forClass(RecipePublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RecipePublishedEvent event = eventCaptor.getValue();
        assertEquals(100L, event.getRecipeId());
        assertEquals(1L, event.getAuthorId());
        assertEquals(1, event.getTags().size());
    }

    @Test
    void publish_WithExistingTag() {
        // 该测试逻辑现已移至 RecipeTagListener，
        // 此处仅验证事件包含正确的标签
        
        // 准备数据
        RecipePublishDTO dto = new RecipePublishDTO();
        dto.setTitle("Existing Tag Recipe");
        dto.setTags(Collections.singletonList("ExistingTag"));
        
        when(infoMapper.insert(any(RecipeInfo.class))).thenAnswer(invocation -> {
            RecipeInfo info = invocation.getArgument(0);
            info.setId(102L);
            return 1;
        });

        // 执行操作
        recipeService.publish(1L, dto);

        // 验证事件
        ArgumentCaptor<RecipePublishedEvent> eventCaptor = ArgumentCaptor.forClass(RecipePublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("ExistingTag", eventCaptor.getValue().getTags().get(0));
    }

    @Test
    void deleteRecipe_Success() {
        // 准备数据
        Long userId = 1L;
        Long recipeId = 100L;
        
        RecipeInfo info = new RecipeInfo();
        info.setId(recipeId);
        info.setAuthorId(userId);
        
        // 模拟 baseMapper 方法而非 service 方法
        when(infoMapper.selectById(recipeId)).thenReturn(info);
        when(infoMapper.deleteById(recipeId)).thenReturn(1);

        // 执行操作
        recipeService.deleteRecipe(userId, recipeId);

        // 验证交互
        verify(stepMapper, times(1)).delete(any());
        verify(eventPublisher).publishEvent(any(RecipeDeletedEvent.class));
    }

    @Test
    void publish_WithNoSteps() {
        // 准备数据
        RecipePublishDTO dto = new RecipePublishDTO();
        dto.setTitle("No Steps Recipe");
        
        when(infoMapper.insert(any(RecipeInfo.class))).thenAnswer(invocation -> {
            RecipeInfo info = invocation.getArgument(0);
            info.setId(103L);
            return 1;
        });

        // 执行操作
        recipeService.publish(1L, dto);

        // 验证交互
        verify(stepMapper, never()).insert(any());
        // 验证事件发布（统计初始化已移至监听器）
        verify(eventPublisher).publishEvent(any(RecipePublishedEvent.class));
    }

    @Test
    void updateRecipe_Success() {
        // 准备数据
        Long userId = 1L;
        Long recipeId = 105L;
        RecipePublishDTO dto = new RecipePublishDTO();
        dto.setTitle("Updated Title");
        dto.setTags(Collections.singletonList("NewTag"));
        
        RecipeInfo existingInfo = new RecipeInfo();
        existingInfo.setId(recipeId);
        existingInfo.setAuthorId(userId);
        
        when(infoMapper.selectById(recipeId)).thenReturn(existingInfo);
        when(infoMapper.updateById(any(RecipeInfo.class))).thenReturn(1);

        // 执行操作
        recipeService.updateRecipe(userId, recipeId, dto);

        // 验证交互
        verify(infoMapper, times(1)).updateById(any(RecipeInfo.class));
        verify(stepMapper, times(1)).delete(any());
        // 验证更新事件
        ArgumentCaptor<RecipeUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(RecipeUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(recipeId, eventCaptor.getValue().getRecipeId());
        assertEquals("NewTag", eventCaptor.getValue().getTags().get(0));
    }

    @Test
    void getDetail_Success() {
        // 准备数据
        Long recipeId = 100L;
        Long authorId = 1L;
        
        RecipeInfo info = new RecipeInfo();
        info.setId(recipeId);
        info.setAuthorId(authorId);
        info.setTitle("Delicious Cake");
        // Nutrition data
        info.setCalories(500);
        info.setProtein(new BigDecimal("30"));
        info.setFat(new BigDecimal("20"));
        info.setCarbs(new BigDecimal("50"));

        User author = new User();
        author.setId(authorId);
        author.setNickname("Chef");
        
        // Mock Redis Miss
        when(hashOperations.entries(anyString())).thenReturn(Collections.emptyMap());
        
        // 模拟 baseMapper 而非 service
        when(infoMapper.selectById(recipeId)).thenReturn(info);
        when(userService.getById(authorId)).thenReturn(author);
        when(statsMapper.selectById(recipeId)).thenReturn(null); // 将初始化空统计
        when(stepMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        // Mock Works
        when(commentService.getRecipeWorks(recipeId, 5)).thenReturn(Collections.singletonList(new Comment()));

        // 执行操作
        RecipeDetailVO vo = recipeService.getDetail(recipeId);

        // 断言结果
        assertNotNull(vo);
        assertEquals("Delicious Cake", vo.getInfo().getTitle());
        assertEquals("Chef", vo.getAuthor().getNickname());
        assertNotNull(vo.getStats());
        
        // Verify Nutrition
        assertNotNull(vo.getNutritionAnalysis());
        assertEquals(25, vo.getNutritionAnalysis().getCaloriesPercent()); // 500/2000
        
        // Verify Works
        assertNotNull(vo.getWorks());
        assertEquals(1, vo.getWorks().size());
        
        // Verify Redis Write-Back (View Count)
        verify(hashOperations).increment(anyString(), eq("view_count"), eq(1L));
    }

    @Test
    void pageList_Success() {
        // 准备数据
        RecipeQueryDTO query = new RecipeQueryDTO();
        query.setPage(1);
        query.setSize(10);
        
        RecipeInfo record = new RecipeInfo();
        record.setId(100L);
        record.setAuthorId(1L);
        
        Page<RecipeInfo> page = new Page<>();
        page.setRecords(Collections.singletonList(record));
        page.setTotal(1);

        // 模拟 baseMapper 而非 service
        when(infoMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<RecipeInfo> p = invocation.getArgument(0);
            p.setRecords(Collections.singletonList(record));
            p.setTotal(1);
            return p;
        });
        when(userService.listByIds(any())).thenReturn(Collections.singletonList(new User()));

        // 执行操作
        Page<RecipePageVO> result = recipeService.pageList(query);

        // 断言结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(100L, result.getRecords().get(0).getId());
    }
}
