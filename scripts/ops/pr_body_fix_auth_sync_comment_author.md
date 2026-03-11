# MR/PR: 修复鉴权状态不同步 + 评论作者信息返回 + 资源 URL 归一化

## 背景
- 2.0 任务清单中存在多项用户体验 Bug：未登录跳转异常、头像/封面裂图、评论作者信息不准确。

## 问题
- 401 时仅清理了 localStorage，Pinia token 仍保留导致路由判断“假登录”，出现跳转/回跳异常。
- 部分接口返回的资源地址可能是相对路径（如 `/uploads/**`），导致头像/封面在前端直接渲染时失败。
- 评论列表接口仅返回 Comment 实体，前端只能用 fallback 假用户映射，导致昵称/头像不真实。

## 解决方案
- 前端在路由守卫中增加 token 一致性校验：若 Pinia token 存在但 localStorage 不存在则清理登录态。
- Axios 响应拦截器遇到 401 时广播清理事件，主入口统一监听并清空 Pinia（避免状态残留）。
- 统一对社交侧列表的 coverUrl/authorAvatar 做 `normalizeAssetUrl`。
- 后端 `/api/social/comment/list` 返回 `CommentVO`，包含 `author`（来自 `t_usr_base` 与 `t_usr_profile` 的合并信息），前端去掉 fallbackUser 逻辑，改用后端 author。

## 变更点（关键文件）
- 前端鉴权一致性：
  - `fronted/src/router/index.ts`
  - `fronted/src/api/http.ts`
  - `fronted/src/main.ts`
- 前端资源 URL 归一化：
  - `fronted/src/api/social.ts`
  - `fronted/src/utils/assetUrl.ts`
- 评论作者信息：
  - `src/main/java/com/zyyyys/culinarywhispers/module/social/vo/CommentVO.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/social/service/CommentService.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/social/service/impl/CommentServiceImpl.java`
  - `src/main/java/com/zyyyys/culinarywhispers/module/social/controller/SocialController.java`
  - `src/test/java/com/zyyyys/culinarywhispers/module/social/controller/SocialControllerTest.java`

## 测试与验证
- Go 边缘层：`go test ./...` 通过（在 `go-edge` 目录执行）。
- 前端：`npm --prefix fronted run build` 通过。
- 后端：需要 JDK21/CI（本机默认 JDK 为 1.8，无法本地编译 Spring Boot 3.x）。

## 关联 Issue
- Closes #27
- Closes #28

