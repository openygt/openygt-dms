/**
 * PDA Mock Server - Express HTTP Server
 *
 * 启动命令：node server.js
 * 默认端口：3456
 * 环境变量：PORT=3456 MOCK_LATENCY=200 MOCK_FAILURE_RATE=0.1
 *
 * 提供能力：
 * 1. 完整的 PDA API Mock（与详设文档对齐）
 * 2. Web UI 仪表盘（http://localhost:3456/）
 * 3. 测试控制 API（reset / offline / latency）
 */

const express = require('express')
const cors = require('cors')
const path = require('path')

const store = require('./store')
const authRoutes = require('./routes/auth')
const taskRoutes = require('./routes/tasks')
const deviceRoutes = require('./routes/devices')
const photoRoutes = require('./routes/photos')
const logRoutes = require('./routes/logs')
const controlRoutes = require('./routes/control')

const app = express()
const PORT = process.env.PORT || 3456

// 全局中间件
app.use(cors())
app.use(express.json({ limit: '10mb' }))
app.use(express.urlencoded({ extended: true }))

// 请求日志
app.use((req, res, next) => {
  const ts = new Date().toISOString().split('T')[1].split('.')[0]
  console.log(`[${ts}] ${req.method} ${req.path}`)
  next()
})

// 模拟延迟中间件
app.use(async (req, res, next) => {
  if (store.offlineMode() && req.path !== '/__control/online') {
    return res.status(503).json({ code: 503, msg: 'Service Unavailable (offline mode)' })
  }
  await store.simulateDelay()
  if (store.shouldFail() && req.path.startsWith('/api/')) {
    return res.status(500).json({ code: 500, msg: 'Random failure triggered by mock server' })
  }
  next()
})

// 统一响应格式中间件
app.use((req, res, next) => {
  res.jsonOk = (data, msg = 'success') => res.json({ code: 200, msg, data })
  res.jsonErr = (code, msg) => res.status(code >= 400 ? code : 200).json({ code, msg, data: null })
  next()
})

// 静态 Web UI
app.use('/', express.static(path.join(__dirname, 'public')))

// API 路由挂载
app.use('/api/v1/pda/auth', authRoutes)
app.use('/api/v1/pda/tasks', taskRoutes)
app.use('/api/v1/pda/devices', deviceRoutes)
app.use('/api/v1/pda/photos', photoRoutes)
app.use('/api/v1/pda/logs', logRoutes)
app.use('/__control', controlRoutes)

// 版本检查
app.post('/api/v1/pda/config/version', (req, res) => {
  res.jsonOk({ version: '1.0.0', forceUpdate: false })
})

// 心跳
app.post('/api/v1/pda/heartbeat', (req, res) => {
  res.jsonOk({ status: 'ok', timestamp: new Date().toISOString() })
})

// 健康检查
app.get('/health', (req, res) => {
  res.jsonOk({
    status: 'ok',
    offlineMode: store.offlineMode(),
    latencyMs: store.latencyMs || 0,
    failureRate: store.failureRate || 0,
    tasksCount: store.db.tasks.length,
    usersCount: store.db.users.length
  })
})

// 404
app.use((req, res) => {
  res.jsonErr(404, `API not found: ${req.method} ${req.path}`)
})

// 错误处理
app.use((err, req, res, next) => {
  console.error('[MockServer] Error:', err)
  res.jsonErr(500, err.message || 'Internal Server Error')
})

app.listen(PORT, () => {
  console.log(`
╔══════════════════════════════════════════════════════════╗
║                                                          ║
║     🧪 PDA Mock Server 已启动                             ║
║                                                          ║
║     API Base: http://localhost:${PORT}/api/v1/pda          ║
║     Web UI:   http://localhost:${PORT}/                    ║
║     Health:   http://localhost:${PORT}/health              ║
║     Control:  http://localhost:${PORT}/__control           ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
  `)
  console.log('可用控制接口：')
  console.log('  POST /__control/reset          - 重置所有数据')
  console.log('  POST /__control/offline        - 开启离线模式')
  console.log('  POST /__control/online         - 关闭离线模式')
  console.log('  POST /__control/latency        - 设置延迟 { ms: 2000 }')
  console.log('  POST /__control/failure-rate   - 设置失败率 { rate: 0.2 }')
  console.log('  GET  /__control/status         - 查看当前模拟状态')
  console.log('')
})
