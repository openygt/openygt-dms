/**
 * PDA Mock Server - 照片路由
 */

const express = require('express')
const router = express.Router()
const store = require('../store')

// POST /api/v1/pda/photos/upload - 拍照上传
router.post('/upload', (req, res) => {
  // 支持 multipart/form-data 和 JSON 两种格式
  const { taskId, photoType, stepType, remark, photoUrl } = req.body

  const task = store.getTask(parseInt(taskId))
  if (!task) {
    return res.jsonErr(404, '任务不存在')
  }

  const photoId = store.db.photos.length + 1
  store.db.photos.push({
    id: photoId,
    taskId: parseInt(taskId),
    photoType: photoType || 'REVIEW',
    stepType: stepType || null,
    photoUrl: photoUrl || `https://mock.cdn/photo/${photoId}.jpg`,
    remark: remark || '',
    uploadedAt: new Date().toISOString()
  })

  store.addOperationLog(
    { userId: task.operatorId, userName: task.operatorName },
    parseInt(taskId),
    'PHOTO_UPLOAD',
    { photoType, stepType, photoId }
  )

  res.jsonOk({
    photoId: photoId,
    url: `https://mock.cdn/photo/${photoId}.jpg`,
    taskId: parseInt(taskId)
  })
})

module.exports = router
