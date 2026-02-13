# AI 本地知识库（与 ai 模块同目录）

这里用于存放 AI 助手“必须熟知并严格遵循”的资料（支持 `.md` / `.txt`，可递归子目录）。

建议存放内容：
- 项目模块说明（前端页面、后端接口、数据表）
- 业务规则（积分规则、VIP 权益、通知类型含义）
- UI 文案规范与术语表
- 常见问题与标准回答（FAQ）

约定：
- `SYSTEM_PROMPT.md`：系统提示词（模型的“最高优先级规则”）
- 其他文档：作为知识库检索片段，按相关度注入上下文

默认读取路径：
- `src/main/java/com/zyyyys/culinarywhispers/module/ai/knowledge`

