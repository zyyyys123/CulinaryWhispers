package com.zyyyys.culinarywhispers.module.recipe.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食谱列表视图对象
 * @author zyyyys
 */
@Data
public class RecipePageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String coverUrl;
    private String description;
    private Integer difficulty;
    private Integer timeCost;
    private BigDecimal score;
    
    // 作者信息
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    // 统计数据
    private Long viewCount;
    private Long likeCount;
    private Long collectCount;

    private LocalDateTime gmtCreate;
}
