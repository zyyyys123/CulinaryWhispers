package com.zyyyys.culinarywhispers.module.user.dto;

import lombok.Data;

/**
 * 用户信息更新 DTO
 * @author zyyyys
 */
@Data
public class UserUpdateDTO {
    private String nickname;
    private String avatarUrl;
    private Integer gender; // 0-Unknown, 1-Male, 2-Female
    private String signature;
    private String city;
    private String job;
}
