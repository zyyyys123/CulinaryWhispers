package com.zyyyys.culinarywhispers.module.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.utils.NutritionCalculator;
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
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
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
    private final CommentService commentService;
    private final UserPointsService pointsService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

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
            List<RecipePublishDTO.StepDTO> stepDTOs = publishDTO.getSteps().stream()
                    .filter(s -> s != null && StrUtil.isNotBlank(s.getDesc()))
                    .sorted(Comparator.comparing(s -> Objects.requireNonNullElse(s.getStepNo(), Integer.MAX_VALUE)))
                    .collect(Collectors.toList());

            List<RecipeStep> steps = new ArrayList<>();
            for (int i = 0; i < stepDTOs.size(); i++) {
                RecipePublishDTO.StepDTO stepDTO = stepDTOs.get(i);
                RecipeStep step = new RecipeStep();
                BeanUtils.copyProperties(stepDTO, step);
                step.setRecipeId(recipeId);
                step.setStepNo(i + 1);
                steps.add(step);
            }
            
            // 批量插入步骤
            steps.forEach(stepMapper::insert);
        }

        // 4. 发布事件 (处理统计初始化、标签关联等副作用)
        // 使用观察者模式解耦核心流程与辅助逻辑
        eventPublisher.publishEvent(new RecipePublishedEvent(this, recipeId, userId, publishDTO.getTags()));

        pointsService.addPoints(userId, 20, 2, "发布食谱");

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

        // 2. 获取统计信息 (优先从 Redis 获取)
        RecipeStats stats = getStatsFromRedisOrDb(id);

        // 3. 获取步骤列表
        LambdaQueryWrapper<RecipeStep> stepWrapper = new LambdaQueryWrapper<>();
        stepWrapper.eq(RecipeStep::getRecipeId, id)
                   .orderByAsc(RecipeStep::getStepNo);
        List<RecipeStep> steps = stepMapper.selectList(stepWrapper);
        normalizeStepNo(steps);

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

        // 6. 营养分析 (3.2 营养分析与热量计算)
        RecipeDetailVO.NutritionAnalysisVO nutritionVO = new RecipeDetailVO.NutritionAnalysisVO();
        nutritionVO.setCaloriesPercent(NutritionCalculator.calculateCaloriesPercentage(recipeInfo.getCalories()));
        nutritionVO.setProteinPercent(NutritionCalculator.calculateProteinPercentage(recipeInfo.getProtein()));
        nutritionVO.setFatPercent(NutritionCalculator.calculateFatPercentage(recipeInfo.getFat()));
        nutritionVO.setCarbsPercent(NutritionCalculator.calculateCarbsPercentage(recipeInfo.getCarbs()));
        
        // 简单健康建议
        if (nutritionVO.getProteinPercent() > 30) {
            nutritionVO.setHealthTip("高蛋白，适合增肌");
        } else if (nutritionVO.getFatPercent() < 10) {
            nutritionVO.setHealthTip("低脂清淡");
        } else {
            nutritionVO.setHealthTip("营养均衡");
        }
        vo.setNutritionAnalysis(nutritionVO);

        // 7. "跟做"作业展示 (4.1 社区运营活动)
        try {
            vo.setWorks(commentService.getRecipeWorks(id, 5)); // 默认展示前5个热门作业
        } catch (Exception e) {
            log.error("Failed to load recipe works for: " + id, e);
            // 降级：不展示作业，不影响主流程
        }

        // 增加浏览量 (异步写入 Redis)
        try {
            redisTemplate.opsForHash().increment(RedisKeyConstant.RECIPE_STATS_PREFIX + id, "view_count", 1);
            redisTemplate.opsForSet().add(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, String.valueOf(id));
        } catch (Exception e) {
            log.error("Failed to increment view count for recipe: " + id, e);
        }

        return vo;
    }

    private void normalizeStepNo(List<RecipeStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            RecipeStep s = steps.get(i);
            if (s != null) {
                s.setStepNo(i + 1);
            }
        }
    }

    private RecipeStats getStatsFromRedisOrDb(Long recipeId) {
        String key = RedisKeyConstant.RECIPE_STATS_PREFIX + recipeId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        if (!entries.isEmpty()) {
            // Hit Redis
            RecipeStats stats = new RecipeStats();
            stats.setRecipeId(recipeId);
            stats.setViewCount(parseLong(entries.get("view_count")));
            stats.setLikeCount(parseLong(entries.get("like_count")));
            stats.setCollectCount(parseLong(entries.get("collect_count")));
            stats.setCommentCount(parseLong(entries.get("comment_count")));
            stats.setShareCount(parseLong(entries.get("share_count")));
            // Other fields use default or null
            return stats;
        }

        // Miss Redis, load from DB
        RecipeStats stats = statsMapper.selectById(recipeId);
        if (stats == null) {
            stats = new RecipeStats();
            stats.setRecipeId(recipeId);
            stats.setViewCount(0L);
            stats.setLikeCount(0L);
            stats.setCollectCount(0L);
            stats.setCommentCount(0L);
            stats.setShareCount(0L);
            stats.setTryCount(0);
            stats.setScore(BigDecimal.ZERO);
        }
        
        // Write back to Redis for next time
        try {
            redisTemplate.opsForHash().putAll(key, Map.of(
                    "view_count", String.valueOf(stats.getViewCount()),
                    "like_count", String.valueOf(stats.getLikeCount()),
                    "collect_count", String.valueOf(stats.getCollectCount()),
                    "comment_count", String.valueOf(stats.getCommentCount()),
                    "share_count", String.valueOf(stats.getShareCount())
            ));
        } catch (Exception e) {
            log.error("Failed to cache stats to Redis for recipe: " + recipeId, e);
        }
        
        return stats;
    }
    
    private Long parseLong(Object val) {
        if (val == null) return 0L;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
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

    @Override
    public Page<RecipePageVO> pageListPersonalized(Long userId, RecipeQueryDTO queryDTO) {
        if (userId == null) {
            return pageList(queryDTO);
        }

        UserProfile profile = userService.getUserProfile(userId);
        if (profile == null) {
            return pageList(queryDTO);
        }

        int fetchSize = Math.max(queryDTO.getSize() * 3, queryDTO.getSize());
        Page<RecipeInfo> page = new Page<>(queryDTO.getPage(), fetchSize);
        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(RecipeInfo::getStatus, 2);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(RecipeInfo::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(RecipeInfo::getDescription, queryDTO.getKeyword()));
        }

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(RecipeInfo::getCategoryId, queryDTO.getCategoryId());
        }

        if (queryDTO.getAuthorId() != null) {
            wrapper.eq(RecipeInfo::getAuthorId, queryDTO.getAuthorId());
        }

        wrapper.orderByDesc(RecipeInfo::getGmtCreate);
        this.page(page, wrapper);

        List<RecipeInfo> records = page.getRecords();
        if (records.isEmpty()) {
            return new Page<>(queryDTO.getPage(), queryDTO.getSize());
        }

        String[] restrictions = splitKeywords(profile.getDietaryRestrictions());
        String[] cuisines = splitKeywords(profile.getFavoriteCuisine());
        String[] tastes = splitKeywords(profile.getTastePreference());

        List<ScoredRecipe> candidates = new ArrayList<>();
        for (RecipeInfo info : records) {
            List<String> tags = parseTags(info.getTags());
            if (containsAnyRestriction(info, tags, restrictions)) {
                continue;
            }
            double score = baseScore(info);
            score += matchBoost(info, tags, cuisines, 2.0, 1.5, 1.0);
            score += matchBoost(info, tags, tastes, 1.5, 1.0, 0.5);
            candidates.add(new ScoredRecipe(info, score));
        }

        candidates.sort(Comparator.comparingDouble(ScoredRecipe::score).reversed());
        List<RecipeInfo> picked = candidates.stream()
                .limit(queryDTO.getSize())
                .map(ScoredRecipe::recipe)
                .collect(Collectors.toList());

        Page<RecipePageVO> resultPage = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        resultPage.setTotal(page.getTotal());
        resultPage.setRecords(toPageVoList(picked));
        return resultPage;
    }

    private List<RecipePageVO> toPageVoList(List<RecipeInfo> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> authorIds = records.stream().map(RecipeInfo::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> authorMap = authorIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(authorIds).stream().collect(Collectors.toMap(User::getId, user -> user));

        return records.stream().map(info -> {
            RecipePageVO vo = new RecipePageVO();
            BeanUtils.copyProperties(info, vo);
            User author = authorMap.get(info.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private boolean containsAnyRestriction(RecipeInfo info, List<String> tags, String[] restrictions) {
        if (restrictions == null || restrictions.length == 0) {
            return false;
        }
        String title = info != null ? info.getTitle() : null;
        String description = info != null ? info.getDescription() : null;
        for (String r : restrictions) {
            if (!StringUtils.hasText(r)) {
                continue;
            }
            if (StringUtils.hasText(title) && StrUtil.containsIgnoreCase(title, r)) {
                return true;
            }
            if (StringUtils.hasText(description) && StrUtil.containsIgnoreCase(description, r)) {
                return true;
            }
            if (tags != null && tags.stream().anyMatch(t -> StrUtil.containsIgnoreCase(t, r))) {
                return true;
            }
        }
        return false;
    }

    private double baseScore(RecipeInfo info) {
        if (info == null || info.getScore() == null) {
            return 0.0;
        }
        return info.getScore().doubleValue();
    }

    private double matchBoost(RecipeInfo info, List<String> tags, String[] keywords, double tagBoost, double titleBoost, double descBoost) {
        if (keywords == null || keywords.length == 0 || info == null) {
            return 0.0;
        }
        String title = info.getTitle();
        String description = info.getDescription();
        double boost = 0.0;
        for (String k : keywords) {
            if (!StringUtils.hasText(k)) {
                continue;
            }
            if (tags != null && tags.stream().anyMatch(t -> StrUtil.containsIgnoreCase(t, k))) {
                boost += tagBoost;
            }
            if (titleBoost > 0.0 && StringUtils.hasText(title) && StrUtil.containsIgnoreCase(title, k)) {
                boost += titleBoost;
            }
            if (descBoost > 0.0 && StringUtils.hasText(description) && StrUtil.containsIgnoreCase(description, k)) {
                boost += descBoost;
            }
        }
        return boost;
    }

    private List<String> parseTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String[] splitKeywords(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new String[0];
        }
        return raw.split("[,，\\s]+");
    }

    private record ScoredRecipe(RecipeInfo recipe, double score) {}

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
