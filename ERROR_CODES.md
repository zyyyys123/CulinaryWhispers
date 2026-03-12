## 统一错误响应

错误响应字段（JSON）：
- code：业务错误码（数字）
- message：可读错误信息
- requestId：请求标识（优先来自请求头 X-Request-Id；否则后端生成）
- traceId：链路标识（优先来自请求头 X-Trace-Id；否则与 requestId 一致）
- timestamp：服务端时间戳（毫秒）
- path：请求路径

响应头：
- X-Request-Id：同 requestId
- X-Trace-Id：同 traceId

## HTTP Status 语义
- 400：参数错误/请求体解析失败/类型不匹配
- 401：未登录/Token 失效
- 403：无权限/禁止访问
- 404：资源不存在
- 405：方法不支持
- 429：限流（预留）
- 500：系统异常

## 错误码表（现有）

通用：
- 0：SUCCESS
- 400：VALIDATE_FAILED
- 401：UNAUTHORIZED
- 403：FORBIDDEN
- 404：DATA_NOT_FOUND
- 500：ERROR

用户（USER）：
- 1001：USER_NOT_EXIST
- 1002：PASSWORD_ERROR
- 1003：USER_EXIST

## 新增错误码规范（建议）

约定：
- code 以业务域分段，便于检索与治理
- HTTP Status 与 code 分离：HTTP 只表达语义，code 用于前端分支与定位

建议分段（示例）：
- 1xxx：USER
- 2xxx：RECIPE
- 3xxx：SOCIAL
- 4xxx：NOTIFY
- 5xxx：POINTS
- 6xxx：VIP
- 7xxx：COMMERCE
- 8xxx：AI

建议每个模块至少预留：
- *_NOT_FOUND：资源不存在（HTTP 404）
- *_CONFLICT：重复操作/状态冲突（HTTP 409）
- *_FORBIDDEN：无权限（HTTP 403）
