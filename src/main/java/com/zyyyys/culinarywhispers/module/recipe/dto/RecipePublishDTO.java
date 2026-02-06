package com.zyyyys.culinarywhispers.module.recipe.dto;

import lombok.Data;
import java.util.List;

/**
 * 食谱发布 DTO
 * @author zyyyys
 */
@Data
public class RecipePublishDTO {
    /**
     * 标题
     */
    private String title;

    /**
     * 封面图
     */
    private String coverUrl;

    /**
     * 视频地址
     */
    private String videoUrl;

    /**
     * 简介
     */
    private String description;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 难度: 1-5
     */
    private Integer difficulty;

    /**
     * 耗时(分钟)
     */
    private Integer timeCost;

    /**
     * 小贴士
     */
    private String tips;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 步骤列表
     */
    private List<StepDTO> steps;

    /**
     * 步骤 DTO
     */
    @Data
    public static class StepDTO {
        private Integer stepNo;
        private String desc;
        private String imgUrl;
        private String videoUrl;
        private Integer timeCost;
        private Boolean isKeyStep;
    }
}
