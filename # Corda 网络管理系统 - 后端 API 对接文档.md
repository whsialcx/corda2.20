# Corda 网络管理系统 - 后端 API 对接文档

本文档旨在为前端开发人员提供后端接口的详细说明，包含身份验证、节点生命周期管理以及 Corda 链上业务操作。

---

## 1. 基础信息
- **Base URL**: `http://<server-ip>:<port>`
- **Content-Type**: `application/json`
- **认证方式**: 登录接口返回 Token，后续请求请在 Header 中携带（具体视 Security 配置而定）。

---

## 2. 身份验证模块 (Auth)
负责用户与管理员的注册、登录及账号验证。

| 接口名称 | 请求路径 | 方法 | 参数 (Body/Query) | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **用户注册** | `/api/auth/user/register` | `POST` | `AuthRequest` (Body) | 注册普通用户角色 |
| **用户登录** | `/api/auth/user/login` | `POST` | `AuthRequest` (Body) | 返回 AuthResponse |
| **管理员注册** | `/api/auth/admin/register` | `POST` | `AuthRequest` (Body) | 注册管理员角色 |
| **管理员登录** | `/api/auth/admin/login` | `POST` | `AuthRequest` (Body) | 管理员身份校验 |
| **管理员激活** | `/api/auth/admin/verify` | `GET` | `token` (Query) | 验证管理员激活状态 |
| **用户名校验** | `/api/auth/check-username` | `GET` | `username` (Query) | 检查用户名是否重复 |

---

## 3. 节点管理模块 (Nodes)
用于管理 Corda 节点的申请、审批、部署以及物理状态控制。

### 3.1 申请与审批流
| 接口名称 | 请求路径 | 方法 | 参数 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **提交节点申请** | `/api/nodes/apply` | `POST` | `NodeApplyRequest` | 用户申请 X.500 名称 (O, L, C) |
| **待审批列表** | `/api/nodes/applications/pending` | `GET` | 无 | 获取所有状态为 PENDING 的申请 |
| **审批通过** | `/api/nodes/approve/{id}` | `POST` | `id` (Path) | 审批并物理生成节点 |
| **拒绝申请** | `/api/nodes/applications/reject` | `POST` | `{id, reason}` | 拒绝申请并触发邮件通知 |
| **我的申请记录** | `/api/nodes/applications/my` | `GET` | `username` (Query) | 用户查看自己的申请进度 |

### 3.2 节点控制与部署
| 接口名称 | 请求路径 | 方法 | 参数 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **节点列表** | `/api/nodes/list` | `GET` | 无 | 获取所有节点名及数据库详情 |
| **网络部署** | `/api/nodes/deploy` | `POST` | 无 | 执行 Gradle 部署任务 |
| **启动所有节点** | `/api/nodes/start-all` | `POST` | 无 | 批量启动所有配置的节点 |
| **单节点启动** | `/api/nodes/start` | `POST` | `{nodeName}` | 启动特定节点 |
| **单节点停止** | `/api/nodes/stop` | `POST` | `{nodeName}` | 停止特定节点 |
| **移除节点** | `/api/nodes/remove` | `POST` | `{nodeName}` | 从环境及数据库中删除节点 |
| **下载节点包** | `/api/nodes/download/{nodeName}` | `GET` | `nodeName` (Path) | 打包并下载节点物理文件 |
| **项目校验** | `/api/nodes/validate` | `GET` | 无 | 校验 Corda 项目路径配置是否正确 |

---

## 4. Corda 业务操作模块 (Corda)
通过指定的 `nodeName` 与具体的 Corda 节点进行交互。

### 4.1 节点信息查询
| 接口名称 | 请求路径 | 方法 | 说明 |
| :--- | :--- | :--- | :--- |
| **全节点体检** | `/api/corda/test-all-nodes` | `GET` | 测试所有节点的 HTTP 联通性 |
| **节点详情** | `/api/corda/{nodeName}/info` | `GET` | 获取身份、状态、Peers、时间等汇总信息 |
| **获取 Peer** | `/api/corda/{nodeName}/peers` | `GET` | 获取该节点可见的其他参与方 |
| **可用接收方** | `/api/corda/{nodeName}/receivers` | `GET` | 获取可作为交易接收方的节点 |
| **账本状态** | `/api/corda/{nodeName}/states` | `GET` | 查询该节点 Vault 中存储的所有状态 |

