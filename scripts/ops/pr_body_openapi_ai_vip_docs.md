# PR: 完善 AI/VIP/积分 OpenAPI 文档 + 收敛 VIP 返回契约

## 背景
- 当前项目已启用 springdoc OpenAPI，但部分 VO/Entity/Controller 缺少 `@Schema/@Parameter`，导出的接口文档字段含义不清晰。
- VIP 接口直接返回 `UserProfile`，存在“对外暴露过多字段”的风险，且文档展示噪声较大。

## 问题
- 用户资料/成长数据等 VO 没有 Schema 描述，Swagger UI 中字段语义不可读。
- 积分接口分页参数缺少说明；积分流水实体缺少 Schema。
- AI 对话接口的 `@RequestBody` 写法不统一，影响可维护性。

## 验收标准（来自 Issue）
- [ ] OpenAPI/Swagger UI 中关键字段含义可读（有 description/example，且无明显错配）
- [ ] 积分接口的分页参数含义明确，返回结构对外稳定
- [ ] VIP 返回契约收敛后无多余敏感字段暴露，且前端/调用方可正常工作
- [ ] AI Controller 的请求体注解与写法统一，可维护性提升

## 解决方案
- 为 `UserProfileVO/UserStatsVO/PointsRecord` 等补齐类级与字段级 `@Schema(description/example)`。
- 为积分接口补齐 `@Tag` 描述与分页参数 `@Parameter`。
- VIP 接口返回改为专用 `VipStatusVO`（只包含 userId/vipLevel/vipExpireTime），减少对外暴露面。
- 统一 AI Controller 的 RequestBody 注解写法。

## 变更点（关键文件）
- AI：
  - `src/main/java/com/zyyyys/culinarywhispers/module/ai/controller/AiController.java`
- VIP/积分：
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/controller/VipController.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/vo/VipStatusVO.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/controller/UserPointsController.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/entity/PointsRecord.java`
- 用户资料/成长 VO：
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/vo/UserProfileVO.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/user/vo/UserStatsVO.java`

## 测试与验证
- 自动化验证：
  - 前端：`npm --prefix fronted run build` 通过。
  - Go 边缘层：在 `go-edge` 目录执行 `go test ./...` 通过。
  - 后端：建议使用 CI/Docker（Java 21）验证 `./mvnw -DskipTests package`（或执行 `./mvnw test`）。
- 文档验收（建议最少 2 条）：
  - Swagger UI 中检查：PointsRecord/UserProfileVO/UserStatsVO/VipStatusVO 字段描述是否清晰
  - 导出 OpenAPI：确认 schema 定义与示例值符合预期（无明显空字段/错类型）

## 风险与回滚
- 风险：VIP 接口返回从 `UserProfile` 收敛为 `VipStatusVO`，若有未更新客户端可能出现字段缺失。
- 回滚：回退该 PR 或恢复接口返回 `UserProfile`。

## 关联 Issue
- Closes #(自动创建)

