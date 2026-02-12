package com.zyyyys.culinarywhispers.module.user.vo;

import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import lombok.Data;

@Data
public class UserProfileVO {
    // 基础信息
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String mobile;
    private String email;
    
    // 画像信息
    private Integer gender;
    private String signature;
    private String city;
    private String job;
    private Integer cookAge;
    private String favoriteCuisine;
    private String tastePreference;
    private String dietaryRestrictions;
    private Boolean isMasterChef;
    
    // 静态工厂方法用于合并数据
    public static UserProfileVO from(User user, UserProfile profile) {
        UserProfileVO vo = new UserProfileVO();
        if (user != null) {
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setMobile(user.getMobile()); // 实际场景中需脱敏处理
            vo.setEmail(user.getEmail());
        }
        if (profile != null) {
            vo.setGender(profile.getGender());
            vo.setSignature(profile.getSignature());
            vo.setCity(profile.getCity());
            vo.setJob(profile.getOccupation());
            vo.setCookAge(profile.getCookAge());
            vo.setFavoriteCuisine(profile.getFavoriteCuisine());
            vo.setTastePreference(profile.getTastePreference());
            vo.setDietaryRestrictions(profile.getDietaryRestrictions());
            vo.setIsMasterChef(profile.getIsMasterChef());
        }
        return vo;
    }
}
