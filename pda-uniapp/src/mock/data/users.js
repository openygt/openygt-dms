export const MOCK_USERS = [
  {
    userCode: 'admin',
    password: 'admin123',
    userId: 1,
    userName: '系统管理员',
    deviceCode: 'PDA-001',
    permissions: ['ROLE_ADMIN', 'ROLE_DIRECTOR', 'ROLE_WORKER', 'ROLE_INSPECTOR']
  },
  {
    userCode: 'worker',
    password: '123456',
    userId: 2,
    userName: '王师傅',
    deviceCode: 'PDA-002',
    permissions: ['ROLE_WORKER']
  },
  {
    userCode: 'qc',
    password: '123456',
    userId: 3,
    userName: '李质检',
    deviceCode: 'PDA-003',
    permissions: ['ROLE_INSPECTOR']
  }
]
