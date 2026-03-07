package com.zyyyys.culinarywhispers.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户画像实体
 */
@Data
@TableName("t_usr_profile")
@Schema(name = "UserProfile", description = "用户画像（含 VIP 状态）")
public class UserProfile {
    @TableId
    @Schema(description = "用户ID", example = "1")
    private Long userId;
    private Integer gender; // 0-Unknown, 1-Male, 2-Female
    private LocalDate birthday;
    private String signature;
    private String regionCode;
    private String country;
    private String province;
    private String city;
    private String realName;
    private String idCardNo;
    private String occupation;
    private String interests;
    private Integer cookAge;
    private String favoriteCuisine;
    private String tastePreference;
    private String dietaryRestrictions;
    @Schema(description = "VIP 到期时间", example = "2026-03-14T00:00:00")
    private LocalDateTime vipExpireTime;
    @Schema(description = "VIP 等级（0-未开通）", example = "1")
    private Integer vipLevel;
    private String wechatOpenid;
    private String wechatUnionid;
    private String weiboUid;
    private String tiktokUid;
    private Boolean isMasterChef;
    private String masterTitle;
    private String bgImageUrl;
    private String videoIntroUrl;
    private String contactEmail;
    /**
     * 用户总消费金额 (用于数据一致性演示)
     */
    private java.math.BigDecimal totalSpend;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
