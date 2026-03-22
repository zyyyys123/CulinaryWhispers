## 背景
- Issue #67 要求菜谱支持视频教程，并能在详情页直接播放或引导观看。

## 问题
- 现有视频链接多为 YouTube，多种 URL 形态（watch/shorts/live/embeds）未统一解析，导致详情页黑屏或空白。
- 数据库已有视频字段但缺少批量回填方案，内容覆盖不足。

## 验收标准（来自 Issue）
- [x] 发布菜谱支持填写视频链接（http/https）
- [x] 详情页可播放视频（支持 YouTube / 直链）
- [x] 无法播放时可点击跳转观看
- [x] 对现有菜谱回填部分有效视频链接

## 解决方案
- 前端：
  - 扩展 YouTube 链接解析，支持 watch/shorts/embed/live
  - 增加 iframe 超时/失败兜底，避免黑屏
- 数据：
  - 提供视频回填脚本：基于 mealdb 语料的 cover_url → video_url 映射回填

## 最终结果
- 详情页视频教程稳定展示，无法播放时有可点击跳转兜底。
- 已完成 1350 条菜谱视频回填（仅 YouTube 且与封面一致的关联来源）。

## 测试与验证
- [x] 前端：`npm --prefix fronted run build`
- [x] 接口验证：`GET /api/recipe/{id}` 返回 `info.videoUrl` 非空
- [x] 本地验证：详情页视频教程区域可见并可播放/跳转

## 影响范围与风险
- 影响范围：
  - 详情页视频播放区
  - 数据回填脚本（ops）
- 风险：
  - 部分网络环境无法访问 YouTube，会触发兜底跳转
- 回滚：
  - 回滚本 PR 提交即可恢复原逻辑；视频字段保持不变

## 关联 Issue
- Closes #67
