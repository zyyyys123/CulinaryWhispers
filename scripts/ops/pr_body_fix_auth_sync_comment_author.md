# PR: 修复鉴权状态不同步 + 评论作者信息返回 + 资源 URL 归一化

## 背景
- 2.0 任务清单中存在多项用户体验 Bug：未登录跳转异常、头像/封面裂图、评论作者信息不准确。

## 问题
- 401 时仅清理了 localStorage，Pinia token 仍保留导致路由判断“假登录”，出现跳转/回跳异常。
- 部分接口返回的资源地址可能是相对路径（如 `/uploads/**`），导致头像/封面在前端直接渲染时失败。
- 评论列表接口仅返回 Comment 实体，前端只能用 fallback 假用户映射，导致昵称/头像不真实。

## 验收标准（来自 Issue）
- [ ] 未登录访问受限页面会跳转到登录页；登录成功后可正常回跳
- [ ] token 失效/401 后登录态会被完全清理，不出现“假登录”
- [ ] 列表/详情的头像、封面等资源 URL 可稳定渲染（相对/绝对地址都兼容）
- [ ] 评论列表返回真实作者信息，前端不再依赖 fallback 假数据

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
- 自动化验证：
  - Go 边缘层：在 `go-edge` 目录执行 `go test ./...` 通过。
  - 前端：`npm --prefix fronted run build` 通过。
  - 后端：需要 Java 21；若本机不满足则由 CI（Java 21）验证 `./mvnw -DskipTests package`。
- 手工冒烟（建议最少 3 条）：
  - 未登录访问：进入社交/评论相关页面，确认跳转登录逻辑正确
  - 401 路径：使 token 过期或模拟 401，确认登录态被完全清理且不会出现假登录
  - 资源渲染：确认头像/封面等图片可正常显示（含相对路径场景）

## 影响范围与风险
- 影响范围：鉴权状态同步、评论列表返回结构、资源 URL 归一化逻辑
- 风险：若前后端对 CommentVO 字段约定不一致，可能导致评论作者展示异常
- 回滚：回退该 PR；若接口已被客户端依赖，需同时回退前端变更或提供兼容字段


## 关联 Issue
- Closes #27

