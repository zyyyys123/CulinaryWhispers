package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Interaction;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.InteractionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

    @Mock
    private InteractionMapper interactionMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InteractionServiceImpl interactionService;

    @BeforeEach
    void setUp() {
        // 使用 Spy 来部分模拟 ServiceImpl 的行为 (绕过 MyBatis-Plus 的复杂 Wrapper 匹配)
        interactionService = spy(new InteractionServiceImpl(eventPublisher));
        // 注入 baseMapper 以支持 save/remove 等操作
        ReflectionTestUtils.setField(interactionService, "baseMapper", interactionMapper);
    }

    @Test
    void toggleInteraction_Add_Success() {
        // 准备数据
        Long userId = 1L;
        Integer targetType = 1;
        Long targetId = 100L;
        Integer actionType = 1;

        // 模拟 getOne 返回 null
        doReturn(null).when(interactionService).getOne(any());
        // 模拟 save 成功
        doReturn(true).when(interactionService).save(any(Interaction.class));

        // 执行操作
        interactionService.toggleInteraction(userId, targetType, targetId, actionType);

        // 验证交互
        verify(interactionService).save(any(Interaction.class));
        verify(interactionService, never()).removeById(anyLong());

        // 验证事件
        ArgumentCaptor<InteractionEvent> eventCaptor = ArgumentCaptor.forClass(InteractionEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isAdd());
    }

    @Test
    void toggleInteraction_Remove_Success() {
        // 准备数据
        Long userId = 1L;
        Integer targetType = 1;
        Long targetId = 100L;
        Integer actionType = 1;

        Interaction existing = new Interaction();
        existing.setId(555L);
        existing.setUserId(userId);

        // 模拟 getOne 返回现有记录
        doReturn(existing).when(interactionService).getOne(any());
        // 模拟 removeById 成功
        doReturn(true).when(interactionService).removeById(555L);

        // 执行操作
        interactionService.toggleInteraction(userId, targetType, targetId, actionType);

        // 验证交互
        verify(interactionService).removeById(555L);
        verify(interactionService, never()).save(any(Interaction.class));

        // 验证事件
        ArgumentCaptor<InteractionEvent> eventCaptor = ArgumentCaptor.forClass(InteractionEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().isAdd());
    }

    @Test
    void toggleInteraction_ParamError() {
        // 测试参数缺失的边界情况
        assertThrows(BusinessException.class, () -> 
            interactionService.toggleInteraction(null, 1, 100L, 1));
        
        assertThrows(BusinessException.class, () -> 
            interactionService.toggleInteraction(1L, null, 100L, 1));
    }

    @Test
    void toggleInteraction_Concurrency_Insert() {
        // 准备数据
        Long userId = 1L;
        Integer targetType = 1;
        Long targetId = 100L;
        Integer actionType = 1;

        doReturn(null).when(interactionService).getOne(any());
        doReturn(true).when(interactionService).save(any(Interaction.class));
        
        // 执行操作
        interactionService.toggleInteraction(userId, targetType, targetId, actionType);

        // 验证
        verify(interactionService).save(any(Interaction.class));
    }
}
