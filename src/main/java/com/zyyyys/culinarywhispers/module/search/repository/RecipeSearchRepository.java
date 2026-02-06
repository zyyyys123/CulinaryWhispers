package com.zyyyys.culinarywhispers.module.search.repository;

import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 食谱搜索Repository
 * @author zyyyys
 */
@Repository
public interface RecipeSearchRepository extends ElasticsearchRepository<RecipeDocument, Long> {
}
