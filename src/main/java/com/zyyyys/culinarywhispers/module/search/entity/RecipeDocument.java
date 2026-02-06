package com.zyyyys.culinarywhispers.module.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

/**
 * 食谱搜索文档
 * @author zyyyys
 */
@Data
@Document(indexName = "recipe_index")
public class RecipeDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    /**
     * 标题
     * 使用 ik_max_word 分词器，搜索时使用 ik_smart
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 标签
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 作者姓名
     */
    @Field(type = FieldType.Keyword)
    private String authorName;

    /**
     * 难度
     */
    @Field(type = FieldType.Integer)
    private Integer difficulty;

    /**
     * 耗时
     */
    @Field(type = FieldType.Integer)
    private Integer timeCost;

    /**
     * 评分
     */
    @Field(type = FieldType.Double)
    private Double score;
}
