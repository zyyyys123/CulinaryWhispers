CREATE TABLE IF NOT EXISTS `t_soc_interaction` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `target_type` int(11) NOT NULL COMMENT '目标类型 (1:食谱, 2:评论)',
  `target_id` bigint(20) unsigned NOT NULL COMMENT '目标ID',
  `action_type` int(11) NOT NULL COMMENT '动作类型 (1:点赞, 2:收藏, 3:分享)',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target_action` (`user_id`,`target_type`,`target_id`,`action_type`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户互动表';

CREATE TABLE IF NOT EXISTS `t_soc_comment` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `recipe_id` bigint(20) unsigned NOT NULL COMMENT '食谱ID',
  `content` varchar(1024) NOT NULL COMMENT '内容',
  `parent_id` bigint(20) unsigned DEFAULT NULL COMMENT '父评论ID',
  `like_count` int(11) NOT NULL DEFAULT '0' COMMENT '点赞数',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_recipe` (`recipe_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE IF NOT EXISTS `t_soc_follow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `follower_id` bigint(20) unsigned NOT NULL COMMENT '粉丝ID',
  `following_id` bigint(20) unsigned NOT NULL COMMENT '被关注者ID',
  `remark_name` varchar(64) DEFAULT NULL COMMENT '备注名',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_following` (`following_id`),
  KEY `idx_follower` (`follower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

CREATE TABLE IF NOT EXISTS `t_points_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `type` int(11) NOT NULL COMMENT '类型 1:签到',
  `amount` int(11) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';
