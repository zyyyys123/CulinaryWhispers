package com.zyyyys.culinarywhispers.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新 DTO
 * @author zyyyys
 */
@Data
public class UserUpdateDTO {
    @Size(max = 32, message = "昵称长度不能超过32")
    private String nickname;

    @Size(max = 512, message = "头像URL长度不能超过512")
    private String avatarUrl;

    @Min(value = 0, message = "性别值非法")
    @Max(value = 2, message = "性别值非法")
    private Integer gender; // 0-Unknown, 1-Male, 2-Female

    @Size(max = 128, message = "个性签名长度不能超过128")
    private String signature;

    @Size(max = 64, message = "城市长度不能超过64")
    private String city;

    @Size(max = 64, message = "职业长度不能超过64")
    private String job;

    @Min(value = 0, message = "厨龄不能小于0")
    @Max(value = 80, message = "厨龄不能大于80")
    private Integer cookAge;

    @Size(max = 256, message = "喜爱菜系长度不能超过256")
    private String favoriteCuisine;

    @Size(max = 256, message = "口味偏好长度不能超过256")
    private String tastePreference;

    @Size(max = 256, message = "饮食忌口长度不能超过256")
    private String dietaryRestrictions;
}
