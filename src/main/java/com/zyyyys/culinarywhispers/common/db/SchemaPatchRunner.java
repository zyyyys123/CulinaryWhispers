package com.zyyyys.culinarywhispers.common.db;

import com.zyyyys.culinarywhispers.common.db.mapper.SchemaPatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库结构补丁
 * 说明：用于开发/演示环境快速修复“表结构与实体不一致”导致的运行时报错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaPatchRunner implements ApplicationRunner {
    private final SchemaPatchMapper schemaPatchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        try {
            String schema = schemaPatchMapper.currentDatabase();
            if (schema == null || schema.isBlank()) {
                return;
            }
            ensureTotalSpendColumn(schema);
            ensureVipColumns(schema);
            ensureUserStatsPointsColumns(schema);
            ensureFollowTable(schema);
            ensurePointsRecordTable(schema);
            ensureNotifyTable(schema);
        } catch (Exception e) {
            // 启动补丁不应阻断应用启动；失败时保留日志便于排查
            log.error("Schema patch failed", e);
        }
    }

    private void ensureTotalSpendColumn(String schema) {
        Integer count = schemaPatchMapper.countColumn(schema, "t_usr_profile", "total_spend");
        if (count != null && count > 0) {
            return;
        }
        schemaPatchMapper.exec("ALTER TABLE t_usr_profile ADD COLUMN total_spend DECIMAL(12,2) DEFAULT 0.00 COMMENT '用户总消费金额' AFTER contact_email");
        log.info("Schema patch applied: added t_usr_profile.total_spend");
    }

    private void ensureVipColumns(String schema) {
        Integer vipLevelCol = schemaPatchMapper.countColumn(schema, "t_usr_profile", "vip_level");
        if (vipLevelCol == null || vipLevelCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_usr_profile ADD COLUMN vip_level INT DEFAULT 0 COMMENT 'VIP 等级' AFTER dietary_restrictions");
            log.info("Schema patch applied: added t_usr_profile.vip_level");
        }

        Integer vipExpireCol = schemaPatchMapper.countColumn(schema, "t_usr_profile", "vip_expire_time");
        if (vipExpireCol == null || vipExpireCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_usr_profile ADD COLUMN vip_expire_time DATETIME(3) DEFAULT NULL COMMENT 'VIP 到期时间' AFTER vip_level");
            log.info("Schema patch applied: added t_usr_profile.vip_expire_time");
        }
    }

    private void ensureUserStatsPointsColumns(String schema) {
        Integer pointsCol = schemaPatchMapper.countColumn(schema, "t_usr_stats", "points");
        if (pointsCol == null || pointsCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_usr_stats ADD COLUMN points INT DEFAULT 0 COMMENT '积分余额' AFTER last_publish_time");
            log.info("Schema patch applied: added t_usr_stats.points");
        }

        Integer continueCol = schemaPatchMapper.countColumn(schema, "t_usr_stats", "continue_sign_days");
        if (continueCol == null || continueCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_usr_stats ADD COLUMN continue_sign_days INT DEFAULT 0 COMMENT '连续签到天数' AFTER points");
            log.info("Schema patch applied: added t_usr_stats.continue_sign_days");
        }

        Integer lastSignCol = schemaPatchMapper.countColumn(schema, "t_usr_stats", "last_sign_date");
        if (lastSignCol == null || lastSignCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_usr_stats ADD COLUMN last_sign_date DATE DEFAULT NULL COMMENT '上次签到日期' AFTER continue_sign_days");
            log.info("Schema patch applied: added t_usr_stats.last_sign_date");
        }
    }

    private void ensureFollowTable(String schema) {
        String ddl = "CREATE TABLE IF NOT EXISTS t_soc_follow (\n"
                + "  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关注ID',\n"
                + "  follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者ID',\n"
                + "  following_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',\n"
                + "  status INT NOT NULL DEFAULT 1 COMMENT '关注状态: 1-关注,0-取消',\n"
                + "  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n"
                + "  gmt_modified DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),\n"
                + "  PRIMARY KEY (id),\n"
                + "  UNIQUE KEY uk_follower_following (follower_id, following_id),\n"
                + "  KEY idx_following (following_id),\n"
                + "  KEY idx_follower (follower_id)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表'";
        ensureTable(schema, "t_soc_follow", ddl);
        ensureFollowTableColumnsAndIndexes(schema);
    }

    private void ensurePointsRecordTable(String schema) {
        String ddl = "CREATE TABLE IF NOT EXISTS t_points_record (\n"
                + "  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',\n"
                + "  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',\n"
                + "  type INT NOT NULL COMMENT '类型: 1-签到,2-发布食谱,3-被点赞,10-兑换商品',\n"
                + "  amount INT NOT NULL COMMENT '变动数量(可为负)',\n"
                + "  description VARCHAR(255) DEFAULT NULL COMMENT '说明',\n"
                + "  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n"
                + "  PRIMARY KEY (id),\n"
                + "  KEY idx_user_time (user_id, gmt_create)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分流水表'";
        ensureTable(schema, "t_points_record", ddl);
    }

    private void ensureNotifyTable(String schema) {
        String ddl = "CREATE TABLE IF NOT EXISTS t_notify (\n"
                + "  id BIGINT UNSIGNED NOT NULL COMMENT '通知ID',\n"
                + "  from_user_id BIGINT UNSIGNED NOT NULL COMMENT '触发者ID',\n"
                + "  to_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',\n"
                + "  type INT NOT NULL COMMENT '类型: 1-评论回复,2-评论食谱,3-点赞食谱,4-收藏食谱,5-点赞评论',\n"
                + "  target_type INT NOT NULL COMMENT '目标类型: 1-食谱,2-评论',\n"
                + "  target_id BIGINT UNSIGNED NOT NULL COMMENT '目标ID',\n"
                + "  content VARCHAR(512) NOT NULL COMMENT '内容',\n"
                + "  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读,1-已读',\n"
                + "  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n"
                + "  PRIMARY KEY (id),\n"
                + "  KEY idx_to_time (to_user_id, gmt_create),\n"
                + "  KEY idx_read (to_user_id, is_read)\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知'";
        ensureTable(schema, "t_notify", ddl);
    }

    private void ensureFollowTableColumnsAndIndexes(String schema) {
        Integer tableCount = schemaPatchMapper.countTable(schema, "t_soc_follow");
        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer followerCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "follower_id");
        if (followerCol == null || followerCol == 0) {
            Integer userCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "user_id");
            if (userCol != null && userCol > 0) {
                schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN follower_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注者ID' AFTER id");
                schemaPatchMapper.exec("UPDATE t_soc_follow SET follower_id = user_id WHERE follower_id = 0");
            } else {
                schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN follower_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注者ID' AFTER id");
            }
            log.info("Schema patch applied: added t_soc_follow.follower_id");
        }

        Integer followingCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "following_id");
        if (followingCol == null || followingCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN following_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '被关注者ID'");
            log.info("Schema patch applied: added t_soc_follow.following_id");
        }

        Integer statusCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "status");
        if (statusCol == null || statusCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN status INT NOT NULL DEFAULT 1 COMMENT '关注状态: 1-关注,0-取消'");
            log.info("Schema patch applied: added t_soc_follow.status");
        }

        Integer remarkCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "remark_name");
        if (remarkCol == null || remarkCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN remark_name VARCHAR(64) DEFAULT NULL COMMENT '备注名' AFTER following_id");
            log.info("Schema patch applied: added t_soc_follow.remark_name");
        }

        Integer gmtModifiedCol = schemaPatchMapper.countColumn(schema, "t_soc_follow", "gmt_modified");
        if (gmtModifiedCol == null || gmtModifiedCol == 0) {
            schemaPatchMapper.exec("ALTER TABLE t_soc_follow ADD COLUMN gmt_modified DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)");
            log.info("Schema patch applied: added t_soc_follow.gmt_modified");
        }

        Integer uniqIdx = schemaPatchMapper.countIndex(schema, "t_soc_follow", "uk_follower_following");
        if (uniqIdx == null || uniqIdx == 0) {
            schemaPatchMapper.exec("CREATE UNIQUE INDEX uk_follower_following ON t_soc_follow (follower_id, following_id)");
            log.info("Schema patch applied: created index t_soc_follow.uk_follower_following");
        }

        Integer idxFollower = schemaPatchMapper.countIndex(schema, "t_soc_follow", "idx_follower");
        if (idxFollower == null || idxFollower == 0) {
            schemaPatchMapper.exec("CREATE INDEX idx_follower ON t_soc_follow (follower_id)");
            log.info("Schema patch applied: created index t_soc_follow.idx_follower");
        }

        Integer idxFollowing = schemaPatchMapper.countIndex(schema, "t_soc_follow", "idx_following");
        if (idxFollowing == null || idxFollowing == 0) {
            schemaPatchMapper.exec("CREATE INDEX idx_following ON t_soc_follow (following_id)");
            log.info("Schema patch applied: created index t_soc_follow.idx_following");
        }
    }

    private void ensureTable(String schema, String tableName, String ddl) {
        Integer count = schemaPatchMapper.countTable(schema, tableName);
        if (count != null && count > 0) {
            return;
        }
        schemaPatchMapper.exec(ddl);
        log.info("Schema patch applied: created {}", tableName);
    }
}
