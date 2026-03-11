package com.zyyyys.culinarywhispers.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 用户成长数据VO
 * @author zyyyys
 */
@Data
@Schema(name = "UserStatsVO", description = "用户成长与统计数据")
public class UserStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    private Long userId;
    
    // 成长体系
    @Schema(description = "等级", example = "3")
    private Integer level;
    @Schema(description = "经验值", example = "1200")
    private Long experience;
    @Schema(description = "下一级所需经验", example = "1500")
    private Long nextLevelExperience; // 下一级所需经验
    @Schema(description = "当前等级进度（0.0-1.0）", example = "0.8")
    private Double levelProgress; // 当前等级进度 (0.0 - 1.0)
    
    // 勋章/认证
    @Schema(description = "是否认证大厨", example = "false")
    private Boolean isMasterChef;
    @Schema(description = "大厨称号", example = "资深大厨")
    private String masterTitle;
    @Schema(description = "勋章列表", example = "[\"人气之星\",\"资深大厨\"]")
    private List<String> badges; // 勋章列表: ["人气之星", "资深大厨"]

    // 统计数据
    @Schema(description = "累计发布食谱数", example = "12")
    private Integer totalRecipes;
    @Schema(description = "累计获赞数", example = "345")
    private Long totalLikesReceived;
    @Schema(description = "粉丝数", example = "89")
    private Integer totalFans;
}
