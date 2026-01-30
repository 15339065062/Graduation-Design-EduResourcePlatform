# 压测与报告（本地）

## 目标
- 评论列表与发布接口满足基础并发
- 不出现 5xx、超时、明显的内存泄漏或文件句柄泄漏

## 测试对象
- `GET /api/comment/v2?resourceId=31&page=1&pageSize=20`
- `GET /api/comment/v2/{id}/replies?page=1&pageSize=20`
- `POST /api/comment/v2`（纯文本）
- `GET /api/comment/image/thumb/{fileName}`

## 环境
- Windows 本机 Tomcat + MySQL
- 前端 Vite 仅用于人工回归，不参与压测

## 方法（PowerShell 示例）
```powershell
$base = "http://localhost:8080/edu-resource-backend"
$n = 200
$sw = [Diagnostics.Stopwatch]::StartNew()
1..$n | ForEach-Object {
  Invoke-WebRequest -UseBasicParsing "$base/api/comment/v2?resourceId=31&page=1&pageSize=20" | Out-Null
}
$sw.Stop()
"Total=$n, ms=$($sw.ElapsedMilliseconds), avg_ms=$([math]::Round($sw.ElapsedMilliseconds/$n,2))"
```

## 结果（示例记录）
- GET 列表：平均延迟 < 30ms（本机，未并发）
- 备注：并发与更真实的链路（含网络抖动）需在上线前使用专业压测工具补充

## 风险与改进
- 若评论量大：可将 `COUNT(*)` 与图片查询做缓存/异步
- 若热点资源：可引入 CDN 缓存缩略图

