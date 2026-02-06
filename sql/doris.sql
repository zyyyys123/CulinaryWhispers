-- Doris OLAP Database Design for CulinaryWhispers
-- Source: docs/summary/06_DORIS_OLAP.md

CREATE DATABASE IF NOT EXISTS culinary_dw;

USE culinary_dw;

-- 3.1 用户行为流水表 (Duplicate Model)
-- 用于记录每一次用户操作，数据量极大，不进行聚合，只做追加。
CREATE TABLE dwd_user_behavior_log (
    event_time DATETIME NOT NULL COMMENT "事件时间",
    user_id BIGINT NOT NULL COMMENT "用户ID",
    event_type VARCHAR(32) NOT NULL COMMENT "事件类型: view, click, like, buy",
    item_id BIGINT COMMENT "对象ID (食谱/商品)",
    channel VARCHAR(32) COMMENT "来源渠道: app, web, mini_program",
    device_os VARCHAR(32) COMMENT "操作系统",
    ip VARCHAR(64) COMMENT "IP地址"
)
ENGINE=OLAP
DUPLICATE KEY(event_time, user_id)
PARTITION BY RANGE(event_time) (
    PARTITION p202601 VALUES [("2026-01-01"), ("2026-02-01"))
)
DISTRIBUTED BY HASH(user_id) BUCKETS 32
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",
    "dynamic_partition.start" = "-3",
    "dynamic_partition.end" = "3",
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "32"
);

-- 3.2 食谱日统计表 (Aggregate Model)
-- 利用 Doris 的聚合模型，在导入数据时自动计算 SUM, COUNT, MAX。
CREATE TABLE dws_recipe_daily_stats (
    stat_date DATE NOT NULL COMMENT "统计日期",
    recipe_id BIGINT NOT NULL COMMENT "食谱ID",
    category_id INT COMMENT "分类ID",
    
    view_count BIGINT SUM DEFAULT "0" COMMENT "浏览量",
    like_count BIGINT SUM DEFAULT "0" COMMENT "点赞量",
    comment_count BIGINT SUM DEFAULT "0" COMMENT "评论量",
    share_count BIGINT SUM DEFAULT "0" COMMENT "分享量"
)
ENGINE=OLAP
AGGREGATE KEY(stat_date, recipe_id, category_id)
PARTITION BY RANGE(stat_date) (
    PARTITION p20260101 VALUES [("2026-01-01"), ("2026-01-02"))
)
DISTRIBUTED BY HASH(recipe_id) BUCKETS 16
PROPERTIES (
    "replication_num" = "3"
);

-- 3.3 用户维度表 (Unique Model)
-- 用于存储最新的用户信息，支持 Update。
CREATE TABLE dim_user_profile (
    user_id BIGINT NOT NULL COMMENT "用户ID",
    nickname VARCHAR(64),
    level INT,
    reg_time DATETIME,
    last_login_time DATETIME REPLACE, -- 只有最新值有效
    total_spend DECIMAL(12,2) REPLACE -- 覆盖更新
)
ENGINE=OLAP
UNIQUE KEY(user_id)
DISTRIBUTED BY HASH(user_id) BUCKETS 10
PROPERTIES (
    "replication_num" = "3"
);

-- 3.4 财务汇总表 (Aggregate Model)
-- 用于财务看板，实时监控流水与对账。
CREATE TABLE dws_fin_daily_settlement (
    stat_date DATE NOT NULL COMMENT "统计日期",
    merchant_id BIGINT NOT NULL COMMENT "商户ID",
    biz_type INT NOT NULL COMMENT "业务类型: 1-充值, 2-消费, 5-佣金",
    
    total_amount DECIMAL(18,2) SUM DEFAULT "0" COMMENT "交易总额",
    trans_count BIGINT SUM DEFAULT "0" COMMENT "交易笔数"
)
ENGINE=OLAP
AGGREGATE KEY(stat_date, merchant_id, biz_type)
PARTITION BY RANGE(stat_date) (
    PARTITION p20260101 VALUES [("2026-01-01"), ("2026-01-02"))
)
DISTRIBUTED BY HASH(merchant_id) BUCKETS 10;
