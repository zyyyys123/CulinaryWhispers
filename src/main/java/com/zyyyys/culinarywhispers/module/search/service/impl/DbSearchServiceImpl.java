package com.zyyyys.culinarywhispers.module.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.service.SearchService;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "cw.search.type", havingValue = "db", matchIfMissing = true)
public class DbSearchServiceImpl implements SearchService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeTagMapper recipeTagMapper;
    private final UserService userService;
    private final RecipeStatsMapper statsMapper;

    @Override
    public void syncRecipe(Long recipeId) {
        log.info("Skip sync recipe to ES (db search mode). recipeId={}", recipeId);
    }

    @Override
    public void deleteRecipe(Long recipeId) {
        log.info("Skip delete recipe from ES (db search mode). recipeId={}", recipeId);
    }

    @Override
    public Page<RecipeDocument> searchRecipe(String keyword, int page, int size) {
        Page<RecipeInfo> mpPage = new Page<>(page, size);

        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeInfo::getIsDeleted, 0);
        wrapper.eq(RecipeInfo::getStatus, 2);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(RecipeInfo::getTitle, keyword).or().like(RecipeInfo::getDescription, keyword));
        }
        wrapper.orderByDesc(RecipeInfo::getGmtCreate);

        recipeInfoMapper.selectPage(mpPage, wrapper);
        return buildResultPage(mpPage);
    }

    @Override
    public Page<RecipeDocument> searchPersonalized(Long userId, String keyword, int page, int size) {
        Page<RecipeDocument> base = searchRecipe(keyword, page, size);
        if (userId == null) {
            return base;
        }

        UserProfile profile = userService.getUserProfile(userId);
        if (profile == null) {
            return base;
        }

        List<RecipeDocument> filtered = new ArrayList<>(base.getRecords());
        if (StrUtil.isNotBlank(profile.getDietaryRestrictions())) {
            String[] restrictions = profile.getDietaryRestrictions().split("[,， ]+");
            filtered.removeIf(doc -> containsAnyRestriction(doc, restrictions));
        }

        if (StrUtil.isNotBlank(profile.getFavoriteCuisine()) || StrUtil.isNotBlank(profile.getTastePreference())) {
            String[] cuisines = StrUtil.isNotBlank(profile.getFavoriteCuisine())
                    ? profile.getFavoriteCuisine().split("[,， ]+")
                    : new String[0];
            String[] tastes = StrUtil.isNotBlank(profile.getTastePreference())
                    ? profile.getTastePreference().split("[,， ]+")
                    : new String[0];
            filtered.sort(Comparator.comparingDouble(doc -> -scoreBoost(doc, cuisines, tastes)));
        }

        Page<RecipeDocument> result = new Page<>(page, size);
        result.setTotal(filtered.size());
        result.setRecords(filtered);
        return result;
    }

    private Page<RecipeDocument> buildResultPage(Page<RecipeInfo> mpPage) {
        List<RecipeDocument> docs = new ArrayList<>();
        for (RecipeInfo info : mpPage.getRecords()) {
            RecipeDocument doc = new RecipeDocument();
            doc.setId(info.getId());
            doc.setTitle(info.getTitle());
            doc.setDescription(info.getDescription());
            doc.setDifficulty(info.getDifficulty());
            doc.setTimeCost(info.getTimeCost());

            User author = userService.getById(info.getAuthorId());
            if (author != null) {
                doc.setAuthorName(author.getNickname());
            }

            List<String> tags = recipeTagMapper.selectTagNamesByRecipeId(info.getId());
            doc.setTags(tags);

            RecipeStats stats = statsMapper.selectById(info.getId());
            if (stats != null && stats.getScore() != null) {
                doc.setScore(stats.getScore().doubleValue());
            } else if (info.getScore() != null) {
                doc.setScore(info.getScore().doubleValue());
            } else {
                doc.setScore(0.0);
            }

            docs.add(doc);
        }

        Page<RecipeDocument> result = new Page<>(mpPage.getCurrent(), mpPage.getSize());
        result.setTotal(mpPage.getTotal());
        result.setRecords(docs);
        return result;
    }

    private boolean containsAnyRestriction(RecipeDocument doc, String[] restrictions) {
        if (restrictions == null || restrictions.length == 0) {
            return false;
        }
        for (String restriction : restrictions) {
            if (StrUtil.isBlank(restriction)) {
                continue;
            }
            if (doc.getTags() != null && doc.getTags().stream().anyMatch(t -> StrUtil.containsIgnoreCase(t, restriction))) {
                return true;
            }
            if (StrUtil.isNotBlank(doc.getDescription()) && StrUtil.containsIgnoreCase(doc.getDescription(), restriction)) {
                return true;
            }
        }
        return false;
    }

    private double scoreBoost(RecipeDocument doc, String[] cuisines, String[] tastes) {
        double score = doc.getScore() != null ? doc.getScore() : 0.0;
        score += matchBoost(doc, cuisines, 2.0, 1.5);
        score += matchBoost(doc, tastes, 1.5, 0.0);
        return score;
    }

    private double matchBoost(RecipeDocument doc, String[] keywords, double tagBoost, double titleBoost) {
        if (keywords == null || keywords.length == 0) {
            return 0.0;
        }
        double boost = 0.0;
        for (String k : keywords) {
            if (StrUtil.isBlank(k)) {
                continue;
            }
            if (doc.getTags() != null && doc.getTags().stream().anyMatch(t -> StrUtil.containsIgnoreCase(t, k))) {
                boost += tagBoost;
            }
            if (titleBoost > 0.0 && StrUtil.isNotBlank(doc.getTitle()) && StrUtil.containsIgnoreCase(doc.getTitle(), k)) {
                boost += titleBoost;
            }
        }
        return boost;
    }
}
