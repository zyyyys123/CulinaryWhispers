package com.zyyyys.culinarywhispers.module.social.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FollowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private UserVO user;
    private LocalDateTime gmtCreate;
    private Boolean isMutual;
    private String remarkName;

    @Data
    public static class UserVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        private Boolean isMasterChef;
        private String masterTitle;
        private String bgImageUrl;
    }
}

