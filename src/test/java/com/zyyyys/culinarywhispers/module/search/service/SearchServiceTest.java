package com.zyyyys.culinarywhispers.module.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.repository.RecipeSearchRepository;
import com.zyyyys.culinarywhispers.module.search.service.impl.SearchServiceImpl;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

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
    public void testSearchRecipe() {
        // Mock SearchHits & SearchHit using Mockito to avoid constructor issues
        SearchHits<RecipeDocument> hits = mock(SearchHits.class);
        SearchHit<RecipeDocument> hit = mock(SearchHit.class);
        RecipeDocument doc = new RecipeDocument();
        doc.setId(1L);
        doc.setTitle("Test Recipe");

        when(hit.getContent()).thenReturn(doc);
        when(hits.getSearchHits()).thenReturn(Collections.singletonList(hit));
        when(hits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(RecipeDocument.class))).thenReturn(hits);

        Page<RecipeDocument> result = searchService.searchRecipe("Test", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("Test Recipe", result.getRecords().get(0).getTitle());
    }

    @Test
    public void testSearchPersonalized() {
        // Mock User Profile
        UserProfile profile = new UserProfile();
        profile.setUserId(1L);
        profile.setDietaryRestrictions("花生");
        profile.setFavoriteCuisine("川菜");
        when(userService.getUserProfile(1L)).thenReturn(profile);

        // Mock SearchHits
        SearchHits<RecipeDocument> hits = mock(SearchHits.class);
        SearchHit<RecipeDocument> hit = mock(SearchHit.class);
        RecipeDocument doc = new RecipeDocument();
        doc.setId(1L);
        doc.setTitle("Kung Pao Chicken");

        when(hit.getContent()).thenReturn(doc);
        when(hits.getSearchHits()).thenReturn(Collections.singletonList(hit));
        when(hits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(RecipeDocument.class))).thenReturn(hits);

        Page<RecipeDocument> result = searchService.searchPersonalized(1L, "Chicken", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }
}
