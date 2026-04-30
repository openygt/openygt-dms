/**
 * Playwright Global Teardown - 关闭 PDA Mock Server
 */

module.exports = async function globalTeardown() {
  const pid = process.env.MOCK_SERVER_PID
  if (pid) {
    console.log('[E2E] 关闭 PDA Mock Server (PID:', pid, ')')
    try {
      process.kill(pid, 'SIGTERM')
    } catch (e) {
      console.log('[E2E] Mock Server 可能已退出')
    }
  }
}
