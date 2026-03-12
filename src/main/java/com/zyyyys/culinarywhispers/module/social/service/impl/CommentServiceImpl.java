package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.notify.entity.Notification;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.CommentMapper;
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import com.zyyyys.culinarywhispers.module.social.vo.CommentVO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.mapper.UserMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final RecipeInfoMapper recipeInfoMapper;
    private final UserPointsService pointsService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

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
        Comment parent = null;
        if (parentId != null && parentId > 0) {
            parent = this.getById(parentId);
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

        Long newCommentId = comment.getId();

        String shortContent = content.trim();
        if (shortContent.length() > 80) {
            shortContent = shortContent.substring(0, 80) + "...";
        }

        if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(userId)) {
            Notification n = new Notification();
            n.setFromUserId(userId);
            n.setToUserId(parent.getUserId());
            n.setType(1);
            n.setTargetType(2);
            n.setTargetId(newCommentId);
            n.setContent("回复了你：" + shortContent);
            n.setIsRead(0);
            n.setGmtCreate(LocalDateTime.now());
            notificationService.save(n);
            pointsService.addPoints(userId, 1, 4, "回复评论");
        }

        RecipeInfo info = recipeInfoMapper.selectById(recipeId);
        if (info != null && info.getAuthorId() != null && !info.getAuthorId().equals(userId) && (parentId == null || parentId <= 0)) {
            Notification n = new Notification();
            n.setFromUserId(userId);
            n.setToUserId(info.getAuthorId());
            n.setType(2);
            n.setTargetType(2);
            n.setTargetId(newCommentId);
            n.setContent("评论了你的食谱：" + shortContent);
            n.setIsRead(0);
            n.setGmtCreate(LocalDateTime.now());
            notificationService.save(n);
            pointsService.addPoints(userId, 2, 4, "发表评论");
            pointsService.addPoints(info.getAuthorId(), 3, 7, "食谱被评论");
        }

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

        if (!comment.getUserId().equals(userId) && !UserContext.isAdmin()) {
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
    public Page<CommentVO> listComments(Long recipeId, int page, int size) {
        Page<Comment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRecipeId, recipeId)
               .orderByDesc(Comment::getGmtCreate); // 默认按时间倒序
        
        Page<Comment> entityPage = this.page(pageParam, wrapper);
        List<Comment> records = entityPage.getRecords() == null ? Collections.emptyList() : entityPage.getRecords();

        Set<Long> userIds = records.stream()
            .map(Comment::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty()
            ? Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream().filter(Objects::nonNull).collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Map<Long, UserProfile> profileMap = userIds.isEmpty()
            ? Collections.emptyMap()
            : userProfileMapper.selectBatchIds(userIds).stream().filter(Objects::nonNull).collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (a, b) -> a));

        List<CommentVO> voRecords = records.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setRecipeId(c.getRecipeId());
            vo.setContent(c.getContent());
            vo.setParentId(c.getParentId());
            vo.setLikeCount(c.getLikeCount());
            vo.setGmtCreate(c.getGmtCreate());

            Long uid = c.getUserId();
            User u = uid == null ? null : userMap.get(uid);
            UserProfile p = uid == null ? null : profileMap.get(uid);

            CommentVO.AuthorVO author = new CommentVO.AuthorVO();
            author.setId(uid);
            author.setUsername(u != null && StringUtils.hasText(u.getUsername()) ? u.getUsername() : "user_" + uid);
            author.setNickname(u != null && StringUtils.hasText(u.getNickname()) ? u.getNickname() : ("User " + uid));
            author.setAvatarUrl(u != null ? u.getAvatarUrl() : null);
            author.setIsMasterChef(p != null && p.getIsMasterChef() != null ? p.getIsMasterChef() : Boolean.FALSE);
            author.setMasterTitle(p != null ? p.getMasterTitle() : null);
            author.setBgImageUrl(p != null ? p.getBgImageUrl() : null);

            vo.setAuthor(author);
            return vo;
        }).collect(Collectors.toList());

        Page<CommentVO> voPage = new Page<>();
        voPage.setCurrent(entityPage.getCurrent());
        voPage.setSize(entityPage.getSize());
        voPage.setTotal(entityPage.getTotal());
        voPage.setPages(entityPage.getPages());
        voPage.setRecords(voRecords);
        return voPage;
    }

    @Override
    public CommentVO getComment(Long commentId) {
        Comment c = this.getById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        Long uid = c.getUserId();
        User u = uid == null ? null : userMapper.selectById(uid);
        UserProfile p = uid == null ? null : userProfileMapper.selectById(uid);

        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setRecipeId(c.getRecipeId());
        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setLikeCount(c.getLikeCount());
        vo.setGmtCreate(c.getGmtCreate());

        CommentVO.AuthorVO author = new CommentVO.AuthorVO();
        author.setId(uid);
        author.setUsername(u != null && StringUtils.hasText(u.getUsername()) ? u.getUsername() : "user_" + uid);
        author.setNickname(u != null && StringUtils.hasText(u.getNickname()) ? u.getNickname() : ("User " + uid));
        author.setAvatarUrl(u != null ? u.getAvatarUrl() : null);
        author.setIsMasterChef(p != null && p.getIsMasterChef() != null ? p.getIsMasterChef() : Boolean.FALSE);
        author.setMasterTitle(p != null ? p.getMasterTitle() : null);
        author.setBgImageUrl(p != null ? p.getBgImageUrl() : null);
        vo.setAuthor(author);
        return vo;
    }

    /**
     * 获取食谱的"跟做"作业 (带图片的评论)
     * @param recipeId 食谱ID
     * @param limit 限制数量
     * @return 评论列表
     */
    @Override
    public java.util.List<Comment> getRecipeWorks(Long recipeId, int limit) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRecipeId, recipeId)
               .isNotNull(Comment::getImgUrls)
               .ne(Comment::getImgUrls, "")
               .orderByDesc(Comment::getLikeCount) // 按点赞数排序，展示热门作业
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }
}
