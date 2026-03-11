package com.zyyyys.culinarywhispers.common.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * 数据库结构补丁
 * 说明：用于开发/演示环境快速修复“表结构与实体不一致”导致的运行时报错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaPatchRunner implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }
        try {
            ensureTotalSpendColumn();
            ensureVipColumns();
            ensureFollowTable();
            ensurePointsRecordTable();
            ensureNotifyTable();
        } catch (Exception e) {
            // 启动补丁不应阻断应用启动；失败时保留日志便于排查
            log.error("Schema patch failed", e);
        }
    }

    private void ensureTotalSpendColumn() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_usr_profile",
                "total_spend"
        );
        if (count != null && count > 0) {
            return;
        }
        // 修复字段缺失：避免 UserProfile 映射 select * 时出现 Unknown column
        jdbcTemplate.execute("ALTER TABLE t_usr_profile ADD COLUMN total_spend DECIMAL(12,2) DEFAULT 0.00 COMMENT '用户总消费金额' AFTER contact_email");
        log.info("Schema patch applied: added t_usr_profile.total_spend");
    }

    private void ensureVipColumns() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            return;
        }

        Integer vipLevelCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_usr_profile",
                "vip_level"
        );
        if (vipLevelCol == null || vipLevelCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_usr_profile ADD COLUMN vip_level INT DEFAULT 0 COMMENT 'VIP 等级' AFTER dietary_restrictions");
            log.info("Schema patch applied: added t_usr_profile.vip_level");
        }

        Integer vipExpireCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_usr_profile",
                "vip_expire_time"
        );
        if (vipExpireCol == null || vipExpireCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_usr_profile ADD COLUMN vip_expire_time DATETIME(3) DEFAULT NULL COMMENT 'VIP 到期时间' AFTER vip_level");
            log.info("Schema patch applied: added t_usr_profile.vip_expire_time");
        }
    }

    private void ensureFollowTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS t_soc_follow (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关注ID',
                  follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者ID',
                  following_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',
                  status INT NOT NULL DEFAULT 1 COMMENT '关注状态: 1-关注,0-取消',
                  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  gmt_modified DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_follower_following (follower_id, following_id),
                  KEY idx_following (following_id),
                  KEY idx_follower (follower_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表'
                """;
        ensureTable("t_soc_follow", ddl);
        ensureFollowTableColumnsAndIndexes();
    }

    private void ensurePointsRecordTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS t_points_record (
                  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
                  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                  type INT NOT NULL COMMENT '类型: 1-签到,2-发布食谱,3-被点赞,10-兑换商品',
                  amount INT NOT NULL COMMENT '变动数量(可为负)',
                  description VARCHAR(255) DEFAULT NULL COMMENT '说明',
                  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  KEY idx_user_time (user_id, gmt_create)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分流水表'
                """;
        ensureTable("t_points_record", ddl);
    }

    private void ensureNotifyTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS t_notify (
                  id BIGINT UNSIGNED NOT NULL COMMENT '通知ID',
                  from_user_id BIGINT UNSIGNED NOT NULL COMMENT '触发者ID',
                  to_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
                  type INT NOT NULL COMMENT '类型: 1-评论回复,2-评论食谱,3-点赞食谱,4-收藏食谱,5-点赞评论',
                  target_type INT NOT NULL COMMENT '目标类型: 1-食谱,2-评论',
                  target_id BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
                  content VARCHAR(512) NOT NULL COMMENT '内容',
                  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读,1-已读',
                  gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                  PRIMARY KEY (id),
                  KEY idx_to_time (to_user_id, gmt_create),
                  KEY idx_read (to_user_id, is_read)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知'
                """;
        ensureTable("t_notify", ddl);
    }

    private void ensureFollowTableColumnsAndIndexes() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            return;
        }
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow"
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer followerCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "follower_id"
        );
        if (followerCol == null || followerCol == 0) {
            Integer userCol = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class,
                    schema,
                    "t_soc_follow",
                    "user_id"
            );
            if (userCol != null && userCol > 0) {
                jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN follower_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注者ID' AFTER id");
                jdbcTemplate.execute("UPDATE t_soc_follow SET follower_id = user_id WHERE follower_id = 0");
            } else {
                jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN follower_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注者ID' AFTER id");
            }
            log.info("Schema patch applied: added t_soc_follow.follower_id");
        }

        Integer followingCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "following_id"
        );
        if (followingCol == null || followingCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN following_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '被关注者ID'");
            log.info("Schema patch applied: added t_soc_follow.following_id");
        }

        Integer statusCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "status"
        );
        if (statusCol == null || statusCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN status INT NOT NULL DEFAULT 1 COMMENT '关注状态: 1-关注,0-取消'");
            log.info("Schema patch applied: added t_soc_follow.status");
        }

        Integer remarkCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "remark_name"
        );
        if (remarkCol == null || remarkCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN remark_name VARCHAR(64) DEFAULT NULL COMMENT '备注名' AFTER following_id");
            log.info("Schema patch applied: added t_soc_follow.remark_name");
        }

        Integer gmtModifiedCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "gmt_modified"
        );
        if (gmtModifiedCol == null || gmtModifiedCol == 0) {
            jdbcTemplate.execute("ALTER TABLE t_soc_follow ADD COLUMN gmt_modified DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)");
            log.info("Schema patch applied: added t_soc_follow.gmt_modified");
        }

        Integer uniqIdx = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "uk_follower_following"
        );
        if (uniqIdx == null || uniqIdx == 0) {
            jdbcTemplate.execute("CREATE UNIQUE INDEX uk_follower_following ON t_soc_follow (follower_id, following_id)");
            log.info("Schema patch applied: created index t_soc_follow.uk_follower_following");
        }

        Integer idxFollower = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "idx_follower"
        );
        if (idxFollower == null || idxFollower == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_follower ON t_soc_follow (follower_id)");
            log.info("Schema patch applied: created index t_soc_follow.idx_follower");
        }

        Integer idxFollowing = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                schema,
                "t_soc_follow",
                "idx_following"
        );
        if (idxFollowing == null || idxFollowing == 0) {
            jdbcTemplate.execute("CREATE INDEX idx_following ON t_soc_follow (following_id)");
            log.info("Schema patch applied: created index t_soc_follow.idx_following");
        }
    }

    private void ensureTable(String tableName, String ddl) {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                Integer.class,
                schema,
                tableName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Schema patch applied: created {}", tableName);
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String product = metaData.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (Exception e) {
            return false;
        }
    }
}
