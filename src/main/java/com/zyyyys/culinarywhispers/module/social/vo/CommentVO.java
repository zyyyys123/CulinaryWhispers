package com.zyyyys.culinarywhispers.module.social.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long recipeId;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private LocalDateTime gmtCreate;
    private AuthorVO author;

    @Data
    public static class AuthorVO implements Serializable {

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

