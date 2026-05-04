/**
 * 15 个模拟任务，覆盖所有关键状态
 * 条码格式: JY20260504xxx
 */
export const MOCK_TASKS = [
  {
    taskId: 1001, barcode: 'JY20260504001', patientName: '张建国',
    prescriptionNumber: 'CH20260504001', hospitalName: '市中医院',
    repetition: 7, taskStatus: 'PENDING', statusName: '待泡药',
    createdTime: '2026-05-04T07:00:00', updatedAt: '2026-05-04T07:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: false, current: false, time: '' }
    ]
  },
  {
    taskId: 1002, barcode: 'JY20260504002', patientName: '李明芳',
    prescriptionNumber: 'CH20260504002', hospitalName: '市中医二院',
    repetition: 5, taskStatus: 'SOAKING', statusName: '泡药中',
    createdTime: '2026-05-04T06:30:00', updatedAt: '2026-05-04T07:30:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '07:30' },
      { value: 'END_SOAK', label: '结束泡药', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1003, barcode: 'JY20260504003', patientName: '王翠花',
    prescriptionNumber: 'CH20260504003', hospitalName: '市中医院',
    repetition: 10, taskStatus: 'SOAKED', statusName: '泡药完成',
    createdTime: '2026-05-04T06:00:00', updatedAt: '2026-05-04T08:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '07:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '08:00' },
      { value: 'START_DECOCT', label: '开始煎药', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1004, barcode: 'JY20260504004', patientName: '赵德明',
    prescriptionNumber: 'CH20260504004', hospitalName: '市中医二院',
    repetition: 7, taskStatus: 'DECOCTING', statusName: '煎药中',
    createdTime: '2026-05-04T05:30:00', updatedAt: '2026-05-04T08:30:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '06:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '07:30' },
      { value: 'START_DECOCT', label: '开始煎药', completed: true, current: false, time: '08:00' },
      { value: 'END_DECOCT', label: '结束煎药', completed: false, current: true, time: '' }
    ],
    medicines: [
      { name: '当归', dosage: 15, unit: 'g' }, { name: '川芎', dosage: 10, unit: 'g' },
      { name: '白芍', dosage: 12, unit: 'g' }, { name: '熟地黄', dosage: 20, unit: 'g' },
      { name: '党参', dosage: 15, unit: 'g' }, { name: '黄芪', dosage: 30, unit: 'g' }
    ]
  },
  {
    taskId: 1005, barcode: 'JY20260504005', patientName: '刘秀英',
    prescriptionNumber: 'CH20260504005', hospitalName: '市中医院',
    repetition: 14, taskStatus: 'DECOCTED', statusName: '煎药完成',
    createdTime: '2026-05-04T05:00:00', updatedAt: '2026-05-04T09:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '06:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '07:00' },
      { value: 'START_DECOCT', label: '开始煎药', completed: true, current: false, time: '07:30' },
      { value: 'END_DECOCT', label: '结束煎药', completed: true, current: false, time: '09:00' },
      { value: 'START_POUR', label: '开始封装', completed: false, current: true, time: '' }
    ],
    medicines: [
      { name: '当归', dosage: 15, unit: 'g' }, { name: '川芎', dosage: 10, unit: 'g' },
      { name: '白芍', dosage: 12, unit: 'g' }, { name: '熟地黄', dosage: 20, unit: 'g' }
    ]
  },
  {
    taskId: 1006, barcode: 'JY20260504006', patientName: '陈伟强',
    prescriptionNumber: 'CH20260504006', hospitalName: '市中医二院',
    repetition: 7, taskStatus: 'POURING', statusName: '封装中',
    createdTime: '2026-05-04T04:30:00', updatedAt: '2026-05-04T09:30:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '05:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '06:30' },
      { value: 'START_DECOCT', label: '开始煎药', completed: true, current: false, time: '07:00' },
      { value: 'END_DECOCT', label: '结束煎药', completed: true, current: false, time: '09:00' },
      { value: 'START_POUR', label: '开始封装', completed: true, current: false, time: '09:15' },
      { value: 'END_POUR', label: '结束封装', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1007, barcode: 'JY20260504007', patientName: '林小红',
    prescriptionNumber: 'CH20260504007', hospitalName: '市中医院',
    repetition: 5, taskStatus: 'POURED', statusName: '封装完成',
    createdTime: '2026-05-04T04:00:00', updatedAt: '2026-05-04T10:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '05:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '06:00' },
      { value: 'START_DECOCT', label: '开始煎药', completed: true, current: false, time: '06:30' },
      { value: 'END_DECOCT', label: '结束煎药', completed: true, current: false, time: '08:30' },
      { value: 'START_POUR', label: '开始封装', completed: true, current: false, time: '09:00' },
      { value: 'END_POUR', label: '结束封装', completed: true, current: false, time: '10:00' },
      { value: 'START_WRAP', label: '开始包装', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1008, barcode: 'JY20260504008', patientName: '黄大勇',
    prescriptionNumber: 'CH20260504008', hospitalName: '市中医二院',
    repetition: 10, taskStatus: 'PACKAGING', statusName: '包装中',
    createdTime: '2026-05-04T03:30:00', updatedAt: '2026-05-04T10:30:00',
    steps: [
      { value: 'START_WRAP', label: '开始包装', completed: true, current: false, time: '10:00' },
      { value: 'END_WRAP', label: '包装完成', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1009, barcode: 'JY20260504009', patientName: '吴美丽',
    prescriptionNumber: 'CH20260504009', hospitalName: '市中医院',
    repetition: 7, taskStatus: 'PACKAGED', statusName: '包装完成',
    createdTime: '2026-05-04T03:00:00', updatedAt: '2026-05-04T11:00:00',
    steps: [
      { value: 'START_WRAP', label: '开始包装', completed: true, current: false, time: '10:30' },
      { value: 'END_WRAP', label: '包装完成', completed: true, current: false, time: '11:00' },
      { value: 'START_LABEL', label: '开始贴标', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1010, barcode: 'JY20260504010', patientName: '孙志远',
    prescriptionNumber: 'CH20260504010', hospitalName: '市中医二院',
    repetition: 5, taskStatus: 'LABELING', statusName: '贴标中',
    createdTime: '2026-05-04T02:30:00', updatedAt: '2026-05-04T11:15:00',
    steps: [
      { value: 'START_LABEL', label: '开始贴标', completed: true, current: false, time: '11:00' },
      { value: 'END_LABEL', label: '贴标完成', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1011, barcode: 'JY20260504011', patientName: '周小红',
    prescriptionNumber: 'CH20260504011', hospitalName: '市中医院',
    repetition: 7, taskStatus: 'LABELED', statusName: '贴标完成',
    createdTime: '2026-05-04T02:00:00', updatedAt: '2026-05-04T11:45:00',
    steps: [
      { value: 'START_LABEL', label: '开始贴标', completed: true, current: false, time: '11:20' },
      { value: 'END_LABEL', label: '贴标完成', completed: true, current: false, time: '11:45' },
      { value: 'START_QC', label: '开始质检', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1012, barcode: 'JY20260504012', patientName: '马国强',
    prescriptionNumber: 'CH20260504012', hospitalName: '市中医二院',
    repetition: 14, taskStatus: 'INSPECTING', statusName: '质检中',
    createdTime: '2026-05-04T01:30:00', updatedAt: '2026-05-04T12:00:00',
    steps: [
      { value: 'START_QC', label: '开始质检', completed: true, current: false, time: '11:50' },
      { value: 'PASS_QC', label: '质检通过', completed: false, current: true, time: '' }
    ]
  },
  {
    taskId: 1013, barcode: 'JY20260504013', patientName: '郑秀兰',
    prescriptionNumber: 'CH20260504013', hospitalName: '市中医院',
    repetition: 7, taskStatus: 'COMPLETED', statusName: '已完成',
    createdTime: '2026-05-03T08:00:00', updatedAt: '2026-05-03T16:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '08:00' },
      { value: 'END_SOAK', label: '结束泡药', completed: true, current: false, time: '09:00' },
      { value: 'START_DECOCT', label: '开始煎药', completed: true, current: false, time: '09:30' },
      { value: 'END_DECOCT', label: '结束煎药', completed: true, current: false, time: '11:30' },
      { value: 'START_POUR', label: '开始封装', completed: true, current: false, time: '12:00' },
      { value: 'END_POUR', label: '结束封装', completed: true, current: false, time: '13:00' },
      { value: 'START_WRAP', label: '开始包装', completed: true, current: false, time: '13:30' },
      { value: 'END_WRAP', label: '包装完成', completed: true, current: false, time: '14:00' },
      { value: 'START_LABEL', label: '开始贴标', completed: true, current: false, time: '14:30' },
      { value: 'END_LABEL', label: '贴标完成', completed: true, current: false, time: '15:00' },
      { value: 'START_QC', label: '开始质检', completed: true, current: false, time: '15:15' },
      { value: 'PASS_QC', label: '质检通过', completed: true, current: false, time: '15:45' },
      { value: 'STORED', label: '已入库', completed: true, current: false, time: '16:00' }
    ]
  },
  {
    taskId: 1014, barcode: 'JY20260504014', patientName: '钱学文',
    prescriptionNumber: 'CH20260504014', hospitalName: '市中医二院',
    repetition: 5, taskStatus: 'CANCELLED', statusName: '已取消',
    createdTime: '2026-05-04T06:00:00', updatedAt: '2026-05-04T07:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: true, current: false, time: '06:30' },
      { value: 'CANCELLED', label: '已取消', completed: true, current: true, time: '07:00' }
    ]
  },
  {
    taskId: 1015, barcode: 'JY20260504015', patientName: '冯丽华',
    prescriptionNumber: 'CH20260504015', hospitalName: '市中医院',
    repetition: 10, taskStatus: 'PENDING', statusName: '待泡药',
    createdTime: '2026-05-04T08:00:00', updatedAt: '2026-05-04T08:00:00',
    steps: [
      { value: 'START_SOAK', label: '开始泡药', completed: false, current: false, time: '' }
    ]
  }
]

/** 按条码查找任务 */
export function findTask(barcode) {
  return MOCK_TASKS.find(t => t.barcode === barcode) || null
}

/** 按 taskId 查找任务 */
export function findTaskById(taskId) {
  return MOCK_TASKS.find(t => t.taskId === Number(taskId)) || null
}

/** 深拷贝任务（用于 handler 中安全返回） */
export function cloneTask(task) {
  return JSON.parse(JSON.stringify(task))
}
