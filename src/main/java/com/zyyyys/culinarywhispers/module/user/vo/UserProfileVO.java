package com.zyyyys.culinarywhispers.module.user.vo;

import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "UserProfileVO", description = "用户主页信息（含 VIP 状态）")
public class UserProfileVO {
    // 基础信息
    @Schema(description = "用户ID", example = "1")
    private Long id;
    @Schema(description = "用户名", example = "test_user")
    private String username;
    @Schema(description = "昵称", example = "小厨神")
    private String nickname;
    @Schema(description = "头像URL", example = "/uploads/avatar.png")
    private String avatarUrl;
    @Schema(description = "手机号", example = "138****0000")
    private String mobile;
    @Schema(description = "邮箱", example = "test@example.com")
    private String email;
    
    // 画像信息
    @Schema(description = "性别：0-未知,1-男,2-女", example = "0")
    private Integer gender;
    @Schema(description = "个性签名", example = "热爱烹饪")
    private String signature;
    @Schema(description = "城市", example = "上海")
    private String city;
    @Schema(description = "职业", example = "工程师")
    private String job;
    @Schema(description = "厨龄（年）", example = "3")
    private Integer cookAge;
    @Schema(description = "偏好菜系", example = "川菜")
    private String favoriteCuisine;
    @Schema(description = "口味偏好", example = "微辣")
    private String tastePreference;
    @Schema(description = "饮食限制", example = "无")
    private String dietaryRestrictions;
    @Schema(description = "是否认证大厨", example = "false")
    private Boolean isMasterChef;
    @Schema(description = "大厨称号", example = "资深大厨")
    private String masterTitle;
    @Schema(description = "个人页背景图URL", example = "/uploads/bg.png")
    private String bgImageUrl;
    @Schema(description = "总消费金额", example = "99.99")
    private BigDecimal totalSpend;
    @Schema(description = "国家", example = "中国")
    private String country;
    @Schema(description = "省份", example = "上海")
    private String province;
    @Schema(description = "兴趣标签", example = "烘焙,家常菜")
    private String interests;
    @Schema(description = "VIP 等级（0-未开通）", example = "1")
    private Integer vipLevel;
    @Schema(description = "VIP 到期时间", example = "2026-03-14T00:00:00")
    private LocalDateTime vipExpireTime;
    @Schema(description = "是否管理员", example = "false")
    private Boolean isAdmin;
    
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
            vo.setIsAdmin("admin".equals(user.getUsername()));
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
            vo.setMasterTitle(profile.getMasterTitle());
            vo.setBgImageUrl(profile.getBgImageUrl());
            vo.setTotalSpend(profile.getTotalSpend());
            vo.setCountry(profile.getCountry());
            vo.setProvince(profile.getProvince());
            vo.setInterests(profile.getInterests());
            vo.setVipLevel(profile.getVipLevel());
            vo.setVipExpireTime(profile.getVipExpireTime());
        }
        return vo;
    }
}