### 4.2 业务 Flow 接口
| 业务动作 | 接口路径 | 方法 | 重要参数 (Query/Path) |
| :--- | :--- | :--- | :--- |
| **创建 IOU** | `/api/corda/{nodeName}/create-iou` | `POST` | `iouValue`, `partyName` |
| **查询我的 IOU** | `/api/corda/{nodeName}/my-ious` | `GET` | 获取与当前节点相关的借据 |
| **记录温度** | `/api/corda/{nodeName}/record-temperature` | `POST` | `temperature`, `isCritical`, `receiver` |
| **查询温度记录** | `/api/corda/{nodeName}/temperatures` | `GET` | `onlyCritical` (可选) |


## 5. 实名认证模块 (KYC)(新增)
【新增】用于处理用户的个人/企业实名认证资料提交及状态查询。**所有涉及节点申请的操作，必须在实名状态为 VERIFIED 时方可进行。**

| 接口名称 | 请求路径 | 方法 | 参数 (Body/Query) | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **提交实名认证** | `/api/kyc/submit` | `POST` | `KycSubmitRequest` (Body)| 提交个人或企业实名资料。提交后状态变为 PENDING。 |
| **查询实名状态** | `/api/kyc/status` | `GET` | `username` (Query) | 查询当前用户的 KYC 状态 (UNVERIFIED, PENDING, VERIFIED) 及详细记录。 |

---

---

## 5. 主要数据对象 (DTO) 定义

### NodeApplyRequest
```json
{
  "o": "Organization Name",
  "l": "Locality/City",
  "c": "Country Code (e.g., CN)",
  "applicant": "username"
}
```
### AuthRequest (登录/注册)
```json
{
  "username": "admin",
  "password": "password123",
  "email": "admin@example.com"
}
```
### 错误处理说明
| HTTP 状态码 | 说明 |
|-------------|------|
| 200 OK | 逻辑执行成功，返回 success: true。 |
| 400 Bad Request | 参数错误或业务逻辑冲突（如组织名已存在）。 |
| 500 Internal Server Error | 后端脚本执行错误或数据库异常。 |
| 202 Accepted | 节点文件正在生成中，请稍后下载。 |


### 普通用户（User）使用的 API
| 接口路径 | 方法 | 说明 | 所在文件 |
| :--- | :--- | :--- | :--- |
| `/api/auth/user/login` | **POST** | **用户登录** | `index.html` |
| `/api/auth/user/register` | **POST** | **用户注册** | `user-dashboard.html` |
| `/api/nodes/applications/my?username={username}` | **GET** | **获取当前用户的所有节点申请记录** | `user-dashboard.html` |
| `/api/nodes/apply` | **POST** | **提交新节点申请（需提供组织、城市、国家）** | `user-dashboard.html` |
| `/api/corda/{nodeName}/status` | **GET** | **检查指定节点的 RPC 连通性（在线/离线）** | `user-dashboard.html` |
| `/api/nodes/download/{nodeName}` | **GET** | **下载已通过审批的节点安装包（ZIP 文件）** | `user-dashboard.html` |
| `/api/kyc/submit` | **POST** | **提交个人/企业实名认证** | `kyc.html` |（new）
| `/api/kyc/status` | **GET** | **查询当前用户的实名认证状态** | `kyc.html` |（new）
		

### 管理员（Admin）使用的 API
| 接口路径 | 方法 | 说明 | 所在文件 |
| :--- | :--- | :--- | :--- |
| `/api/auth/admin/login` | **POST** | **管理员登录** | `index.html` |
| `/api/auth/admin/register` | **POST** | **管理员注册** | `index.html` |
| `/api/nodes/validate` | **GET** | **验证 Corda 项目环境（检查 build.gradle、部署脚本等）** | `admin-dashboard.html` |
| `/api/nodes/deploy` | **POST** | **部署整个网络（执行 gradlew deployNodes）** | `admin-dashboard.html` |
| `/api/nodes/start-all` | **POST** | **启动所有已部署的节点** | `admin-dashboard.html` |
| `/api/nodes/applications/pending` | **GET** | **获取所有待审批的节点申请** | `admin-dashboard.html` |
| `/api/nodes/approve/{id}` | **POST** | **审批通过指定申请，并实际生成节点** | `admin-dashboard.html` |
| `/api/nodes/list` | **GET** | **获取所有已注册节点（包括名称、状态等信息）** | `admin-dashboard.html` |
| `/api/corda/test-all-nodes` | **GET** | **测试所有节点的连接状态（在线/离线）** | `admin-dashboard.html` |
| `/api/corda/{nodeName}/info` | **GET** | **获取指定节点的详细信息（身份、对等节点等）** | `admin-dashboard.html` |
| `/api/nodes/stop` | **POST** | **停止指定节点（需提供节点名称）** | `admin-dashboard.html` |