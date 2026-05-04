import { MOCK_USERS } from '../data/users.js'

export const authHandlers = {
  /** POST /auth/login */
  login(params) {
    const user = MOCK_USERS.find(u => u.userCode === params.userCode && u.password === params.password)
    if (!user) {
      return { code: 401, message: '账号或密码错误', data: null }
    }
    const token = 'mock_token_' + user.userCode + '_' + Date.now()
    const userInfo = {
      userId: user.userId,
      userCode: user.userCode,
      userName: user.userName,
      deviceCode: user.deviceCode,
      permissions: user.permissions
    }
    uni.setStorageSync('pda_token', token)
    uni.setStorageSync('pda_user_info', userInfo)
    return {
      code: 200,
      message: 'success',
      data: { token, userInfo, recordId: Date.now() }
    }
  },

  /** POST /auth/scan-login */
  scanLogin(params) {
    const user = MOCK_USERS[0]
    const token = 'mock_token_scan_' + Date.now()
    const userInfo = {
      userId: user.userId,
      userCode: user.userCode,
      userName: user.userName,
      deviceCode: params.deviceCode || user.deviceCode,
      permissions: user.permissions
    }
    uni.setStorageSync('pda_token', token)
    uni.setStorageSync('pda_user_info', userInfo)
    return {
      code: 200,
      message: 'success',
      data: { token, userInfo, recordId: Date.now() }
    }
  },

  /** POST /auth/logout */
  logout() {
    uni.removeStorageSync('pda_token')
    uni.removeStorageSync('pda_user_info')
    return { code: 200, message: 'success', data: {} }
  },

  /** POST /change-password */
  changePassword() {
    return { code: 200, message: '密码修改成功', data: {} }
  }
}
