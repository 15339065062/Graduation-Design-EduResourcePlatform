# 发布与回滚

## 灰度发布（前端）
评论 V2 的灰度开关使用本地存储：
- 开启：`localStorage.commentV2 = '1'`
- 关闭：`localStorage.commentV2 = '0'`

建议的灰度流程：
1. 仅对内测账号开启（手动设置 localStorage）
2. 扩大到测试环境默认开启
3. 线上逐步放量（可扩展为后端下发配置）

## 回滚
1. 前端将 `localStorage.commentV2` 置为 `0`，立即回退旧评论 UI
2. 后端保留 v1 与 v2 接口并行，不需要回滚数据库

## 监控与告警
- 健康检查：`GET /api/health`
- 关键接口：
  - `GET /api/comment/v2`
  - `POST /api/comment/v2`
  - `GET /api/comment/image/*`
  - `GET /api/notification/unread-count`
- 建议告警条件：
  - 5xx 比例 > 1%
  - 关键接口 p95 > 800ms（本地可通过压测脚本采样）
  - DB 连接失败（health=500）

