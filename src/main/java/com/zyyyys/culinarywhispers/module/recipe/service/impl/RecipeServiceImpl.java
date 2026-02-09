package com.zyyyys.culinarywhispers.module.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipeQueryDTO;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStep;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeUpdatedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStepMapper;
import com.zyyyys.culinarywhispers.module.recipe.service.RecipeService;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipeDetailVO;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 食谱服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl extends ServiceImpl<RecipeInfoMapper, RecipeInfo> implements RecipeService {

    private final RecipeStepMapper stepMapper;
    private final RecipeStatsMapper statsMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 发布食谱
     * @param userId 用户ID
     * @param publishDTO 发布信息
     * @return 食谱ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, RecipePublishDTO publishDTO) {
        // 1. 转换 DTO 为实体
        RecipeInfo recipeInfo = new RecipeInfo();
        BeanUtils.copyProperties(publishDTO, recipeInfo);
        recipeInfo.setAuthorId(userId);
        // 默认状态: 0-草稿 (或根据业务逻辑调整，如直接发布为 1-审核中)
        recipeInfo.setStatus(0); 

        // 处理标签 JSON 序列化
        if (publishDTO.getTags() != null) {
            try {
                recipeInfo.setTags(objectMapper.writeValueAsString(publishDTO.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Tags serialization failed", e);
                // 仅记录日志，不阻断流程
            }
        }

        // 2. 保存食谱主表
        this.save(recipeInfo);
        Long recipeId = recipeInfo.getId();
        
        // 3. 保存步骤
        if (publishDTO.getSteps() != null && !publishDTO.getSteps().isEmpty()) {
            List<RecipeStep> steps = publishDTO.getSteps().stream().map(stepDTO -> {
                RecipeStep step = new RecipeStep();
                BeanUtils.copyProperties(stepDTO, step);
                step.setRecipeId(recipeId);
                return step;
            }).collect(Collectors.toList());
            
            // 批量插入步骤
            steps.forEach(stepMapper::insert);
        }

        // 4. 发布事件 (处理统计初始化、标签关联等副作用)
        // 使用观察者模式解耦核心流程与辅助逻辑
        eventPublisher.publishEvent(new RecipePublishedEvent(this, recipeId, userId, publishDTO.getTags()));

        log.info("Recipe published successfully. ID: {}, Author: {}", recipeId, userId);
        return recipeId;
    }

    /**
     * 获取食谱详情
     * @param id 食谱ID
     * @return 食谱详情VO
     */
    @Override
    public RecipeDetailVO getDetail(Long id) {
        // 1. 获取食谱基本信息
        RecipeInfo recipeInfo = this.getById(id);
        if (recipeInfo == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "食谱不存在");
        }

        // 2. 获取统计信息
        RecipeStats stats = statsMapper.selectById(id);
        if (stats == null) {
            stats = new RecipeStats();
            stats.setRecipeId(id);
            stats.setViewCount(0L);
            stats.setLikeCount(0L);
            stats.setCollectCount(0L);
            stats.setCommentCount(0L);
            stats.setShareCount(0L);
            stats.setTryCount(0);
            stats.setScore(BigDecimal.ZERO);
        }

        // 3. 获取步骤列表
        LambdaQueryWrapper<RecipeStep> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(RecipeStep::getRecipeId, id)
                   .orderByAsc(RecipeStep::getStepNo);
        List<RecipeStep> steps = stepMapper.selectList(stepWrapper);

        // 4. 获取作者信息
        User author = userService.getById(recipeInfo.getAuthorId());
        RecipeDetailVO.AuthorVO authorVO = new RecipeDetailVO.AuthorVO();
        if (author != null) {
            authorVO.setId(author.getId());
            authorVO.setNickname(author.getNickname());
            authorVO.setAvatarUrl(author.getAvatarUrl());
        }

        // 5. 组装 VO
        RecipeDetailVO vo = new RecipeDetailVO();
        vo.setInfo(recipeInfo);
        vo.setStats(stats);
        vo.setSteps(steps);
        vo.setAuthor(authorVO);

        return vo;
    }

    /**
     * 获取食谱分页列表
     * @param queryDTO 查询参数
     * @return 食谱分页VO列表
     */
    @Override
    public Page<RecipePageVO> pageList(RecipeQueryDTO queryDTO) {
        // 1. 构建查询条件
        Page<RecipeInfo> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<>();
        
        // 状态: 已发布 (假设 2 为已发布)
        wrapper.eq(RecipeInfo::getStatus, 2);

        // 关键词搜索
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(RecipeInfo::getTitle, queryDTO.getKeyword())
                            .or()
                            .like(RecipeInfo::getDescription, queryDTO.getKeyword()));
        }

        // 分类筛选
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(RecipeInfo::getCategoryId, queryDTO.getCategoryId());
        }
        
        // 作者筛选
        if (queryDTO.getAuthorId() != null) {
            wrapper.eq(RecipeInfo::getAuthorId, queryDTO.getAuthorId());
        }

        // 排序
        if (StringUtils.hasText(queryDTO.getSortBy())) {
            switch (queryDTO.getSortBy()) {
                case "score":
                    wrapper.orderByDesc(RecipeInfo::getScore);
                    break;
                case "view_count":
                    wrapper.orderByDesc(RecipeInfo::getViewCount);
                    break;
                case "like_count":
                    wrapper.orderByDesc(RecipeInfo::getLikeCount);
                    break;
                default:
                    wrapper.orderByDesc(RecipeInfo::getGmtCreate);
            }
        } else {
            wrapper.orderByDesc(RecipeInfo::getGmtCreate);
        }

        // 2. 执行分页查询
        this.page(page, wrapper);

        // 3. 转换为 VO
        List<RecipeInfo> records = page.getRecords();
        if (records.isEmpty()) {
            return new Page<>(queryDTO.getPage(), queryDTO.getSize());
        }

        // 批量获取作者信息
        Set<Long> authorIds = records.stream().map(RecipeInfo::getAuthorId).collect(Collectors.toSet());
        Map<Long, User> authorMap = userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<RecipePageVO> voList = records.stream().map(info -> {
            RecipePageVO vo = new RecipePageVO();
            BeanUtils.copyProperties(info, vo);
            
            // 填充作者信息
            User author = authorMap.get(info.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<RecipePageVO> resultPage = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        resultPage.setTotal(page.getTotal());
        resultPage.setRecords(voList);

        return resultPage;
    }

    /**
     * 更新食谱
     * @param userId 用户ID
     * @param recipeId 食谱ID
     * @param publishDTO 更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecipe(Long userId, Long recipeId, RecipePublishDTO publishDTO) {
        // 1. 检查是否存在
        RecipeInfo recipeInfo = this.getById(recipeId);
        if (recipeInfo == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "食谱不存在");
        }

        // 2. 检查权限
        if (!recipeInfo.getAuthorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权修改该食谱");
        }

        // 3. 更新主表
        BeanUtils.copyProperties(publishDTO, recipeInfo, "id", "authorId", "status", "gmtCreate");
        
        // 处理标签 JSON 序列化
        if (publishDTO.getTags() != null) {
            try {
                recipeInfo.setTags(objectMapper.writeValueAsString(publishDTO.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Tags serialization failed", e);
            }
        }
        
        this.updateById(recipeInfo);

        // 4. 更新步骤 (全量替换)
        // 4.1 删除旧步骤
        LambdaQueryWrapper<RecipeStep> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(RecipeStep::getRecipeId, recipeId);
        stepMapper.delete(stepWrapper);

        // 4.2 插入新步骤
        if (publishDTO.getSteps() != null && !publishDTO.getSteps().isEmpty()) {
            List<RecipeStep> steps = publishDTO.getSteps().stream().map(stepDTO -> {
                RecipeStep step = new RecipeStep();
                BeanUtils.copyProperties(stepDTO, step);
                step.setRecipeId(recipeId);
                return step;
            }).collect(Collectors.toList());
            steps.forEach(stepMapper::insert);
        }

        // 5. 发布更新事件 (处理标签更新)
        eventPublisher.publishEvent(new RecipeUpdatedEvent(this, recipeId, publishDTO.getTags()));
        
        log.info("Recipe updated successfully. ID: {}, User: {}", recipeId, userId);
    }

    /**
     * 删除食谱
     * @param userId 用户ID
     * @param recipeId 食谱ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecipe(Long userId, Long recipeId) {
        // 1. 检查是否存在
        RecipeInfo recipeInfo = this.getById(recipeId);
        if (recipeInfo == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "食谱不存在");
        }

        // 2. 检查权限
        if (!recipeInfo.getAuthorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除该食谱");
        }

        // 3. 删除主表
        this.baseMapper.deleteById(recipeId);

        // 4. 删除步骤
        LambdaQueryWrapper<RecipeStep> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(RecipeStep::getRecipeId, recipeId);
        stepMapper.delete(stepWrapper);

        // 5. 发布删除事件 (处理统计、标签关系等清理)
        eventPublisher.publishEvent(new RecipeDeletedEvent(this, recipeId));

        log.info("Recipe deleted successfully. ID: {}, User: {}", recipeId, userId);
    }
}
