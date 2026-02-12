package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTag;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTagRelation;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeUpdatedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagRelationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecipeTagListenerTest {

    private RecipeTagMapper tagMapper;
    private RecipeTagRelationMapper relationMapper;
    private RecipeTagListener listener;

    @BeforeEach
    void setUp() {
        tagMapper = mock(RecipeTagMapper.class);
        relationMapper = mock(RecipeTagRelationMapper.class);
        listener = new RecipeTagListener(tagMapper, relationMapper);
    }

    @Test
    void handleRecipePublished_createsTagAndRelation() {
        when(tagMapper.selectOne(any())).thenReturn(null);
        when(tagMapper.insert(any(RecipeTag.class))).thenAnswer(invocation -> {
            RecipeTag tag = invocation.getArgument(0);
            tag.setId(10L);
            return 1;
        });

        RecipePublishedEvent event = new RecipePublishedEvent(this, 100L, 1L, List.of("T1"));
        listener.handleRecipePublished(event);

        verify(tagMapper).insert(any(RecipeTag.class));
        ArgumentCaptor<RecipeTagRelation> relationCaptor = ArgumentCaptor.forClass(RecipeTagRelation.class);
        verify(relationMapper).insert(relationCaptor.capture());
        assertEquals(100L, relationCaptor.getValue().getRecipeId());
        assertEquals(10L, relationCaptor.getValue().getTagId());
    }

    @Test
    void handleRecipePublished_existingTag_updatesUseCount() {
        RecipeTag existing = new RecipeTag();
        existing.setId(9L);
        existing.setUseCount(2);
        when(tagMapper.selectOne(any())).thenReturn(existing);

        RecipePublishedEvent event = new RecipePublishedEvent(this, 100L, 1L, List.of("T1"));
        listener.handleRecipePublished(event);

        verify(tagMapper).updateById(argThat(t -> t.getUseCount() == 3));
        verify(relationMapper).insert(any(RecipeTagRelation.class));
    }
    
    @Test
    void handleRecipePublished_duplicateTags_deduplicatesToAvoidDuplicateRelation() {
        when(tagMapper.selectOne(any())).thenReturn(null);
        when(tagMapper.insert(any(RecipeTag.class))).thenAnswer(invocation -> {
            RecipeTag tag = invocation.getArgument(0);
            tag.setId(10L);
            return 1;
        });

        RecipePublishedEvent event = new RecipePublishedEvent(this, 100L, 1L, List.of("T1", "T1", "  T1 "));
        assertDoesNotThrow(() -> listener.handleRecipePublished(event));

        verify(tagMapper, times(1)).insert(any(RecipeTag.class));
        verify(relationMapper, times(1)).insert(any(RecipeTagRelation.class));
    }

    @Test
    void handleRecipeUpdated_deletesOldRelations_thenProcess() {
        when(tagMapper.selectOne(any())).thenReturn(null);
        when(tagMapper.insert(any(RecipeTag.class))).thenAnswer(invocation -> {
            RecipeTag tag = invocation.getArgument(0);
            tag.setId(1L);
            return 1;
        });

        RecipeUpdatedEvent event = new RecipeUpdatedEvent(this, 100L, List.of("T1"));
        listener.handleRecipeUpdated(event);

        verify(relationMapper).delete(any());
        verify(relationMapper).insert(any(RecipeTagRelation.class));
    }

    @Test
    void handleRecipeDeleted_deletesRelations() {
        RecipeDeletedEvent event = new RecipeDeletedEvent(this, 100L);
        listener.handleRecipeDeleted(event);
        verify(relationMapper).delete(any());
    }
}
