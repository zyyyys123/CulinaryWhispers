package com.zyyyys.culinarywhispers.module.user.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 用户成长数据VO
 * @author zyyyys
 */
@Data
public class UserStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    
    // 成长体系
    private Integer level;
    private Long experience;
    private Long nextLevelExperience; // 下一级所需经验
    private Double levelProgress; // 当前等级进度 (0.0 - 1.0)
    
    // 勋章/认证
    private Boolean isMasterChef;
    private String masterTitle;
    private List<String> badges; // 勋章列表: ["人气之星", "资深大厨"]

    // 统计数据
    private Integer totalRecipes;
    private Long totalLikesReceived;
    private Integer totalFans;
}
