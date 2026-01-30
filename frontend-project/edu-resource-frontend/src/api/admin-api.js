import request from '@/utils/request'

export const adminApi = {
  getUsers() {
    return request.get('/admin/users')
  },
  
  updateUserStatus(userId, status, adminPassword) {
    return request.post('/admin/users/status', { userId, status, adminPassword })
  },
  
  resetUserPassword(userId, newPassword) {
    return request.post('/admin/users/password', { userId, newPassword })
  },
  
  updateUserRole(userId, role) {
    return request.post('/admin/users/role', { userId, role })
  },
  
  getRoleRequests() {
    return request.get('/admin/role-requests')
  },
  
  auditRoleRequest(requestId, status, remark) {
    return request.post('/admin/role-requests/audit', { requestId, status, remark })
  },
  
  getLogs() {
    return request.get('/admin/logs')
  }
}
