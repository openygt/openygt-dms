/**
 * Playwright Global Setup - 启动 PDA Mock Server
 */

const { spawn } = require('child_process')
const path = require('path')

module.exports = async function globalSetup() {
  // 如果 Mock Server 已经在运行，跳过启动
  try {
    const health = await fetch('http://localhost:3456/health')
    if (health.ok) {
      console.log('[E2E] Mock Server 已在运行，跳过启动')
      return
    }
  } catch (e) {
    // 未运行，继续启动
  }

  console.log('[E2E] 启动 PDA Mock Server...')

  const mockServerPath = path.join(__dirname, 'mock-server', 'server.js')
  const child = spawn('node', [mockServerPath], {
    env: { ...process.env, PORT: '3456' },
    stdio: 'pipe',
    detached: true
  })

  // 等待 Mock Server 就绪
  await new Promise((resolve, reject) => {
    let output = ''
    const timeout = setTimeout(() => {
      reject(new Error('Mock Server 启动超时（10s）'))
    }, 10000)

    child.stdout.on('data', (data) => {
      output += data.toString()
      if (output.includes('PDA Mock Server 已启动')) {
        clearTimeout(timeout)
        console.log('[E2E] Mock Server 已就绪: http://localhost:3456')
        resolve()
      }
    })

    child.stderr.on('data', (data) => {
      console.error('[MockServer stderr]', data.toString())
    })

    child.on('error', (err) => {
      clearTimeout(timeout)
      reject(err)
    })
  })

  // 将子进程 PID 写入环境变量，供 teardown 使用
  process.env.MOCK_SERVER_PID = child.pid

  // 可选：健康检查
  const health = await fetch('http://localhost:3456/health')
  const healthJson = await health.json()
  console.log('[E2E] Mock Server 健康状态:', healthJson.data ? 'OK' : 'FAIL')
}
