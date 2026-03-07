# 配置清单（后端）

本项目后端支持通过 `application.yml` 配置，并允许使用环境变量覆盖（`${ENV_VAR:default}`）。

## 核心环境变量

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---:|---|
| 服务端口 | `CW_SERVER_PORT` | `8080` | Spring Boot 端口 |
| 数据库 URL | `CW_DB_URL` | `jdbc:mysql://localhost:3306/culinary_user?...` | MySQL 连接串 |
| 数据库用户名 | `CW_DB_USERNAME` | `root` | 生产环境建议单独账号 |
| 数据库密码 | `CW_DB_PASSWORD` | 空 | 生产环境必填（不要写进仓库） |
| Redis Host | `CW_REDIS_HOST` | `localhost` | Redis 连接 |
| Redis Port | `CW_REDIS_PORT` | `6379` |  |
| Redis 密码 | `CW_REDIS_PASSWORD` | 空 |  |
| Redis DB | `CW_REDIS_DATABASE` | `0` |  |
| Redis 超时(ms) | `CW_REDIS_TIMEOUT_MS` | `3000` |  |
| JWT 密钥 | `CW_JWT_SECRET` | 空 | 生产环境必填并通过环境变量注入 |
| JWT 过期(ms) | `CW_JWT_EXPIRATION_MS` | `86400000` |  |
| 日志目录 | `CW_LOG_DIR` | `logs` | logback 输出目录 |
| MyBatis 日志实现 | `CW_MYBATIS_LOG_IMPL` | `StdOutImpl` | 生产环境建议关闭 stdout |

## 功能开关

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---:|---|
| Token Store | `CW_TOKEN_STORE` | `redis` | `redis` / `memory` |
| 搜索实现 | `CW_SEARCH_TYPE` | `db` | `db` / `elasticsearch` |

## 上传配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---:|---|
| 上传目录 | `CW_UPLOAD_DIR` | `uploads` | 存储路径（相对/绝对均可） |
| 单文件大小(MB) | `CW_UPLOAD_MAX_SIZE_MB` | `3` | 超出返回 400 |
| 静态资源缓存(s) | `CW_UPLOAD_CACHE_SECONDS` | `3600` | `/api/uploads/**` 缓存 |
| 允许类型 | `CW_UPLOAD_ALLOWED_CONTENT_TYPES` | `image/jpeg,image/png,image/webp,image/gif` | 逗号分隔 |
| 公开 URL 前缀 | `CW_UPLOAD_PUBLIC_URL_PREFIX` | `/api/uploads/` | 返回给前端的 URL 前缀 |

## Go 网关（如启用）

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---:|---|
| 网关端口 | `CW_GATEWAY_PORT` | `:8081` |  |
| 目标后端 | `CW_GATEWAY_TARGET_URL` | `http://app:8080` |  |
| JWT 密钥 | `CW_JWT_SECRET` | 空 | 需与后端一致 |

