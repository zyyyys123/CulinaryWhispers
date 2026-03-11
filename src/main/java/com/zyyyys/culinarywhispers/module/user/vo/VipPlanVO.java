package com.zyyyys.culinarywhispers.module.user.vo;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(name = "VipPlanVO", description = "VIP 计划")
public class VipPlanVO {
    @Schema(description = "VIP 等级", example = "1")
    private Integer level;
    @Schema(description = "计划名称", example = "VIP 青铜")
    private String name;
    @Schema(description = "兑换所需积分", example = "200")
    private Integer costPoints;
    @Schema(description = "有效期（天）", example = "7")
    private Integer durationDays;
    @Schema(description = "权益列表", example = "[\"专属勋章与个人页标识\",\"推荐权重提升（规划）\"]")
    private String[] benefits;
}
