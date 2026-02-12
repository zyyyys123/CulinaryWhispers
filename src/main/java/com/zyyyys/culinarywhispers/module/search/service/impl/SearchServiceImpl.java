package com.zyyyys.culinarywhispers.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.repository.RecipeSearchRepository;
import com.zyyyys.culinarywhispers.module.search.service.SearchService;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "cw.search.type", havingValue = "elasticsearch", matchIfMissing = false)
@ConditionalOnBean({RecipeSearchRepository.class, ElasticsearchOperations.class})
public class SearchServiceImpl implements SearchService {

    private final RecipeSearchRepository recipeSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeTagMapper recipeTagMapper;
    private final UserService userService;
    private final RecipeStatsMapper statsMapper;

    /**
     * 同步食谱到 Elasticsearch
     * @param recipeId 食谱ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRecipe(Long recipeId) {
        RecipeInfo info = recipeInfoMapper.selectById(recipeId);
        if (info == null) {
            // 如果 DB 中不存在，则尝试删除 ES 中的数据
            deleteRecipe(recipeId);
            return;
        }

        // 组装 Document
        RecipeDocument doc = new RecipeDocument();
        doc.setId(info.getId());
        doc.setTitle(info.getTitle());
        doc.setDescription(info.getDescription());
        doc.setDifficulty(info.getDifficulty());
        doc.setTimeCost(info.getTimeCost());

        // 获取作者名
        User author = userService.getById(info.getAuthorId());
        if (author != null) {
            doc.setAuthorName(author.getNickname());
        }

        // 获取 Tags
        List<String> tags = recipeTagMapper.selectTagNamesByRecipeId(recipeId);
        doc.setTags(tags);

        // 获取评分
        RecipeStats stats = statsMapper.selectById(recipeId);
        if (stats != null) {
            doc.setScore(stats.getScore() != null ? stats.getScore().doubleValue() : 0.0);
        }

        recipeSearchRepository.save(doc);
        log.info("Synced recipe to ES: {}", recipeId);
    }

    /**
     * 从 Elasticsearch 删除食谱
     * @param recipeId 食谱ID
     */
    @Override
    public void deleteRecipe(Long recipeId) {
        recipeSearchRepository.deleteById(recipeId);
        log.info("Deleted recipe from ES: {}", recipeId);
    }

    /**
     * 搜索食谱
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 搜索结果分页
     */
    @Override
    public Page<RecipeDocument> searchRecipe(String keyword, int page, int size) {
        // 构建查询条件 (使用 StringQuery 统一逻辑)
        Map<String, Object> multiMatch = new HashMap<>();
        multiMatch.put("query", keyword);
        multiMatch.put("fields", new String[]{"title", "description", "tags"});
        
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("multi_match", multiMatch);

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(queryMap);
        } catch (Exception e) {
            log.error("Failed to build search query json", e);
            throw new RuntimeException("Search query build failed");
        }

        StringQuery query = new StringQuery(json);
        query.setPageable(PageRequest.of(page - 1, size));

        SearchHits<RecipeDocument> searchHits = elasticsearchOperations.search(query, RecipeDocument.class);
        
        return mapSearchHitsToPage(searchHits, page, size);
    }

    /**
     * 个性化搜索食谱
     */
    @Override
    public Page<RecipeDocument> searchPersonalized(Long userId, String keyword, int page, int size) {
        Map<String, Object> boolMap = new HashMap<>();
        List<Object> mustList = new ArrayList<>();
        List<Object> mustNotList = new ArrayList<>();
        List<Object> shouldList = new ArrayList<>();

        // 1. 基础关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", keyword);
            multiMatch.put("fields", new String[]{"title", "description", "tags"});
            
            Map<String, Object> matchQuery = new HashMap<>();
            matchQuery.put("multi_match", multiMatch);
            mustList.add(matchQuery);
        } else {
            Map<String, Object> matchAll = new HashMap<>();
            matchAll.put("match_all", new HashMap<>());
            mustList.add(matchAll);
        }

        // 2. 获取用户画像
        if (userId != null) {
            UserProfile profile = userService.getUserProfile(userId);
            if (profile != null) {
                // 2.1 忌口过滤 (Must Not)
                if (StrUtil.isNotBlank(profile.getDietaryRestrictions())) {
                    String[] restrictions = profile.getDietaryRestrictions().split("[,， ]+");
                    for (String restriction : restrictions) {
                        if (StrUtil.isNotBlank(restriction)) {
                            Map<String, Object> term1 = new HashMap<>();
                            term1.put("tags", restriction);
                            Map<String, Object> match1 = new HashMap<>();
                            match1.put("match", term1);
                            mustNotList.add(match1);

                            Map<String, Object> term2 = new HashMap<>();
                            term2.put("description", restriction);
                            Map<String, Object> match2 = new HashMap<>();
                            match2.put("match", term2);
                            mustNotList.add(match2);
                        }
                    }
                }

                // 2.2 口味偏好加权 (Should Boost)
                if (StrUtil.isNotBlank(profile.getFavoriteCuisine())) {
                    String[] cuisines = profile.getFavoriteCuisine().split("[,， ]+");
                    for (String cuisine : cuisines) {
                        if (StrUtil.isNotBlank(cuisine)) {
                            Map<String, Object> term1 = new HashMap<>();
                            term1.put("query", cuisine);
                            term1.put("boost", 2.0);
                            Map<String, Object> field1 = new HashMap<>();
                            field1.put("tags", term1);
                            Map<String, Object> match1 = new HashMap<>();
                            match1.put("match", field1);
                            shouldList.add(match1);

                            Map<String, Object> term2 = new HashMap<>();
                            term2.put("query", cuisine);
                            term2.put("boost", 1.5);
                            Map<String, Object> field2 = new HashMap<>();
                            field2.put("title", term2);
                            Map<String, Object> match2 = new HashMap<>();
                            match2.put("match", field2);
                            shouldList.add(match2);
                        }
                    }
                }
                
                // 2.3 口味偏好加权
                 if (StrUtil.isNotBlank(profile.getTastePreference())) {
                    String[] tastes = profile.getTastePreference().split("[,， ]+");
                    for (String taste : tastes) {
                        if (StrUtil.isNotBlank(taste)) {
                            Map<String, Object> term1 = new HashMap<>();
                            term1.put("query", taste);
                            term1.put("boost", 1.5);
                            Map<String, Object> field1 = new HashMap<>();
                            field1.put("tags", term1);
                            Map<String, Object> match1 = new HashMap<>();
                            match1.put("match", field1);
                            shouldList.add(match1);
                        }
                    }
                }
            }
        }

        boolMap.put("must", mustList);
        if (!mustNotList.isEmpty()) {
            boolMap.put("must_not", mustNotList);
        }
        if (!shouldList.isEmpty()) {
            boolMap.put("should", shouldList);
        }

        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("bool", boolMap);

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(queryMap);
        } catch (Exception e) {
            log.error("Failed to build search query json", e);
            throw new RuntimeException("Search query build failed");
        }

        StringQuery query = new StringQuery(json);
        query.setPageable(PageRequest.of(page - 1, size));

        SearchHits<RecipeDocument> searchHits = elasticsearchOperations.search(query, RecipeDocument.class);
        return mapSearchHitsToPage(searchHits, page, size);
    }

    private Page<RecipeDocument> mapSearchHitsToPage(SearchHits<RecipeDocument> searchHits, int page, int size) {
        List<RecipeDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        Page<RecipeDocument> resultPage = new Page<>(page, size);
        resultPage.setRecords(content);
        resultPage.setTotal(searchHits.getTotalHits());
        return resultPage;
    }
}
