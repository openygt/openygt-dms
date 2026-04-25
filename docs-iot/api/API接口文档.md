# OpenYGT 煎药室管理系统 - API 接口文档

| 文档版本 | V3.0 |
|---------|------|
| 编写日期 | 2026-04-25 |
| Base URL | http://localhost:8080/api/v1 |
| 变更 | V2.0→V3.0: 与 openygt-dms 9模块实际代码对齐 |

---

## 1. 通用规范

### 响应格式

```json
{"code": 200, "message": "success", "data": {}}
```

### 状态码

| 码 | 说明 |
|----|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 409 | 状态冲突 |
| 500 | 服务器错误 |

### 分页

请求: `?page=1&size=20`
响应: `{"records":[], "total":100, "size":20, "current":1, "pages":5}`

---

## 2. 基础数据 `/api/v1/md`

### 2.1 医院管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /hospitals | 创建医院 |
| PUT | /hospitals/{id} | 更新医院 |
| GET | /hospitals/{id} | 查询医院 |
| GET | /hospitals | 医院列表（分页） |
| DELETE | /hospitals/{id} | 删除医院 |

---

## 3. 系统管理 `/api/v1/sys`

### 3.1 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /users | 创建用户 |
| PUT | /users/{id} | 更新用户 |
| GET | /users/{id} | 查询用户 |
| GET | /users?keyword=xxx | 用户列表（分页+搜索） |
| DELETE | /users/{id} | 删除用户 |

---

## 4. 设备管理 `/api/v1/eq`

### 4.1 设备 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /devices | 注册设备 |
| PUT | /devices/{id} | 更新设备 |
| GET | /devices/{id} | 按ID查询 |
| GET | /devices/code/{deviceCode} | 按编码查询 |
| GET | /devices?keyword=xxx | 设备列表（分页+搜索） |
| DELETE | /devices/{id} | 删除设备 |

### 4.2 设备状态枚举

| 值 | 说明 |
|----|------|
| IDLE | 空闲 |
| BUSY | 工作中 |
| FAULT | 故障 |
| OFFLINE | 离线 |

---

## 5. 生产执行 `/api/v1/prod`

### 5.1 处方

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /prescriptions | 创建处方（自动建任务） |
| GET | /prescriptions/{id} | 查询处方 |
| GET | /prescriptions?hospitalId=&patientType= | 处方列表 |

### 5.2 任务状态机

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /tasks | 任务列表（?status=&deviceId=） |
| GET | /tasks/{taskId} | 任务详情 |
| POST | /tasks/{taskId}/bind | 绑定设备（旧接口） |
| POST | /tasks/{taskId}/soak/start | 开始泡药 |
| POST | /tasks/{taskId}/soak/end | 结束泡药 |
| POST | /tasks/{taskId}/decoct/start | 开始煎药（绑定煎药机） |
| POST | /tasks/{taskId}/decoct/end | 结束煎药 |
| POST | /tasks/{taskId}/pour/start | 开始出液 |
| POST | /tasks/{taskId}/pour/end | 结束出液 |
| POST | /tasks/{taskId}/wrap/start | 开始包装（绑定包装机） |
| POST | /tasks/{taskId}/wrap/end | 结束包装 |
| POST | /tasks/{taskId}/label/confirm | 确认贴标 |
| POST | /tasks/{taskId}/quality | 质检 |
| POST | /tasks/{taskId}/handover | 交接 |
| POST | /tasks/{taskId}/force | 强制操作 |

### 5.3 任务辅助

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /tasks/{taskId}/steps | 工序记录 |
| GET | /tasks/{taskId}/handover-details | 交接明细 |
| GET | /tasks/{taskId}/history | 状态变更历史 |
| GET | /tasks/print-queue | 待打印队列 |
| POST | /tasks/{taskId}/print | 提交打印 |
| POST | /tasks/{taskId}/print/retry | 重试打印 |
| POST | /tasks/clear | 清空任务（测试用） |

### 5.4 请求体示例

**开始煎药:**
```json
{"deviceCode": "DECOCT-001", "operatorId": "OP001"}
```

**质检:**
```json
{"result": "通过", "operatorId": "OP001", "remark": "正常"}
```
result 可选: 通过, 让步放行, 返工, 报废

**交接:**
```json
{"bagCount": 7, "handoverType": "患者自提", "handoverUser": "OP001", "remark": "", "isFinal": true}
```

---

## 6. 质量管理 `/api/v1/qt`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /inspect | 执行质检 |
| GET | /inspection/{taskId} | 查询质检记录 |

---

## 7. 打印中心 `/api/v1/prt`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /tasks/{taskId}/submit | 提交打印任务 |
| POST | /tasks/{taskId}/retry | 重试打印 |
| GET | /queue | 打印队列 |

---

## 8. MQTT 接口

| Topic | 方向 | 说明 |
|-------|------|------|
| device/{deviceId}/status | 上报 | 状态+温度 |
| device/{deviceId}/heartbeat | 上报 | 心跳 |
| device/{deviceId}/command | 下发 | 指令 |
| device/{deviceId}/response | 上报 | 指令响应 |
| device/{deviceId}/fault | 上报 | 故障 |

---

## 9. 运营分析 `/api/v1/ops`（预留）

待 dms-analytics 模块开发后补充。
