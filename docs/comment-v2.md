# 评论系统 V2（抖音式交互）

## 目标
- 展示用户头像与昵称
- 支持多级回复嵌套（任意评论可作为 parent）
- 评论支持上传与展示图片（最多 3 张）
- 分页加载、性能优化、安全过滤、权限控制、异常处理、可灰度与可回滚

## 数据模型
**comment**
- 新增：`reply_to_user_id`、`root_id`、`image_count`
- `parent_id`：指向被回复的直接父评论（支持多级）
- `root_id`：线程根评论 id（父评论为空时 root_id=id）

**comment_image**
- `comment_id` -> `comment(id)` 级联删除
- 存储：`file_name/file_path/mime_type/size/width/height/sort_order`

**analytics_event**
- 用于埋点统计（前端发送事件名与属性）

## 接口（后端）
**评论列表（父评论分页 + 预览回复）**
- `GET /api/comment/v2?resourceId={id}&page=1&pageSize=20`
- 返回：`PageResult` 风格 `{ list, total, page, pageSize, totalPages }`
- 每个父评论附带：
  - `replyCount`
  - `previewReplies`（默认前 2 条）
  - `images`（图片数组）

**回复列表（按 parent_id 分页）**
- `GET /api/comment/v2/{commentId}/replies?page=1&pageSize=20`

**发布评论/回复**
- `POST /api/comment/v2`
- 支持两种 Content-Type：
  - `multipart/form-data`（含图片）
  - `application/json`（纯文本）
- 参数：
  - `resourceId`（必填）
  - `content`（可空，但必须至少有 content 或 images）
  - `parentId`（可选，回复时传）
  - `replyToUserId`（可选）
  - `images`（最多 3 个文件）

**编辑评论**
- `PUT /api/comment/v2/{id}`（JSON：`{ content }`）

**删除评论**
- `DELETE /api/comment/v2/{id}`
- 权限：作者 / admin / 资源上传者

**评论图片访问**
- `GET /api/comment/image/{fileName}`
- `GET /api/comment/image/thumb/{fileName}`

**通知**
- `GET /api/notification/list?page=1&pageSize=20`
- `GET /api/notification/unread-count`
- `GET /api/notification/stream`（SSE，当前为短连接轮询式 SSE）
- `POST /api/notification/mark-read`（JSON：`{ id }`）

**监控**
- `GET /api/health`：包含 DB 连通性探测

## 安全与过滤
- 内容：敏感词过滤 + HTML 转义（防 XSS）
- 图片：仅允许 jpeg/png，限制单张 ≤ 5MB，最多 3 张
- 图片读取：文件名白名单校验，阻止目录穿越

## 性能优化点
- 父评论分页查询
- 回复数统计批量聚合（按 root_id）
- 预览回复使用窗口函数（按 parent_id 每个父评论取前 2）
- 图片批量查询（comment_id IN (...)）

## 存储与压缩策略
- 原图与缩略图统一转为 JPEG（更可控的体积），最大边长：
  - 原图：1080
  - 缩略图：360
- 存储目录：
  - `storage/uploads/comments`
  - `storage/uploads/comments_thumbs`

## 灰度与回滚
- 前端开关：`localStorage.commentV2`
  - `1`：强制启用
  - `0`：强制回退到旧评论 UI
- 回滚策略：
  - 前端直接切回旧 UI（不影响数据库）
  - 后端保留 v1 `/api/comment` 与 v2 `/api/comment/v2` 并行

## 已修复的历史问题
- comment 表缺失 `reply_to_user_id` 导致 SQL 报错
- 删除父评论级联删除回复时，`comment_count` 计数不准确（改为按表 COUNT 回写）

