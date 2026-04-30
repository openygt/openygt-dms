/**
 * PDA Mock Server - 设备路由
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// GET /api/v1/pda/devices/nearby - 附近设备列表
router.get('/nearby', (req, res) => {
  const { type } = req.query

  let devices = store.db.devices.filter(d => d.status === 'NORMAL')
  if (type) {
    devices = devices.filter(d => d.deviceType === type)
  }

  res.jsonOk(devices.map(d => ({
    deviceId: d.deviceId,
    deviceCode: d.deviceCode,
    deviceName: d.deviceName,
    deviceType: d.deviceType,
    status: d.status
  })))
})

module.exports = router
