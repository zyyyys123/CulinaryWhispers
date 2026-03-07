# 工程协作流程（Issue → PR/MR → Merge → Close）

## 目标
- 每个 Issue 必须对应一个 PR/MR
- PR 描述必须包含背景、问题、验收标准、解决方案、最终结果
- PR 合并后 Issue 必须关闭（优先用 “Closes #编号” 自动关闭）

## 分支与提交规范
- 分支命名：`issue/<issueNumber>-<short-slug>`
- PR 标题：与 Issue 标题一致或加前缀（如 `[Bug]` / `[Feature]`）
- PR 必须链接 Issue：在 PR 描述 “关联 Issue” 中填写 `Closes #<issueNumber>`

## 开发与验收流程
1. 从 `master` 拉新分支（按 Issue 编号）
2. 开发完成后，本地自测（前端 `npm run build`；后端按项目约定构建）
3. 推送分支并创建 PR（必须补全模板）
4. 通过 PR 合并（merge/squash/rebase 依项目策略）
5. 合并后确认 Issue 自动关闭；若未关闭则手动关闭并补充最终结果说明

## 自动化脚本
- 自动创建 PR、合并并关闭 Issue：`scripts/ops/github_pr_merge_close.ps1`
- Token：通过环境变量 `GITHUB_TOKEN`（或 `GH_TOKEN`）注入；不要写入仓库或聊天记录

