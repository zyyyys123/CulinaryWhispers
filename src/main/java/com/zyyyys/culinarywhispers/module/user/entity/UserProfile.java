package com.zyyyys.culinarywhispers.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户画像实体
 */
@Data
@TableName("t_usr_profile")
public class UserProfile {
    @TableId
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
    private LocalDateTime vipExpireTime;
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
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
