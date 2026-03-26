# 发现与约定（活动管理模块）

## 代码现状（仓库基线）
- 后端：Spring Boot 2.7 + Java 17 + MyBatis-Plus 3.5 + Spring Security + JWT + Redis。
- 当前仅实现用户/角色/权限与登录；未实现活动相关表与接口。
- MyBatis-Plus：未配置分页拦截器；MapperScan 仅扫描 user 模块，需要扩展到活动模块。
- 安全：JWT 过滤器放行 `/api/v1/auth/**`；其余均需认证；方法级鉴权已开启。
- 前端：现有为 Vue + Vite 的管理端雏形，Axios 会自动带 `Authorization: Bearer <token>`。

## 需求要点（来自 docs/PRD.md + 用户补充）
- 活动接口：CRUD + 分页 + 搜索 + 状态过滤 + 权限校验 + 乐观锁。
- 工作流：草稿→审核→上线→下线；支持定时发布与撤回。
- 编辑能力：富文本正文、封面图、报名表单、库存/限购、渠道投放、白名单、A/B 文案版本；所有变更需要 diff 与回滚。
- 运营后台：React + Ant Design Pro 风格页面（列表/编辑/详情，批量/导出，分步表单，自动保存，实时预览）。
- H5：SSR 渲染、秒开优化、埋点上报。
- 数据与性能：MySQL + Redis，库存扣减原子性、防超卖；单测≥80%；接口自动化≥30；压测报告 QPS≥2000 且 P99≤300ms。
- 上线：checklist、灰度、回滚脚本、监控告警（成功率<99%报警）。

## 默认实现假设（可按需调整）
- “审核”角色：COUNSELOR/ADMIN/SUPER_ADMIN 可审核；club_owner 只能提交与编辑自己的活动草稿。
- “导出”：默认提供 CSV 导出（避免引入重量级 Excel 依赖）；如必须 xlsx 再增 POI。
- “富文本”：后端按 HTML/JSON 字符串存储；安全由前端编辑器与后端内容策略共同保证。
