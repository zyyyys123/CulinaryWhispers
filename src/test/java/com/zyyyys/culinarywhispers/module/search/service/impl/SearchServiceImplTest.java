package com.zyyyys.culinarywhispers.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.repository.RecipeSearchRepository;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private RecipeSearchRepository recipeSearchRepository;
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private RecipeInfoMapper recipeInfoMapper;
    @Mock
    private RecipeTagMapper recipeTagMapper;
    @Mock
    private UserService userService;
    @Mock
    private RecipeStatsMapper statsMapper;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    void syncRecipe_Success() {
        Long recipeId = 100L;
        RecipeInfo info = new RecipeInfo();
        info.setId(recipeId);
        info.setTitle("Test Recipe");
        info.setAuthorId(1L);

        User author = new User();
        author.setNickname("Chef");

        when(recipeInfoMapper.selectById(recipeId)).thenReturn(info);
        when(userService.getById(1L)).thenReturn(author);
        when(recipeTagMapper.selectTagNamesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        // statsMapper mock default return null, which is handled

        searchService.syncRecipe(recipeId);

        verify(recipeSearchRepository).save(any(RecipeDocument.class));
    }

    @Test
    void syncRecipe_DeleteIfNotFound() {
        Long recipeId = 100L;
        when(recipeInfoMapper.selectById(recipeId)).thenReturn(null);

        searchService.syncRecipe(recipeId);

        verify(recipeSearchRepository).deleteById(recipeId);
    }

    @Test
    void searchRecipe_Success() {
        String keyword = "Test";
        
        // Mock SearchHits
        SearchHit<RecipeDocument> hit = mock(SearchHit.class);
        RecipeDocument doc = new RecipeDocument();
        doc.setTitle("Test Recipe");
        when(hit.getContent()).thenReturn(doc);
        
        SearchHits<RecipeDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(Collections.singletonList(hit));
        when(hits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(StringQuery.class), eq(RecipeDocument.class)))
                .thenReturn(hits);

        Page<RecipeDocument> result = searchService.searchRecipe(keyword, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("Test Recipe", result.getRecords().get(0).getTitle());
    }
}
