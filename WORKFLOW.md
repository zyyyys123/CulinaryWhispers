# 工程协作流程（Issue → PR → Merge → Close）

## 目标
- 每个 Issue 必须对应一个 PR
- PR 描述必须包含背景、问题、验收标准、解决方案、最终结果、验证记录、影响范围与回滚
- PR 合并后 Issue 必须关闭（优先用 “Closes #编号” 自动关闭）

## 分支与提交规范
- 分支命名（建议二选一）：
  - `issue/<issueNumber>-<short-slug>`
  - `fix/<topic>` / `feat/<topic>`
- 提交信息（建议）：
  - `fix(scope): message`
  - `feat(scope): message`
  - `chore(scope): message`
- PR 标题：与 Issue 标题一致，或加前缀（如 `fix:` / `feat:` / `[Bug]` / `[Feature]`）
- PR 必须链接 Issue：在 PR 描述 “关联 Issue” 中填写 `Closes #<issueNumber>`

## 开发与验收流程
1. 从 `master` 拉新分支（按 Issue 编号）
2. 开发完成后，本地自测（按改动范围“命中即执行”）
   - 前端（命中 `fronted/`）：
     - `npm --prefix fronted ci`
     - `npm --prefix fronted run build`
   - Go（命中 `go-edge/`）：在 `go-edge` 下执行 `go test ./...`
   - Java（命中 `src/main/java/`）：需要 Java 21；执行 `./mvnw test` 或 `./mvnw -DskipTests package`
   - 若无法本地验证：必须提供 CI 验证结果（链接或结论摘要）
3. 推送分支并创建 PR（必须补全模板）
4. 在 PR 中记录验证证据（命令 + 结果 + 关键输出片段/截图）
5. 合并 PR（merge/squash/rebase 依项目策略）
6. 合并后确认 Issue 自动关闭；若未关闭则手动关闭并补充最终结果说明

## 自动化脚本
- 自动创建 PR、合并并关闭 Issue：`scripts/ops/github_pr_merge_close.ps1`
- Issue 批量创建：
  - `scripts/ops/create_github_issues_api.ps1`（推荐）
  - `scripts/ops/create_github_issues.ps1`（需要 gh CLI）
- Token：通过环境变量 `GITHUB_TOKEN`（或 `GH_TOKEN`）注入；不要写入仓库或聊天记录

## PR 模板
- 默认模板：`.github/PULL_REQUEST_TEMPLATE.md`
- 复杂改动建议在 `scripts/ops/pr_body_*.md` 中预写 PR 正文，再由脚本引用

