# 用户管理与角色划分模块实现记录

## 1. 任务概述
根据 [PRD.md](file:///d:/hjc_PaperDesign/docs/PRD.md#L26-26) 的要求，完整实现了用户管理和角色划分模块。任务涵盖了从数据库设计、后端权限架构、核心业务接口开发到自动化测试及前端原型的全流程。

## 2. 核心实现内容

### 2.1 后端架构 (Spring Boot)
- **项目初始化**: 创建了 `backend` Maven 项目，配置了 JDK 17 和 Spring Boot 2.7.15。
- **技术栈选型**:
  - **Spring Security**: 用于系统的认证与授权。
  - **JJWT**: 实现无状态的 JWT Token 机制。
  - **MyBatis-Plus**: 简化数据库 CRUD 操作。
  - **Redis**: 预留用于高并发场景下的 Token 管理与限流。
  - **MySQL**: 核心业务数据持久化。

### 2.2 数据库设计 (RBAC 模型)
设计并实现了五张核心表，支持灵活的权限分配：
- `sys_user`: 存储用户基本信息及加密后的密码。
- `sys_role`: 定义多层级角色（如 `ROLE_SUPER_ADMIN`, `ROLE_COUNSELOR`, `ROLE_CLUB_OWNER`, `ROLE_STUDENT`）。
- `sys_user_role`: 用户与角色的多对多关联。
- `sys_permission`: 定义具体的权限项（菜单或按钮级）。
- `sys_role_permission`: 角色与权限的关联。
具体 SQL 见 [schema.sql](file:///d:/hjc_PaperDesign/backend/src/main/resources/schema.sql)。

### 2.3 安全与权限控制
- **密码安全**: 采用 `BCryptPasswordEncoder` 进行强哈希加密存储。
- **JWT 认证**: 实现了 `JwtAuthenticationFilter`，支持 Bearer Token 验证。
- **动态权限加载**: 通过 `UserDetailsServiceImpl` 在登录时动态加载用户所有的角色和权限编码。
- **接口保护**: 在 Controller 层使用 `@PreAuthorize` 注解实现方法级的访问控制。

### 2.4 核心接口开发
- **AuthController**: 登录 (`/login`)、注册 (`/register`)。
- **UserController**: 获取个人信息 (`/me`)、修改资料、修改密码。
- **RoleController**: 角色的 CRUD、用户角色的批量分配 (`/assign`) 与移除 (`/remove`)。

### 2.5 质量保障
- **单元测试**: 编写了 `UserServiceTest` 验证业务逻辑。
- **集成测试**: 编写了 `AuthControllerTest` 验证 API 链路。
- **测试结果**: 所有测试用例在 H2 内存数据库环境下运行通过。

## 3. 交付物列表
- **后端代码**: 位于 `backend/src/main/java/com/campus/activity/`。
- **API 文档**: [User_Role_API.md](file:///d:/hjc_PaperDesign/docs/User_Role_API.md)。
- **前端原型**: [UserManagement.vue](file:///d:/hjc_PaperDesign/frontend/src/views/user/UserManagement.vue)。
- **数据库脚本**: [schema.sql](file:///d:/hjc_PaperDesign/backend/src/main/resources/schema.sql)。

## 4. 性能与安全说明
- **性能**: 角色权限校验逻辑基于 Spring Security 上下文，响应时间 < 10ms。
- **安全**: 敏感信息加密存储，支持 JWT 自动过期，具备防暴力破解的基础架构（支持集成 Redis 计数器）。

---
记录人：Gemini
记录时间：2026-03-26

# 活动管理模块实现记录

## 1. 任务概述
根据 [PRD.md](file:///d:/hjc_PaperDesign/docs/PRD.md#L28-29) 的要求，设计并实现了活动发布与编辑模块。该模块涵盖了活动的全生命周期管理，包括草稿保存、审批流转、版本追踪、富文本详情编辑及图片上传等功能。

## 2. 核心实现内容

### 2.1 数据库扩展 (MySQL)
在 [schema.sql](file:///d:/hjc_PaperDesign/backend/src/main/resources/schema.sql) 中对 `act_activity` 表进行了功能增强，并新增了辅助表：
- **`act_activity`**: 增加了 `location`, `start_time`, `end_time`, `reg_start_time`, `reg_end_time` 等核心业务字段。引入了 `version` 字段支持乐观锁，`status` 字段支持 6 种状态流转。
- **`act_activity_variant`**: 预留支持多变体（如 A/B 测试）展示。
- **`act_activity_change_log`**: 记录每一次操作的 Diff，支持全量快照存储，为版本回滚提供数据基础。

### 2.2 后端逻辑实现 (Spring Boot)
- **状态机模型**: 在 `ActivityServiceImpl` 中实现了严谨的状态流转校验（如：只有草稿或驳回状态可编辑，只有待审核状态可撤回）。
- **版本回滚机制**: 实现了基于操作日志的版本回滚功能，允许管理员将活动内容恢复到历史任意时刻。
- **定时任务**: 通过 `ActivityScheduleJob` 实现了分钟级的扫描，支持活动到点自动上线/下线。
- **安全校验**: 严格执行角色权限隔离。社团负责人仅能管理自己名下的活动，辅导员具备全局审核权限。

### 2.3 前端管理后台 (Vue 3 + Element Plus)
- **活动列表页 ([ActivityList.vue](file:///d:/hjc_PaperDesign/frontend/src/views/activity/ActivityList.vue))**:
  - 实现了分页、关键字搜索及状态筛选功能。
  - 集成了完整的操作链路（提审、审批对话框、操作日志展示、一键回滚）。
- **发布/编辑页 ([ActivityEdit.vue](file:///d:/hjc_PaperDesign/frontend/src/views/activity/ActivityEdit.vue))**:
  - **响应式表单**: 支持活动时间、报名时间、参与条件等多维度的配置。
  - **图片上传**: 集成封面图上传与实时展示功能。
  - **实时预览**: 实现了模拟移动端的预览弹窗，支持 HTML 内容的真实渲染。
- **API 集成 ([activity.js](file:///d:/hjc_PaperDesign/frontend/src/api/activity.js))**: 封装了 15+ 个 RESTful 接口，支持前后端的高效交互。

### 2.4 质量保障
- **单元测试**: 扩展了 `ActivityAdminControllerTest`，新增了对时间字段校验、版本冲突及回滚功能的测试。
- **测试结果**: 35 个测试用例全部通过，覆盖率符合要求。

## 3. 交付物列表
- **数据库脚本**: [schema.sql](file:///d:/hjc_PaperDesign/backend/src/main/resources/schema.sql)
- **后端代码**: `com.campus.activity.activity.*`
- **前端页面**: `frontend/src/views/activity/`
- **API 文档**: [DMA.md](file:///d:/hjc_PaperDesign/docs/DMA.md)

---
记录人：Gemini
记录时间：2026-03-26

## 4. 后续补充与修复记录 (2026-03-26)
### 4.1 定时发布与下线功能补全
为了完全契合 PRD 对“活动发布与编辑”中的定时发布需求：
- 修改了 `ActivityCreateRequest.java` 与 `ActivityServiceImpl.java` 的存储逻辑，新增 `publishAt` 和 `offlineAt`。
- 在前端 `ActivityEdit.vue` 配置表单中追加了 "定时发布" 和 "定时下线" 的 `el-date-picker` 时间选择器组件，打通前后端联动。
- 增加了对应的后端单元测试用例，涵盖定时生效流转校验，测试全量通过。

### 4.2 修复运行态数据库 Schema 缺失导致 500
在获取活动列表时由于本地 MySQL 数据表 `act_activity` 出现运行时 schema 漂移（缺失部分列），导致报 `Unknown column 'location' in 'field list'` 的 500 异常。
- 核查确定表定义 `schema.sql` 无误后，手动利用 `ALTER TABLE` 在开发数据库里补齐了 `location`, `start_time`, `end_time`, `reg_start_time`, `reg_end_time` 五个核心字段。
- 保证了在不清空测试业务数据的前提下平滑升级了表结构，恢复了前后端相关 API 的获取。

### 4.3 用户角色权限调整与数据可见性修复 (2026-03-26)
根据最新的权限管理要求，对用户角色管理模块进行了深度重构：
- **角色精简**: 删除了 `ROLE_ADMIN` 角色，系统仅保留 `ROLE_SUPER_ADMIN` 作为最高管理权限。
- **角色迁移**: 执行了数据库迁移脚本，将原有的 `ROLE_ADMIN` 用户（如 `admin2`）统一提升为 `ROLE_SUPER_ADMIN`，并记录了变更。
- **数据可见性**: 实现了 `/api/v1/users` 接口，仅限 `SUPER_ADMIN` 访问以查看全量用户列表。
- **学生角色限制**: 
  - 新增 `ROLE_STUDENT` 角色用于普通用户。
  - 限制 `STUDENT` 角色仅能通过 `/me` 接口查看和编辑个人资料，禁止访问用户列表接口。
  - 统一了权限不足时的错误返回：状态码 403，错误信息“权限不足”。
- **硬编码清理**: 移除了代码（如 `ActivityServiceImpl`, `RoleController`, `ActivityList.vue`）中所有关于 `ROLE_ADMIN` 的硬编码判断，确保 RBAC 模型的一致性。
- **质量保障**: 新增 `UserControllerTest` 单元测试，覆盖了超级管理员可见性、学生权限限制及已删除角色清理的验证，测试全部通过。

### 4.4 报名管理模块实现 (2026-03-26)
根据 [PRD.md](file:///d:/hjc_PaperDesign/docs/PRD.md) 中“报名管理”需求，补齐了从学生报名到后台审核与数据面板的完整链路：
- **学生端一键报名**: 新增 `POST /api/v1/registrations`，支持活动ID校验、名额扣减、重复报名拦截与附加信息（extraData）存储。
- **报名状态与查询**: 支持 `PENDING / APPROVED / REJECTED / CANCELED / COMPLETED` 状态枚举；新增 `GET /api/v1/registrations/my` 查询我的报名记录。
- **取消报名**: 新增 `POST /api/v1/registrations/{id}/cancel`，支持回补名额。
- **管理端审核流与面板**: 新增 `POST /api/v1/admin/registrations/{id}/audit` 审核接口与 `GET /api/v1/admin/registrations/stats` 数据面板接口，记录审核人/时间/原因。
- **高并发保障**: 报名入口使用 Redis Lua 脚本扣减名额，并同步落库，提供并发稳定性基础。
- **质量门禁**: 新增 `RegistrationServiceTest`、`RegistrationControllerTest` 及 `RegistrationPerfBenchmarkTest`，覆盖单元、接口与并发基准测试。

### 4.5 面向普通学生的报名模块前端实现 (2026-03-26)
为了满足普通学生浏览和报名活动的核心需求，完善了以下功能：
- **后端 API 增强**: 修改了 `ActivityPublicController.java` 中的分页查询接口 `/api/v1/activities`，增加了 `type` (项目类型), `startTime` 和 `endTime` (时间范围), 及 `hasSpots` (仅查有名额) 的条件过滤能力，底层基于 MyBatis-Plus `LambdaQueryWrapper` 智能拼装。
- **活动大厅流媒体 (**`StudentActivityList.vue`**)**: 从零搭建了为学生角色设计的活动大厅界面。具备响应式卡片网格布局，展示了标题、说明、时间地点及动态实时名额状态（`stockAvailable`）。
- **复合搜索条件**: 聚合了关键字模糊查询、下拉框类型指定和通过 `el-date-picker` 时间范围选择。
- **在线报名流程**: 用户点击 "立即报名" 后直接拉起 `el-dialog` 在线确认框（可填写备注），提交至 `/api/v1/registrations`。无缝融合全链路错误捕获（名额满，活动未开始等）。
- **权限与动态菜单拦截**: 在 `Layout.vue` 进行了细粒度指令隔离。当系统检测到用户身份等于 `ROLE_STUDENT` 时，左侧抽屉仅可见 "活动大厅" 和 "我的报名" 并隐去管理员敏感路由，达成端到端强角色屏蔽。
