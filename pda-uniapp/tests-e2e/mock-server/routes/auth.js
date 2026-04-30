/**
 * PDA Mock Server - 认证路由
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// POST /api/v1/pda/auth/login - 账号密码登录
router.post('/login', (req, res) => {
  const { loginType, username, password, deviceCode } = req.body

  if (loginType === 'account' || !loginType) {
    const user = store.db.users.find(u => u.userCode === username && u.password === password)
    if (!user) {
      return res.jsonErr(401, '用户名或密码错误')
    }

    // 记录登录
    const recordId = store.db.loginRecords.length + 1
    store.db.loginRecords.push({
      id: recordId,
      userId: user.userId,
      userName: user.userName,
      userCode: user.userCode,
      loginType: 'ACCOUNT',
      deviceCode: deviceCode || null,
      loginAt: new Date().toISOString(),
      logoutAt: null,
      onlineDuration: 0
    })

    // 更新 session
    store.db.sessionCounter++

    return res.jsonOk({
      token: `mock-jwt-${store.db.sessionCounter}`,
      userCode: user.userCode,
      userName: user.userName,
      deviceCode: deviceCode || null,
      deviceName: deviceCode ? store.db.devices.find(d => d.deviceCode === deviceCode)?.deviceName : null,
      recordId: recordId,
      permissions: user.permissions
    })
  }

  return res.jsonErr(400, '不支持的登录类型')
})

// POST /api/v1/pda/auth/scan-login - 扫码登录
router.post('/scan-login', (req, res) => {
  const { scanCode, deviceCode } = req.body

  const user = store.db.users.find(u => u.barcode === scanCode)
  if (!user) {
    return res.jsonErr(401, '员工码不存在')
  }

  const recordId = store.db.loginRecords.length + 1
  store.db.loginRecords.push({
    id: recordId,
    userId: user.userId,
    userName: user.userName,
    userCode: user.userCode,
    loginType: 'SCAN',
    deviceCode: deviceCode || null,
    loginAt: new Date().toISOString(),
    logoutAt: null,
    onlineDuration: 0
  })

  store.db.sessionCounter++

  return res.jsonOk({
    token: `mock-jwt-scan-${store.db.sessionCounter}`,
    userCode: user.userCode,
    userName: user.userName,
    deviceCode: deviceCode || null,
    deviceName: deviceCode ? store.db.devices.find(d => d.deviceCode === deviceCode)?.deviceName : null,
    recordId: recordId,
    permissions: user.permissions
  })
})

module.exports = router
