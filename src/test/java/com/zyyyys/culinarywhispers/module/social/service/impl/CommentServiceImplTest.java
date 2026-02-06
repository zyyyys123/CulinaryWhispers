package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.CommentMapper;
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
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        // 使用 Spy 来模拟 ServiceImpl
        commentService = spy(new CommentServiceImpl(eventPublisher));
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
    }

    @Test
    void addComment_Success() {
        // ... (保持不变)
        // 略，因为 save 是 ServiceImpl 方法，需要 mock 或 verify
        // 这里为了简化，我们模拟 save 方法
        
        Long userId = 1L;
        Long recipeId = 100L;
        String content = "Delicious!";
        
        doReturn(true).when(commentService).save(any(Comment.class));

        Long commentId = commentService.addComment(userId, recipeId, content, null);
        
        // 验证 save 被调用
        verify(commentService).save(any(Comment.class));
        
        // 验证事件
        ArgumentCaptor<CommentEvent> eventCaptor = ArgumentCaptor.forClass(CommentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isAdd());
    }

    @Test
    void addComment_Reply_Success() {
        Long userId = 1L;
        Long recipeId = 100L;
        String content = "Agree!";
        Long parentId = 50L;

        Comment parent = new Comment();
        parent.setId(parentId);
        parent.setRootId(0L);
        
        // 模拟 getById
        doReturn(parent).when(commentService).getById(parentId);
        doReturn(true).when(commentService).save(any(Comment.class));

        commentService.addComment(userId, recipeId, content, parentId);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).save(commentCaptor.capture());
        assertEquals(parentId, commentCaptor.getValue().getRootId());
    }

    @Test
    void deleteComment_Success() {
        Long userId = 1L;
        Long commentId = 10L;
        Long recipeId = 100L;

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(userId);
        comment.setRecipeId(recipeId);

        // 模拟 getById 和 removeById
        doReturn(comment).when(commentService).getById(commentId);
        doReturn(true).when(commentService).removeById(commentId);

        commentService.deleteComment(userId, commentId);

        verify(commentService).removeById(commentId);
        
        ArgumentCaptor<CommentEvent> eventCaptor = ArgumentCaptor.forClass(CommentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().isAdd());
    }

    @Test
    void deleteComment_Forbidden() {
        Long userId = 1L;
        Long commentId = 10L;

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(2L);

        doReturn(comment).when(commentService).getById(commentId);

        assertThrows(BusinessException.class, () -> 
            commentService.deleteComment(userId, commentId));
    }
}
