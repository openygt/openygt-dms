export const dashboardHandlers = {
  /** GET /dashboard/stats */
  stats() {
    return {
      code: 200,
      message: 'success',
      data: {
        todayTotal: 87,
        todayCompleted: 42,
        inProgress: 23,
        pendingCount: 5,
        todayStartTime: '2026-05-04 07:00',
        updateTime: '2026-05-04 12:00'
      }
    }
  },

  /** GET /dashboard/task-status-count */
  taskStatusCount() {
    return {
      code: 200,
      message: 'success',
      data: [
        { status: 'PENDING', statusName: '待泡药', count: 5 },
        { status: 'SOAKING', statusName: '泡药中', count: 8 },
        { status: 'SOAKED', statusName: '泡药完成', count: 6 },
        { status: 'DECOCTING', statusName: '煎药中', count: 12 },
        { status: 'DECOCTED', statusName: '煎药完成', count: 10 },
        { status: 'POURING', statusName: '封装中', count: 7 },
        { status: 'PACKAGING', statusName: '包装中', count: 4 },
        { status: 'LABELING', statusName: '贴标中', count: 3 },
        { status: 'INSPECTING', statusName: '质检中', count: 2 },
        { status: 'COMPLETED', statusName: '已完成', count: 42 },
        { status: 'CANCELLED', statusName: '已取消', count: 3 }
      ]
    }
  }
}
