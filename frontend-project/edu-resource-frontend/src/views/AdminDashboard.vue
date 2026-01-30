<template>
  <div class="admin-dashboard">
    <div class="container">
      <div class="admin-header">
        <h1>系统管理后台</h1>
        <div class="admin-nav">
          <a :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">用户管理</a>
          <a :class="{ active: activeTab === 'requests' }" @click="activeTab = 'requests'">角色审批</a>
          <a :class="{ active: activeTab === 'logs' }" @click="activeTab = 'logs'">操作日志</a>
        </div>
      </div>

      <div class="admin-content">
        <!-- User Management -->
        <div v-if="activeTab === 'users'" class="content-section">
          <div class="section-header">
            <h2>用户列表</h2>
            <div class="search-bar">
              <input v-model="searchQuery" type="text" placeholder="搜索用户名/昵称..." />
            </div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>昵称</th>
                <th>角色</th>
                <th>状态</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.nickname }}</td>
                <td>
                  <span :class="['role-badge', user.role]">{{ getRoleLabel(user.role) }}</span>
                </td>
                <td>
                  <span :class="['status-badge', user.status]">{{ user.status === 'active' ? '正常' : '禁用' }}</span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>
                  <button class="btn-text" @click="handleToggleStatus(user)">
                    {{ user.status === 'active' ? '禁用' : '启用' }}
                  </button>
                  <button class="btn-text" @click="handleResetPassword(user)">重置密码</button>
                  <button class="btn-text" @click="handleEditRole(user)">修改角色</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Role Requests -->
        <div v-if="activeTab === 'requests'" class="content-section">
          <h2>角色切换申请</h2>
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户</th>
                <th>当前角色</th>
                <th>申请角色</th>
                <th>理由</th>
                <th>申请时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="req in requests" :key="req.id">
                <td>{{ req.id }}</td>
                <td>{{ req.nickname || req.username }}</td>
                <td>{{ getRoleLabel(req.currentRole) }}</td>
                <td>{{ getRoleLabel(req.targetRole) }}</td>
                <td class="col-reason" :title="req.reason">{{ req.reason }}</td>
                <td>{{ formatDate(req.createTime) }}</td>
                <td>
                  <span :class="['status-badge', req.status]">{{ getStatusLabel(req.status) }}</span>
                </td>
                <td>
                  <div v-if="req.status === 'pending'">
                    <button class="btn-text success" @click="handleAudit(req, 'approved')">通过</button>
                    <button class="btn-text danger" @click="handleAudit(req, 'rejected')">驳回</button>
                  </div>
                  <span v-else>{{ req.auditorName }} {{ getStatusLabel(req.status) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- System Logs -->
        <div v-if="activeTab === 'logs'" class="content-section">
          <h2>操作日志</h2>
          <table class="data-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>用户</th>
                <th>模块</th>
                <th>操作</th>
                <th>详情</th>
                <th>IP</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id">
                <td>{{ formatDate(log.createTime) }}</td>
                <td>{{ log.username }}</td>
                <td>{{ log.module }}</td>
                <td>{{ log.operation }}</td>
                <td class="col-details" :title="log.details">{{ log.details }}</td>
                <td>{{ log.ipAddress }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { adminApi } from '../api/admin-api'

export default {
  name: 'AdminDashboard',
  setup() {
    const activeTab = ref('users')
    const users = ref([])
    const requests = ref([])
    const logs = ref([])
    const searchQuery = ref('')

    const loadUsers = async () => {
      try {
        const res = await adminApi.getUsers()
        if (res.success) users.value = res.data
      } catch (err) {
        console.error(err)
      }
    }

    const loadRequests = async () => {
      try {
        const res = await adminApi.getRoleRequests()
        if (res.success) requests.value = res.data
      } catch (err) {
        console.error(err)
      }
    }

    const loadLogs = async () => {
      try {
        const res = await adminApi.getLogs()
        if (res.success) logs.value = res.data
      } catch (err) {
        console.error(err)
      }
    }

    watch(activeTab, (val) => {
      if (val === 'users') loadUsers()
      if (val === 'requests') loadRequests()
      if (val === 'logs') loadLogs()
    }, { immediate: true })

    const filteredUsers = computed(() => {
      if (!searchQuery.value) return users.value
      const q = searchQuery.value.toLowerCase()
      return users.value.filter(u => 
        u.username.toLowerCase().includes(q) || 
        (u.nickname && u.nickname.toLowerCase().includes(q))
      )
    })

    const getRoleLabel = (role) => {
      const map = { student: '学生', teacher: '教师', admin: '管理员' }
      return map[role] || role
    }

    const getStatusLabel = (status) => {
      const map = { pending: '待审核', approved: '已通过', rejected: '已驳回' }
      return map[status] || status
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleString()
    }

    const handleToggleStatus = async (user) => {
      const newStatus = user.status === 'active' ? 'disabled' : 'active'
      if (!confirm(`确定要${newStatus === 'active' ? '启用' : '禁用'}该用户吗？`)) return
      
      const adminPassword = prompt('请输入管理员密码以确认操作:')
      if (!adminPassword) return

      try {
        const res = await adminApi.updateUserStatus(user.id, newStatus, adminPassword)
        if (res.success) {
          user.status = newStatus
          alert('操作成功')
        } else {
          alert(res.message || '操作失败')
        }
      } catch (err) {
        alert('操作失败')
      }
    }

    const handleResetPassword = async (user) => {
      const pwd = prompt('请输入新密码:', '123456')
      if (!pwd) return
      
      try {
        const res = await adminApi.resetUserPassword(user.id, pwd)
        if (res.success) alert('密码重置成功')
      } catch (err) {
        alert('操作失败')
      }
    }

    const handleEditRole = async (user) => {
      const role = prompt('请输入新角色 (student/teacher/admin):', user.role)
      if (!role || role === user.role) return
      
      try {
        const res = await adminApi.updateUserRole(user.id, role)
        if (res.success) {
          user.role = role
          alert('角色修改成功')
        }
      } catch (err) {
        alert('操作失败')
      }
    }

    const handleAudit = async (req, status) => {
      const remark = prompt(status === 'approved' ? '请输入通过备注 (可选):' : '请输入驳回理由:')
      if (status === 'rejected' && !remark) return

      try {
        const res = await adminApi.auditRoleRequest(req.id, status, remark || '')
        if (res.success) {
          alert('审批完成')
          loadRequests()
        }
      } catch (err) {
        alert('操作失败')
      }
    }

    return {
      activeTab,
      users,
      requests,
      logs,
      searchQuery,
      filteredUsers,
      getRoleLabel,
      getStatusLabel,
      formatDate,
      handleToggleStatus,
      handleResetPassword,
      handleEditRole,
      handleAudit
    }
  }
}
</script>

<style scoped lang="less">
.admin-dashboard {
  min-height: 100vh;
  background: transparent;
  padding: 28px 0 56px;
}

.admin-header {
  background: rgba(255, 255, 255, 0.82);
  padding: 18px 18px;
  border-radius: 18px;
  border: 1px solid var(--border);
  margin-bottom: 24px;
  box-shadow: var(--shadow-md);
  display: flex;
  justify-content: space-between;
  align-items: center;

  h1 {
    font-size: 24px;
    margin: 0;
    color: var(--text);
    letter-spacing: -0.02em;
  }

  .admin-nav {
    display: flex;
    gap: 20px;
    
    a {
      cursor: pointer;
      padding: 8px 16px;
      border-radius: 12px;
      color: rgba(15, 23, 42, 0.70);
      font-weight: 600;
      transition: background var(--transition), color var(--transition), transform var(--transition);
      
      &.active {
        background: rgba(79, 109, 255, 0.12);
        color: rgba(15, 23, 42, 0.92);
      }
      
      &:hover {
        background: rgba(15, 23, 42, 0.06);
        color: rgba(15, 23, 42, 0.92);
        transform: translateY(-1px);
      }
    }
  }
}

.content-section {
  background: rgba(255, 255, 255, 0.82);
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);

  .section-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
    
    h2 {
      font-size: 20px;
      margin: 0;
      color: var(--text);
    }
    
    input {
      padding: 8px 12px;
      border: 1px solid rgba(15, 23, 42, 0.10);
      border-radius: 12px;
      width: 240px;
      background: rgba(255, 255, 255, 0.78);
      transition: box-shadow var(--transition), border-color var(--transition), background var(--transition);
      
      &:focus {
        outline: none;
        border-color: rgba(79, 109, 255, 0.55);
        box-shadow: var(--focus);
        background: rgba(255, 255, 255, 0.96);
      }
    }
  }
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  
  th, td {
    padding: 12px 16px;
    text-align: left;
    border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  }
  
  th {
    background: rgba(15, 23, 42, 0.03);
    font-weight: 600;
    color: rgba(15, 23, 42, 0.86);
  }
  
  .role-badge {
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 12px;
    
    &.admin { background: rgba(194, 71, 77, 0.14); color: rgba(15, 23, 42, 0.82); border: 1px solid rgba(194, 71, 77, 0.18); }
    &.teacher { background: rgba(79, 109, 255, 0.14); color: rgba(15, 23, 42, 0.82); border: 1px solid rgba(79, 109, 255, 0.18); }
    &.student { background: rgba(59, 155, 122, 0.14); color: rgba(15, 23, 42, 0.82); border: 1px solid rgba(59, 155, 122, 0.18); }
  }
  
  .status-badge {
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 12px;
    
    &.active, &.approved { color: rgba(15, 23, 42, 0.82); background: rgba(59, 155, 122, 0.14); border: 1px solid rgba(59, 155, 122, 0.20); }
    &.disabled, &.rejected { color: rgba(15, 23, 42, 0.82); background: rgba(194, 71, 77, 0.14); border: 1px solid rgba(194, 71, 77, 0.20); }
    &.pending { color: rgba(15, 23, 42, 0.82); background: rgba(184, 138, 59, 0.16); border: 1px solid rgba(184, 138, 59, 0.22); }
  }
  
  .btn-text {
    border: none;
    background: none;
    color: rgba(15, 23, 42, 0.78);
    cursor: pointer;
    margin-right: 8px;
    padding: 0;
    
    &:hover {
      color: rgba(15, 23, 42, 0.92);
    }
    
    &.danger { color: rgba(194, 71, 77, 0.92); }
    &.success { color: rgba(59, 155, 122, 0.92); }
  }
  
  .col-reason, .col-details {
    max-width: 200px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
