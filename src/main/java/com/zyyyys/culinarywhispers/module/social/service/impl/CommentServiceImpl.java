package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.CommentMapper;
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 评论服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 添加评论
     * @param userId 用户ID
     * @param recipeId 食谱ID
     * @param content 评论内容
     * @param parentId 父评论ID（null表示根评论）
     * @return 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long userId, Long recipeId, String content, Long parentId) {
        // 1. 基础校验
        if (userId == null || recipeId == null || !StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        if (content.length() > 500) {
            throw new BusinessException(400, "评论内容过长");
        }

        // 2. 构造评论对象
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setRecipeId(recipeId);
        comment.setContent(content);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setLikeCount(0);
        comment.setGmtCreate(LocalDateTime.now());
        
        // 处理 rootId
        if (parentId != null && parentId > 0) {
            Comment parent = this.getById(parentId);
            if (parent == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND);
            }
            // 如果父评论也是回复，则继承其 rootId；如果父评论是根评论，则 rootId 为父评论 ID
            comment.setRootId(parent.getRootId() != null && parent.getRootId() > 0 ? parent.getRootId() : parentId);
        } else {
            comment.setRootId(0L);
        }

        // 3. 保存入库
        this.save(comment);

        // 4. 发布事件 (增加评论数)
        eventPublisher.publishEvent(new CommentEvent(this, recipeId, true));
        
        return comment.getId();
    }

    /**
     * 删除评论
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 权限校验：只能删除自己的评论 (未来可扩展：食谱作者或管理员也能删除)
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 逻辑删除或物理删除
        this.removeById(commentId);

        // 发布事件 (减少评论数)
        eventPublisher.publishEvent(new CommentEvent(this, comment.getRecipeId(), false));
    }

    /**
     * 分页查询评论
     * @param recipeId 食谱ID
     * @param page 页码
     * @param size 每页数量
     * @return 评论分页列表
     */
    @Override
    public Page<Comment> listComments(Long recipeId, int page, int size) {
        Page<Comment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRecipeId, recipeId)
               .orderByDesc(Comment::getGmtCreate); // 默认按时间倒序
        
        // TODO: 可以在这里处理树形结构的组装，目前先返回扁平列表
        return this.page(pageParam, wrapper);
    }
}
