package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.CommentMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipeInfoMapper recipeInfoMapper;

    @Mock
    private UserPointsService pointsService;

    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        // 使用 Spy 来模拟 ServiceImpl
        commentService = spy(new CommentServiceImpl(eventPublisher, notificationService, recipeInfoMapper, pointsService));
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
    }

    @Test
    void getRecipeWorks_Success() {
        // 准备数据
        Long recipeId = 1L;
        int limit = 5;
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setImgUrls("[\"http://img.com/1.jpg\"]");
        
        // 模拟 list 方法 (MyBatis-Plus IService.list(Wrapper))
        // 注意: list(Wrapper) 是 ServiceImpl 的方法，通常会调用 baseMapper.selectList
        // 这里我们需要 mock ServiceImpl.list 或者 baseMapper.selectList
        
        // 由于 CommentServiceImpl 继承 ServiceImpl，内部调用 this.list(wrapper) 最终调用 baseMapper.selectList
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(comment));

        // 执行
        List<Comment> works = commentService.getRecipeWorks(recipeId, limit);

        // 验证
        assertNotNull(works);
        assertEquals(1, works.size());
        verify(commentMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void addComment_Success() {
        Long userId = 1L;
        Long recipeId = 100L;
        String content = "Delicious!";

        RecipeInfo info = new RecipeInfo();
        info.setId(recipeId);
        info.setAuthorId(2L);
        when(recipeInfoMapper.selectById(recipeId)).thenReturn(info);
        
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
        parent.setUserId(2L);
        
        // 模拟 getById
        doReturn(parent).when(commentService).getById(parentId);
        when(recipeInfoMapper.selectById(recipeId)).thenReturn(new RecipeInfo());
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
