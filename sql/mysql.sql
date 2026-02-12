-- MySQL Database Design for CulinaryWhispers
-- Source: docs/summary/02_DATABASE_DESIGN.md

CREATE DATABASE IF NOT EXISTS culinary_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE culinary_user;

-- 2.1 用户中心 (User Center)

-- 2.1.1 用户基础表 t_usr_base
CREATE TABLE `t_usr_base` (
  `id` bigint(20) unsigned NOT NULL COMMENT '用户ID (Snowflake)',
  `username` varchar(64) NOT NULL COMMENT '用户名 (唯一)',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号 (加密存储)',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `password_hash` varchar(128) NOT NULL COMMENT '密码哈希 (Argon2)',
  `salt` varchar(32) NOT NULL COMMENT '密码盐',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像地址',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-正常, 2-冻结, 3-注销',
  `register_source` varchar(32) DEFAULT NULL COMMENT '注册来源: APP, WEB, WX',
  `register_ip` varchar(64) DEFAULT NULL COMMENT '注册IP',
  `last_login_time` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `version` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础表';

-- 2.1.2 用户画像表 t_usr_profile
CREATE TABLE `t_usr_profile` (
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `gender` tinyint(4) DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
  `region_code` varchar(20) DEFAULT NULL COMMENT '地区编码',
  `country` varchar(50) DEFAULT NULL COMMENT '国家',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `id_card_no` varchar(128) DEFAULT NULL COMMENT '身份证号(加密)',
  `occupation` varchar(64) DEFAULT NULL COMMENT '职业',
  `interests` varchar(512) DEFAULT NULL COMMENT '兴趣标签(逗号分隔)',
  `cook_age` int(11) DEFAULT 0 COMMENT '厨龄(年)',
  `favorite_cuisine` varchar(255) DEFAULT NULL COMMENT '擅长菜系',
  `taste_preference` varchar(255) DEFAULT NULL COMMENT '口味偏好(辣, 甜)',
  `dietary_restrictions` varchar(255) DEFAULT NULL COMMENT '饮食忌口',
  `vip_expire_time` datetime(3) DEFAULT NULL COMMENT '会员过期时间',
  `vip_level` int(11) DEFAULT 0 COMMENT '会员等级',
  `wechat_openid` varchar(64) DEFAULT NULL COMMENT '微信OpenID',
  `wechat_unionid` varchar(64) DEFAULT NULL COMMENT '微信UnionID',
  `weibo_uid` varchar(64) DEFAULT NULL COMMENT '微博UID',
  `tiktok_uid` varchar(64) DEFAULT NULL COMMENT '抖音UID',
  `is_master_chef` tinyint(1) DEFAULT 0 COMMENT '是否认证大厨',
  `master_title` varchar(128) DEFAULT NULL COMMENT '大厨头衔',
  `bg_image_url` varchar(512) DEFAULT NULL COMMENT '个人主页背景图',
  `video_intro_url` varchar(512) DEFAULT NULL COMMENT '视频介绍地址',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '商务联系邮箱',
  `total_spend` decimal(12,2) DEFAULT 0.00 COMMENT '用户总消费金额',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- 2.1.3 用户统计表 t_usr_stats
CREATE TABLE `t_usr_stats` (
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `level` int(11) DEFAULT 1 COMMENT '用户等级',
  `experience` bigint(20) DEFAULT 0 COMMENT '经验值',
  `total_recipes` int(11) DEFAULT 0 COMMENT '发布食谱数',
  `total_moments` int(11) DEFAULT 0 COMMENT '发布动态数',
  `total_likes_received` bigint(20) DEFAULT 0 COMMENT '获赞总数',
  `total_collects_received` bigint(20) DEFAULT 0 COMMENT '被收藏总数',
  `total_fans` int(11) DEFAULT 0 COMMENT '粉丝数',
  `total_follows` int(11) DEFAULT 0 COMMENT '关注数',
  `total_views` bigint(20) DEFAULT 0 COMMENT '主页访问量',
  `week_active_days` int(11) DEFAULT 0 COMMENT '周活跃天数',
  `month_active_days` int(11) DEFAULT 0 COMMENT '月活跃天数',
  `last_publish_time` datetime(3) DEFAULT NULL COMMENT '最后发布时间',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户统计表';

-- 2.2 食谱中心 (Recipe Center)

-- 2.2.1 食谱主表 t_rcp_info
CREATE TABLE `t_rcp_info` (
  `id` bigint(20) unsigned NOT NULL COMMENT '食谱ID',
  `author_id` bigint(20) unsigned NOT NULL COMMENT '作者ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `cover_url` varchar(512) NOT NULL COMMENT '封面图',
  `video_url` varchar(512) DEFAULT NULL COMMENT '视频地址',
  `description` text COMMENT '简介',
  `category_id` int(11) NOT NULL COMMENT '分类ID',
  `cuisine_id` int(11) DEFAULT NULL COMMENT '菜系ID',
  `difficulty` tinyint(4) DEFAULT 1 COMMENT '难度: 1-5',
  `time_cost` int(11) DEFAULT 0 COMMENT '耗时(分钟)',
  `calories` int(11) DEFAULT 0 COMMENT '卡路里(千卡)',
  `protein` decimal(10,2) DEFAULT 0 COMMENT '蛋白质(g)',
  `fat` decimal(10,2) DEFAULT 0 COMMENT '脂肪(g)',
  `carbs` decimal(10,2) DEFAULT 0 COMMENT '碳水(g)',
  `score` decimal(3,1) DEFAULT 0.0 COMMENT '综合评分',
  `view_count` bigint(20) DEFAULT 0 COMMENT '浏览量',
  `like_count` bigint(20) DEFAULT 0 COMMENT '点赞量',
  `collect_count` bigint(20) DEFAULT 0 COMMENT '收藏量',
  `comment_count` bigint(20) DEFAULT 0 COMMENT '评论量',
  `share_count` bigint(20) DEFAULT 0 COMMENT '分享量',
  `try_count` int(11) DEFAULT 0 COMMENT '跟做人数',
  `status` tinyint(4) DEFAULT 0 COMMENT '状态: 0-草稿, 1-审核中, 2-发布, 3-驳回, 4-下架',
  `audit_msg` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `tags` varchar(512) DEFAULT NULL COMMENT '标签JSON数组',
  `tips` text COMMENT '小贴士',
  `is_exclusive` tinyint(1) DEFAULT 0 COMMENT '是否独家',
  `is_paid` tinyint(1) DEFAULT 0 COMMENT '是否付费',
  `price` decimal(10,2) DEFAULT 0.00 COMMENT '价格',
  `publish_time` datetime(3) DEFAULT NULL COMMENT '发布时间',
  `ip_location` varchar(64) DEFAULT NULL COMMENT '发布IP属地',
  `device_info` varchar(128) DEFAULT NULL COMMENT '发布设备',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `version` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱主表';

-- 2.2.2 食谱步骤表 t_rcp_step
CREATE TABLE `t_rcp_step` (
  `id` bigint(20) unsigned NOT NULL,
  `recipe_id` bigint(20) unsigned NOT NULL COMMENT '食谱ID',
  `step_no` int(11) NOT NULL COMMENT '步骤序号',
  `desc` text NOT NULL COMMENT '步骤描述',
  `img_url` varchar(512) DEFAULT NULL COMMENT '步骤图',
  `video_url` varchar(512) DEFAULT NULL COMMENT '步骤视频片段',
  `time_cost` int(11) DEFAULT 0 COMMENT '该步骤耗时',
  `is_key_step` tinyint(1) DEFAULT 0 COMMENT '是否关键步骤',
  `voice_url` varchar(512) DEFAULT NULL COMMENT '语音讲解',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱步骤表';

-- 2.2.3 食谱扩展表 (Recipe Extensions)
-- Recipe Category Table
CREATE TABLE IF NOT EXISTS `t_rcp_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Category ID',
  `name` varchar(64) NOT NULL COMMENT 'Category Name',
  `parent_id` int(11) NOT NULL DEFAULT 0 COMMENT 'Parent Category ID',
  `level` tinyint(4) NOT NULL DEFAULT 1 COMMENT 'Level: 1-Root, 2-Sub, 3-Leaf',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT 'Sort Order',
  `icon_url` varchar(512) DEFAULT NULL COMMENT 'Icon URL',
  `is_visible` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Visibility',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recipe Category Table';

-- Recipe Tag Table
CREATE TABLE IF NOT EXISTS `t_rcp_tag` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Tag ID',
  `name` varchar(64) NOT NULL COMMENT 'Tag Name',
  `type` tinyint(4) DEFAULT 1 COMMENT 'Tag Type: 1-General, 2-Ingredient, 3-Scene',
  `use_count` int(11) DEFAULT 0 COMMENT 'Usage Count',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recipe Tag Table';

-- Recipe Tag Relation Table
CREATE TABLE IF NOT EXISTS `t_rcp_tag_relation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint(20) unsigned NOT NULL COMMENT 'Recipe ID',
  `tag_id` bigint(20) unsigned NOT NULL COMMENT 'Tag ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recipe_tag` (`recipe_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recipe Tag Relation Table';

-- Recipe Stats Table (Separated from Info)
CREATE TABLE IF NOT EXISTS `t_rcp_stats` (
  `recipe_id` bigint(20) unsigned NOT NULL COMMENT 'Recipe ID',
  `view_count` bigint(20) DEFAULT 0 COMMENT 'View Count',
  `like_count` bigint(20) DEFAULT 0 COMMENT 'Like Count',
  `collect_count` bigint(20) DEFAULT 0 COMMENT 'Collect Count',
  `comment_count` bigint(20) DEFAULT 0 COMMENT 'Comment Count',
  `share_count` bigint(20) DEFAULT 0 COMMENT 'Share Count',
  `try_count` int(11) DEFAULT 0 COMMENT 'Try Count',
  `score` decimal(3,1) DEFAULT 0.0 COMMENT 'Score',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recipe Statistics Table';

-- 2.3 社交互动中心 (Social Center)

-- 2.3.1 评论表 t_soc_comment
CREATE TABLE `t_soc_comment` (
  `id` bigint(20) unsigned NOT NULL COMMENT '评论ID',
  `target_type` tinyint(4) NOT NULL COMMENT '目标类型: 1-食谱, 2-动态, 3-课程',
  `target_id` bigint(20) unsigned NOT NULL COMMENT '目标ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '评论者ID',
  `root_id` bigint(20) unsigned DEFAULT 0 COMMENT '根评论ID',
  `parent_id` bigint(20) unsigned DEFAULT 0 COMMENT '父评论ID',
  `content` text NOT NULL COMMENT '评论内容',
  `img_urls` json DEFAULT NULL COMMENT '图片列表',
  `like_count` int(11) DEFAULT 0 COMMENT '点赞数',
  `is_hot` tinyint(1) DEFAULT 0 COMMENT '是否神评',
  `is_sticky` tinyint(1) DEFAULT 0 COMMENT '是否置顶',
  `ip_location` varchar(64) DEFAULT NULL,
  `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-正常, 2-折叠, 3-删除',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_id`, `target_type`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用评论表';

-- 2.3.2 互动行为表 t_soc_interaction
CREATE TABLE `t_soc_interaction` (
  `id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `target_type` tinyint(4) NOT NULL COMMENT '目标类型',
  `target_id` bigint(20) unsigned NOT NULL COMMENT '目标ID',
  `action_type` tinyint(4) NOT NULL COMMENT '动作: 1-点赞, 2-收藏, 3-分享',
  `device_id` varchar(128) DEFAULT NULL,
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target_action` (`user_id`, `target_type`, `target_id`, `action_type`),
  KEY `idx_target_action` (`target_id`, `target_type`, `action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户互动流水表';

-- 2.4 电商交易中心 (Mall Center)

-- 2.4.1 商品 SPU 表 t_mall_spu
CREATE TABLE `t_mall_spu` (
  `id` bigint(20) unsigned NOT NULL COMMENT '商品ID',
  `shop_id` bigint(20) unsigned NOT NULL COMMENT '店铺ID',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `sub_title` varchar(255) DEFAULT NULL COMMENT '副标题',
  `category_id` bigint(20) NOT NULL COMMENT '分类ID',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '品牌ID',
  `main_images` json NOT NULL COMMENT '主图列表',
  `video_url` varchar(512) DEFAULT NULL COMMENT '商品视频',
  `detail_html` mediumtext COMMENT '详情页HTML',
  `spec_list` json COMMENT '规格列表',
  `price_low` decimal(10,2) NOT NULL COMMENT '最低价',
  `price_high` decimal(10,2) NOT NULL COMMENT '最高价',
  `stock_total` int(11) DEFAULT 0 COMMENT '总库存',
  `sales_total` int(11) DEFAULT 0 COMMENT '总销量',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- 2.4.2 订单主表 t_trd_order
CREATE TABLE `t_trd_order` (
  `id` bigint(20) unsigned NOT NULL COMMENT '订单ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '买家ID',
  `shop_id` bigint(20) unsigned NOT NULL COMMENT '店铺ID',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `total_amount` decimal(18,2) NOT NULL COMMENT '总金额',
  `pay_amount` decimal(18,2) NOT NULL COMMENT '实付金额',
  `freight_amount` decimal(18,2) DEFAULT 0.00 COMMENT '运费',
  `discount_amount` decimal(18,2) DEFAULT 0.00 COMMENT '优惠金额',
  `coupon_id` bigint(20) DEFAULT NULL COMMENT '优惠券ID',
  `status` tinyint(4) NOT NULL COMMENT '状态: 10-待支付, 20-待发货, 30-待收货, 40-已完成, 50-已取消, 60-售后中',
  `pay_type` tinyint(4) DEFAULT NULL COMMENT '支付方式: 1-支付宝, 2-微信',
  `pay_time` datetime(3) DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime(3) DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime(3) DEFAULT NULL COMMENT '收货时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `remark` varchar(255) DEFAULT NULL COMMENT '买家备注',
  `receiver_name` varchar(64) NOT NULL COMMENT '收货人',
  `receiver_mobile` varchar(20) NOT NULL COMMENT '收货手机',
  `receiver_addr` varchar(512) NOT NULL COMMENT '收货地址',
  `logistics_company` varchar(64) DEFAULT NULL COMMENT '物流公司',
  `logistics_no` varchar(64) DEFAULT NULL COMMENT '物流单号',
  `is_invoice` tinyint(1) DEFAULT 0 COMMENT '是否开发票',
  `invoice_title` varchar(128) DEFAULT NULL COMMENT '发票抬头',
  `source_type` tinyint(4) DEFAULT 1 COMMENT '订单来源',
  `auto_confirm_day` int(11) DEFAULT 15 COMMENT '自动收货天数',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_create_time` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 2.5 财务中心 (Finance Center)

-- 2.5.1 账户资产表 t_fin_account
CREATE TABLE `t_fin_account` (
  `id` bigint(20) unsigned NOT NULL COMMENT '账户ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `balance` decimal(18,2) DEFAULT 0.00 COMMENT '可用余额(CNY)',
  `frozen_balance` decimal(18,2) DEFAULT 0.00 COMMENT '冻结金额',
  `coin_balance` decimal(18,2) DEFAULT 0.00 COMMENT '金币余额(虚拟币)',
  `points` bigint(20) DEFAULT 0 COMMENT '积分',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态: 1-正常, 2-冻结',
  `pay_password_hash` varchar(128) DEFAULT NULL COMMENT '支付密码',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资产账户表';

-- 2.5.2 资金流水表 t_fin_transaction_flow
CREATE TABLE `t_fin_transaction_flow` (
  `id` bigint(20) unsigned NOT NULL COMMENT '流水ID',
  `transaction_no` varchar(64) NOT NULL COMMENT '系统交易流水号',
  `account_id` bigint(20) unsigned NOT NULL COMMENT '关联账户ID',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `out_trade_no` varchar(128) DEFAULT NULL COMMENT '第三方支付流水号',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `account_type` tinyint(4) NOT NULL COMMENT '账户类型: 1-余额, 2-微信, 3-支付宝, 4-银行卡',
  `flow_type` tinyint(4) NOT NULL COMMENT '流水类型: 1-收入, 2-支出, 3-冻结, 4-解冻',
  `biz_type` tinyint(4) NOT NULL COMMENT '业务类型: 1-充值, 2-消费, 3-退款, 4-提现, 5-佣金结算',
  `amount` decimal(18,2) NOT NULL COMMENT '变动金额',
  `balance_after` decimal(18,2) DEFAULT 0.00 COMMENT '变动后余额',
  `status` tinyint(4) NOT NULL COMMENT '状态: 0-处理中, 1-成功, 2-失败',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_biz` (`user_id`, `biz_type`),
  KEY `idx_create_time` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水表';

-- 2.5.3 结算单表 t_fin_settlement
CREATE TABLE `t_fin_settlement` (
  `id` bigint(20) unsigned NOT NULL,
  `settle_no` varchar(64) NOT NULL COMMENT '结算单号',
  `merchant_id` bigint(20) unsigned NOT NULL COMMENT '商家/用户ID',
  `cycle_start` date NOT NULL COMMENT '结算周期开始',
  `cycle_end` date NOT NULL COMMENT '结算周期结束',
  `total_amount` decimal(18,2) NOT NULL COMMENT '总交易额',
  `commission_fee` decimal(18,2) NOT NULL COMMENT '平台佣金',
  `settle_amount` decimal(18,2) NOT NULL COMMENT '应结金额',
  `status` tinyint(4) NOT NULL COMMENT '状态: 0-待审核, 1-已打款',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settle_no` (`settle_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单表';

-- ================== 2.6 兼容后端实体的补充表结构 ==================
-- 说明：后端模块使用的表名与原设计稿存在差异，这里以“不破坏原表”的方式补齐缺失表，确保功能可跑通。

-- 2.6.1 社交关注表 t_soc_follow
CREATE TABLE IF NOT EXISTS `t_soc_follow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `following_id` bigint(20) unsigned NOT NULL COMMENT '被关注用户ID',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_following` (`user_id`, `following_id`),
  KEY `idx_following` (`following_id`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- 2.6.2 用户积分流水表 t_points_record
CREATE TABLE IF NOT EXISTS `t_points_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `type` int(11) NOT NULL COMMENT '类型: 1-签到,2-发布食谱,3-被点赞,10-兑换商品',
  `amount` int(11) NOT NULL COMMENT '变动数量(可为负)',
  `description` varchar(255) DEFAULT NULL COMMENT '说明',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分流水表';

-- 2.6.3 电商商品表 t_comm_product
CREATE TABLE IF NOT EXISTS `t_comm_product` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `price` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `stock` int(11) NOT NULL DEFAULT 0 COMMENT '库存',
  `category_id` int(11) DEFAULT 0 COMMENT '分类ID',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商商品表';

-- 2.6.4 电商订单表 t_comm_order
CREATE TABLE IF NOT EXISTS `t_comm_order` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付,1-已支付,2-已发货,3-已完成,4-已取消',
  `pay_time` datetime(3) DEFAULT NULL COMMENT '支付时间',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_create_time` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商订单表';

-- 2.6.5 电商订单明细表 t_comm_order_item
CREATE TABLE IF NOT EXISTS `t_comm_order_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint(20) unsigned NOT NULL COMMENT '订单ID',
  `product_id` bigint(20) unsigned NOT NULL COMMENT '商品ID',
  `price` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '购买单价',
  `count` int(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商订单明细表';
