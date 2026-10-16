# WorkHub Web UI（本次新增）

这次前端直接集成到原有 Spring Boot 项目，无需安装 Node.js、React 或 Vue。

## 本次实际修改

- `src/main/resources/static/index.html`：工作概览、工单列表、筛选、分页、详情以及操作窗口。
- `src/main/resources/static/login.html`：Spring Security Form Login 自定义登录页。
- `src/main/resources/static/assets/styles.css`：响应式页面样式（无运行时外部依赖）。
- `src/main/resources/static/assets/app.js`：通过 Fetch API 调用原有 REST API。
- `src/main/resources/static/favicon.svg`：WorkHub 图标。
- `src/main/java/com/example/workhub/controller/SessionController.java`：增加 `GET /api/me`，只返回当前登录用户的 `id` 与 `username`。
- `src/main/java/com/example/workhub/model/SessionUserResponse.java`：避免把包含密码哈希的 `User` 对象直接传给浏览器。
- `src/main/java/com/example/workhub/config/SecurityConfig.java`：指定自定义登录页面，沿用现有的 Form Login / Basic Auth。

没有修改 Ticket / Comment / Log 的原有 Controller、Service、Mapper 或数据库 schema。

## 如何运行（Windows PowerShell）

1. 打开工作目录，在项目 `workhub` 根目录运行命令。
2. 确认 MySQL 数据库 `workhub` 已存在，Redis 正在运行。不要重新导入 ZIP 中的旧 `sql/dump_workhub.sql`，它与现有账号密码结构不同。
3. 根据你已有的运行方式提供数据库密码环境变量（例如在当前 PowerShell 会话中设置 `$env:DB_PASSWORD`），不要把真实密码提交到 Git。
4. 运行：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. 在浏览器中打开 `http://localhost:8080/`，未登录时应跳转 `http://localhost:8080/login.html`。
6. 用数据库 `users` 表中已有的用户名，以及与你先前设置的 BCrypt 哈希匹配的密码登录。

## 支持的功能与实际 API

| 页面操作 | 对接 API | 特别说明 |
|---|---|---|
| 查看当前用户 | `GET /api/me` | 只返回 ID 和 username |
| 列表、筛选、分页、排序、概览统计 | `GET /tickets` | 使用返回的 `data/total/page/totalPages` |
| 查看详情 | `GET /tickets/{id}` | 需要 Redis 可以正常连接 |
| 新建工单 | `POST /tickets` | 现有接口返回更新行数，不返回新 ID |
| 更新描述、分类、标题及解决说明 | `PUT /tickets/{id}` | 使用现有 TicketUpdateRequest |
| 更新优先级 | `PUT /tickets/{id}/priority` | Body 为纯文本，例如 `HIGH` |
| 设置负责人 | `PUT /tickets/{id}/assignee` | Body 为 JSON 数字，例如 `2` |
| 更新状态 | `PUT /tickets/{id}/status` | 仅显示 OPEN → IN_PROGRESS → RESOLVED → CLOSED 的下一步 |
| 软删除 | `DELETE /tickets/{id}` | 需要确认 |
| 工单评论 | `GET/POST /tickets/{id}/comments` | 复用现有 CommentController |
| 状态历史 | `GET /tickets/{id}/logs` | 复用现有 TicketLogController |
| 退出 | `POST /logout` | 由 Spring Security 清除 Session |

说明：前端的“Assigned to Me”按当前登录用户的 ID 调用现有 `assignedTo` 参数；不是新的权限控制。总览统计的“已解决”只统计 `RESOLVED`，不包含 `CLOSED`。创建时默认使用后端 `tickets.status`、`priority` 默认值。

## 注意事项（后续安全收尾时必须处理）

- **原有项目已经禁用了 CSRF**。为了不破坏已有 PowerShell Basic Auth 测试，本次保持该设置；仅适用于本地演示。真实部署需恢复 CSRF 并在前端提交 CSRF Token。
- **原有创建工单和评论接口信任客户端传入的 `createdBy` / `authorId`**。本次前端自动从 `/api/me` 填入正确 ID，但浏览器本身不是安全边界。后续安全整理时需要让后端从认证主体中确定操作者、检查业务权限。
- 现有 Controller 暂无获取用户列表的接口，因此负责人目前填写数据库 User ID；不能凭空提供用户下拉名单。
- 原有服务对于数据库和 Redis 的真实环境仍是必要条件；此包在当前执行环境无法连接你的本地 MySQL/Redis，因此需要你在本机做端到端验收。
- ZIP 中的 `sql/dump_workhub.sql` 是旧历史备份；**不要用于覆盖正在使用的 workhub 数据库**。
- 此交付 ZIP 为干净项目源码，不包含原上传包中的 `.git` 历史目录。你可以把文件合并回自己的 Git 工作区继续开发。

## 快速验收建议

1. 打开 `/` → 跳转到登录页 → 登录后显示概览。
2. 点击 All Tickets → 测试 status / priority / sort / page。
3. 点击 Details → 确认 Comments、Status history。
4. 新建一条测试工单，打开详情 → 编辑标题 → 修改优先级 → 推进状态 → 评论。
5. 测试软删除、Assigned to Me、退出登录。

**后续不需要学习完整的前端框架。** 面试只需能解释 `fetch()` 如何请求 Spring Boot API、Session Cookie 如何识别当前用户，以及 JSON 如何转成列表。
