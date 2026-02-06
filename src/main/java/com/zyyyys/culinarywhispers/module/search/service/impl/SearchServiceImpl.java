package com.zyyyys.culinarywhispers.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTag;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.repository.RecipeSearchRepository;
import com.zyyyys.culinarywhispers.module.search.service.SearchService;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final RecipeSearchRepository recipeSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeTagMapper recipeTagMapper; // 假设有 Tag Mapper
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

        // 获取 Tags (这里简化处理，假设有方法获取 tags)
        // List<String> tags = recipeTagMapper.selectTagsByRecipeId(recipeId);
        // doc.setTags(tags);

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
        // 构建查询条件
        Criteria criteria = new Criteria("title").contains(keyword)
                .or("description").contains(keyword)
                .or("tags").contains(keyword);

        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(page - 1, size));

        SearchHits<RecipeDocument> searchHits = elasticsearchOperations.search(query, RecipeDocument.class);
        
        List<RecipeDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        Page<RecipeDocument> resultPage = new Page<>(page, size);
        resultPage.setRecords(content);
        resultPage.setTotal(searchHits.getTotalHits());
        
        return resultPage;
    }
}
