/**
 * 状态字典统一映射文件
 *
 * 原则：数据库只存英文编码（UPPER_SNAKE_CASE），前端通过映射表展示中文
 * 禁止在业务代码中裸写中文字符串
 */

// ==================== 任务状态映射 ====================
export const TaskStatusMap: Record<string, string> = {
  WAIT_SOAK: '待泡药',
  SOAKING: '泡药中',
  WAIT_DECOCT: '待煎药',
  DECOCTING: '煎药中',
  WAIT_POUR: '待出液',
  POURING: '出液中',
  WAIT_WRAP: '待包装',
  WRAPPING: '包装中',
  WAIT_LABEL: '待贴标',
  WAIT_QC: '待质检',
  STORED: '已暂存',
  WAIT_HANDOVER: '待交接',
  SECOND_JUDGEMENT: '待二次判定',
  SUSPENDED: '已挂起',
  COMPLETED: '已完成',
  PARTIAL_COMPLETED: '已部分完成',
  SCRAPPED: '已报废',
  REWORK: '返工中',
  CANCELLED: '已取消',
}

// ==================== 处方接收状态映射 ====================
export const PrescriptionStatusMap: Record<string, string> = {
  PENDING: '待接收',
  RECEIVED: '已接收',
  REJECTED: '已拒绝',
}

// ==================== 发药/配送状态映射 ====================
export const DeliveryStatusMap: Record<string, string> = {
  PENDING: '待发药',
  DELIVERED: '已发药',
  PARTIAL: '部分发药',
}

// ==================== 设备业务状态映射 ====================
export const DeviceBizStatusMap: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  FAULT: '故障',
  MAINTENANCE: '维护中',
}

// ==================== 通用状态标签获取（兜底：编码原样返回）====================
export function getStatusLabel(status: string | undefined, map: Record<string, string>): string {
  if (!status) return '未知'
  return map[status] || status
}

// ==================== el-tag 类型映射 ====================
export function getStatusType(status: string | undefined): 'success' | 'warning' | 'danger' | 'info' {
  const successStatuses = ['COMPLETED', 'RECEIVED', 'DELIVERED', 'ONLINE']
  const warningStatuses = ['WAIT_WRAP', 'WRAPPING', 'WAIT_LABEL', 'WAIT_QC', 'MAINTENANCE', 'PENDING', 'WAIT_HANDOVER']
  const dangerStatuses = ['REWORK', 'CANCELLED', 'REJECTED', 'FAULT', 'SCRAPPED', 'SUSPENDED']

  if (!status) return 'info'
  if (successStatuses.includes(status)) return 'success'
  if (warningStatuses.includes(status)) return 'warning'
  if (dangerStatuses.includes(status)) return 'danger'
  return 'info'
}
