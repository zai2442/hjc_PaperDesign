# 用户与角色管理 API 文档

## 1. 认证接口 (Auth)

### 1.1 用户登录
- **URL**: `/api/v1/auth/login`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "username": "admin",
    "password": "123456"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```

### 1.2 用户注册
- **URL**: `/api/v1/auth/register`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "username": "student1",
    "password": "password123",
    "email": "student1@example.com",
    "phone": "13800138000"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": null
  }
  ```

## 2. 用户管理接口 (User)

### 2.1 获取用户列表
- **URL**: `/api/v1/users`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <token>`
- **Permission**: `ROLE_SUPER_ADMIN`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "phone": "13800138000",
        "roles": [{"roleCode": "ROLE_SUPER_ADMIN", "roleName": "超级管理员"}]
      }
    ]
  }
  ```

### 2.2 获取当前用户信息
- **URL**: `/api/v1/users/me`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <token>`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "phone": "13800138000",
      "status": 1
    }
  }
  ```

### 2.2 修改当前用户信息
- **URL**: `/api/v1/users/me`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
  ```json
  {
    "email": "new_admin@example.com",
    "phone": "13800138001"
  }
  ```

### 2.3 修改密码
- **URL**: `/api/v1/users/me/password`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
  ```json
  {
    "oldPassword": "123456",
    "newPassword": "newpassword123"
  }
  ```

## 3. 角色管理接口 (Role)

*(需要 `SUPER_ADMIN` 或 `ADMIN` 权限)*

### 3.1 获取角色列表
- **URL**: `/api/v1/roles`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <token>`

### 3.2 创建角色
- **URL**: `/api/v1/roles`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
  ```json
  {
    "roleName": "辅导员",
    "roleCode": "ROLE_COUNSELOR",
    "description": "负责审核活动"
  }
  ```

### 3.3 为用户分配角色
- **URL**: `/api/v1/roles/assign`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
  ```json
  {
    "userId": 2,
    "roleIds": [2, 3]
  }
  ```

### 3.4 移除用户的角色
- **URL**: `/api/v1/roles/remove`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
  ```json
  {
    "userId": 2,
    "roleIds": [3]
  }
  ```
