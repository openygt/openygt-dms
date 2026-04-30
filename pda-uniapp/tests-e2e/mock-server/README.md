# PDA Mock Server 使用手册

> **用途**：为 PDA 移动端提供完整的 Mock API 与状态机模拟，支撑 E2E 测试独立运行，无需启动真实 Java 后端。  
> **技术栈**：Node.js + Express + 内存状态机  
> **默认端口**：`3456`

---

## 一、快速启动

```bash
cd pda-uniapp/tests-e2e/mock-server
npm install        # 首次安装依赖
npm start          # 启动 Mock Server
```

启动后访问：
- **Web UI 仪表盘**：http://localhost:3456/
- **API 基地址**：http://localhost:3456/api/v1/pda
- **健康检查**：http://localhost:3456/health

---

## 二、内置测试数据

启动时自动加载 5 个测试任务，覆盖完整状态机：

| 任务ID | 条码 | 患者 | 状态 | 说明 |
|--------|------|------|------|------|
| 1001 | YP20250430001 | 张三 | PENDING | 待处理，可完整走完整条流程 |
| 1002 | YP20250430002 | 李四 | SOAKING | 泡药中，已确认开始泡药 |
| 1003 | YP20250430003 | 王五 | DECOCTING | 煎药中，已绑定煎药机 |
| 1004 | YP20250430004 | 赵六 | LABELING | 贴标中，已绑定包装机 |
| 1005 | YP20250430005 | 钱七 | COMPLETED | 已完成，用于重打印测试 |

员工账号：
- `EMP001` / `admin123` — 张三（煎药员）
- `EMP002` / `packer123` — 李四（包装员）
- `EMP003` / `inspect123` — 王五（质检员）
- `EMP004` / `leader123` — 赵六（班组长）

扫码条码：`EMP2024001` ~ `EMP2024004`

---

## 三、测试控制 API

### 3.1 重置数据

```bash
curl -X POST http://localhost:3456/__control/reset
```

所有数据恢复到初始状态（任务、日志、照片、签字等全部清空，任务状态还原）。

**E2E 测试最佳实践**：每个 `test.beforeEach` 中调用 reset，确保测试隔离。

### 3.2 离线模式

```bash
# 开启离线
curl -X POST http://localhost:3456/__control/offline

# 关闭离线
curl -X POST http://localhost:3456/__control/online
```

开启后，所有 `/api/v1/pda/*` 请求返回 `503 Service Unavailable`，用于测试 PDA 离线缓存逻辑。

### 3.3 网络延迟

```bash
curl -X POST http://localhost:3456/__control/latency \
  -H "Content-Type: application/json" \
  -d '{"ms": 2000}'
```

所有 API 响应延迟 2 秒，用于测试 Loading 态和超时处理。

### 3.4 随机失败率

```bash
curl -X POST http://localhost:3456/__control/failure-rate \
  -H "Content-Type: application/json" \
  -d '{"rate": 0.2}'
```

20% 概率返回 500 错误，用于测试重试和异常处理。

### 3.5 强制设置任务状态

```bash
curl -X POST http://localhost:3456/__control/tasks/1001/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DECOCTING"}'
```

快速将任务切换到任意状态，用于测试不同状态分支的 UI 表现。

---

## 四、状态机验证

Mock Server 内置完整状态机校验：

1. **不可跳步**：确认 `END_DECOCT` 时，如果当前状态是 `SOAKING`，返回 400
2. **设备绑定拦截**：`START_DECOCT`/`START_PACKAGE` 前必须先绑定对应类型设备
3. **拍照校验**：`INSPECT_PASS` 前必须上传过照片
4. **状态自动流转**：确认步骤后自动计算并更新任务状态

---

## 五、与 Playwright E2E 集成

### 5.1 自动启动

Playwright 的 `global-setup.js` / `global-teardown.js` 已配置为自动启动/关闭 Mock Server：

```bash
cd pda-uniapp/tests-e2e
npx playwright test
```

无需手动启动任何服务。

### 5.2 在测试中使用控制 API

```javascript
test.beforeEach(async ({ page }) => {
  // 每个用例前重置数据
  await page.request.post('http://localhost:3456/__control/reset')
})

test('离线同步测试', async ({ page }) => {
  // 开启离线模式
  await page.request.post('http://localhost:3456/__control/offline')
  
  // 执行 PDA 操作...
  
  // 恢复网络
  await page.request.post('http://localhost:3456/__control/online')
})
```

### 5.3 强制设置任务状态

```javascript
import { setTaskStatus } from '../fixtures/testData'

test('质检拍照强制流程', async ({ page }) => {
  await setTaskStatus('YP20250430004', 'LABELING')
  // 此时任务 1004 状态变为 LABELING，可测试质检通过前的强制拍照
})
```

---

## 六、Web UI 仪表盘

浏览器访问 http://localhost:3456/ 可查看：

- **实时统计**：任务数、工序记录、照片数、操作日志
- **任务状态列表**：当前所有任务的状态、工序、设备、操作人
- **设备列表**：可用设备及其状态
- **操作日志**：最近 10 条操作记录
- **一键控制**：重置 / 离线 / 延迟 / 失败率

---

## 七、目录结构

```
pda-uniapp/tests-e2e/
├── mock-server/              # Mock Server（可独立运行）
│   ├── package.json
│   ├── server.js             # Express 入口
│   ├── store.js              # 内存数据库 + 状态机引擎
│   ├── routes/
│   │   ├── auth.js           # 登录/扫码登录
│   │   ├── tasks.js          # 任务查询/确认/绑定/签字/重打印
│   │   ├── devices.js        # 设备列表
│   │   ├── photos.js         # 拍照上传
│   │   ├── logs.js           # 操作日志
│   │   └── control.js        # 测试控制（reset/offline/latency）
│   ├── public/
│   │   └── index.html        # Web UI 仪表盘
│   └── README.md             # 本文档
├── fixtures/
│   └── testData.js           # 测试数据工厂
├── specs/
│   └── login.spec.js         # 测试用例示例
├── playwright.config.js      # Playwright 配置
├── global-setup.js           # 自动启动 Mock Server
└── global-teardown.js        # 自动关闭 Mock Server
```

---

## 八、常见问题

**Q：Mock Server 端口被占用？**  
A：修改环境变量 `PORT=3457 npm start`，同时修改 `playwright.config.js` 和 `fixtures/testData.js` 中的端口号。

**Q：如何添加新的测试任务？**  
A：编辑 `mock-server/store.js` 中的 `INITIAL_TASKS` 数组，然后重启 Mock Server 或调用 `/__control/reset`。

**Q：H5 模式下如何模拟扫码？**  
A：H5 不支持 `uni.scanCode`，测试中用 `page.fill('[data-testid="scan-input"]', 'YP20250430001')` 模拟扫码结果输入。

**Q：如何验证状态机流转是否正确？**  
A：调用 `/__control/status` 查看所有任务当前状态，或调用 `/api/v1/pda/tasks/:barcode` 查看完整步骤时间线。
