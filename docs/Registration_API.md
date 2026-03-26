# 报名管理模块 API 文档

## 1. 通用约定
- Base URL：`/api/v1`
- 认证方式：`Authorization: Bearer <token>`
- 成功响应（示例）：
  ```json
  { "code": 200, "message": "Success", "data": {} }
  ```
- 失败响应（示例）：
  ```json
  { "code": 400, "message": "No stock", "data": null }
  ```

## 2. 状态枚举
`PENDING / APPROVED / REJECTED / CANCELED / COMPLETED`

## 3. 学生端接口

### 3.1 一键报名
- URL：`POST /registrations`
- Body：
  ```json
  { "activityId": 123, "extraData": "{\"fieldA\":\"value\"}" }
  ```
- 返回：报名记录ID（Long）

### 3.2 取消报名
- URL：`POST /registrations/{id}/cancel`
- 返回：`null`

### 3.3 查询我的报名记录（分页）
- URL：`GET /registrations/my?page=1&size=10&status=PENDING`
- Query：
  - `page`：默认 1
  - `size`：默认 10（最大 200）
  - `status`：可选
- 返回：分页结构 `PageResponse<Registration>`

### 3.4 查询报名详情
- URL：`GET /registrations/{id}`
- 访问控制：仅本人或具备活动管理权限的用户可访问

## 4. 管理端接口
权限：`ROLE_SUPER_ADMIN / ROLE_COUNSELOR / ROLE_CLUB_OWNER`（其中 `CLUB_OWNER` 仅能管理自己创建的活动）

### 4.1 查看某活动报名名单（分页）
- URL：`GET /admin/registrations?activityId=123&page=1&size=10&status=PENDING`
- Query：
  - `activityId`：必填
  - `page/size/status`：同上

### 4.2 审核报名（通过/驳回）
- URL：`POST /admin/registrations/{id}/audit`
- Body：
  ```json
  { "status": "APPROVED", "reason": "ok" }
  ```
  ```json
  { "status": "REJECTED", "reason": "not eligible" }
  ```

### 4.3 数据面板：活动维度统计
- URL：`GET /admin/registrations/stats?activityId=123`
- 返回：
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": {
      "activityId": 123,
      "total": 10,
      "pending": 3,
      "approved": 6,
      "rejected": 1,
      "canceled": 0,
      "completed": 0
    }
  }
  ```

