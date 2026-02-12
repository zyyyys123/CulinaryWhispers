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

