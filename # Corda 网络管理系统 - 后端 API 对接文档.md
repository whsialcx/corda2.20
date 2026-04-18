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


# Corda 节点部署与生命周期管理手册

本章节详细说明了如何在系统中从零开始部署、启动及管理 Corda 节点。所有的操作均通过后端 REST API 与底层的 PowerShell/Gradle 脚本交互完成。

---

## 1. 环境预检 (Environment Validation)
在执行任何部署操作前，必须确保后端服务器与 Corda 项目目录的连通性及脚本权限。

* **接口**: `GET /api/nodes/validate`
* **核心逻辑**: 检查 `build.gradle`、PowerShell 脚本（如 `add_node.ps1`）以及项目根目录是否存在。
* **预期结果**: 必须返回 `valid: true` 才能继续。

## 2. 节点注册 (Node Registration)
在物理部署前，需要先在网络配置和数据库中登记节点信息。

### 方案 A：用户申请流程（适用于普通用户）
1.  **实名校验**: 用户必须先完成实名认证（`KYCStatus` 为 `VERIFIED`）。
2.  **提交申请**: 调用 `POST /api/nodes/apply`，提交组织名称 (O)、所在城市 (L) 和国家 (C)。
3.  **管理员审批**: 管理员调用 `POST /api/nodes/approve/{id}`。系统会自动分配端口并调用 `doAddNode` 逻辑。

### 方案 B：直接添加（适用于管理员测试）
* **接口**: `POST /api/nodes/add`
* **参数**: 可选择手动指定 `p2pPort`、`rpcPort` 或开启 `autoPorts: true` 自动分配。

## 3. 网络物理部署 (Network Deployment)
节点信息登记后，物理文件尚未生成。此步骤将触发 Gradle 构建任务。

* **接口**: `POST /api/nodes/deploy`
* **底层行为**: 系统执行 `gradlew clean deployNodes`。
* **说明**: 该过程会生成证书、配置文件及节点运行所需的 Jar 包。文件将存放在 `build/nodes/` 目录下。

## 4. 节点生命周期管理 (Lifecycle Management)
部署完成后，可通过以下接口控制进程。

* **全量启动**: `POST /api/nodes/start-all` (执行 `runnodes` 脚本)。
* **单节点启动**: `POST /api/nodes/start` (需要传入 `nodeName`)。
* **单节点停止**: `POST /api/nodes/stop` (Ubuntu 下使用 `pkill`，Windows 下通过 `CommandLine` 匹配进程并终止)。

## 5. 节点分发 (Distribution)
如果需要将节点迁移至其他环境运行。

* **接口**: `GET /api/nodes/download/{nodeName}`
* **功能**: 将 `build/nodes/{nodeName}` 目录下的所有内容打包为 `.zip` 文件并下载。
* **注意**: 必须在“网络物理部署”成功后才能调用。

---

## 运维接口参考
| 功能 | 接口地址 | 请求方式 |
| :--- | :--- | :--- |
| 查询所有节点详情 | `/api/nodes/list` | GET |
| 查询待审批申请 | `/api/nodes/applications/pending` | GET |
| 移除节点 | `/api/nodes/remove` | POST |
| 测试节点连接状态 | `/api/corda/test-all-nodes` | GET |