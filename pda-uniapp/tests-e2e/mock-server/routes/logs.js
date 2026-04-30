/**
 * PDA Mock Server - 日志路由
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// GET /api/v1/pda/logs/operation - 操作日志查询
router.get('/operation', (req, res) => {
  const { userId, taskId, operationType, page = 1, size = 20 } = req.query

  let logs = [...store.db.operationLogs].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  if (userId) logs = logs.filter(l => l.userId === parseInt(userId))
  if (taskId) logs = logs.filter(l => l.taskId === parseInt(taskId))
  if (operationType) logs = logs.filter(l => l.operationType === operationType)

  const total = logs.length
  const start = (page - 1) * size
  const data = logs.slice(start, start + parseInt(size))

  res.jsonOk({
    list: data,
    total,
    page: parseInt(page),
    size: parseInt(size)
  })
})

module.exports = router
