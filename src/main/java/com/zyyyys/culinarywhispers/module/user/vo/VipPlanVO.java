package com.zyyyys.culinarywhispers.module.user.vo;

import lombok.Data;

@Data
public class VipPlanVO {
    private Integer level;
    private String name;
    private Integer costPoints;
    private Integer durationDays;
    private String[] benefits;
}

