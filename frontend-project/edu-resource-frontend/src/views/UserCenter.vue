<template>
  <div class="user-center-page">
    <div class="container">
      <div class="user-center-layout">
        <aside class="user-sidebar">
          <div class="user-profile-card">
            <div class="avatar-section">
              <img :src="user.avatar || defaultAvatar" alt="User Avatar" class="user-avatar" />
              <button class="btn-avatar-upload" @click="triggerFileInput">
                <i class="icon-camera"></i>
              </button>
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                @change="handleAvatarUpload"
                style="display: none"
              />
            </div>
            <h3 class="user-name">{{ user.nickname || user.username }}</h3>
            <p class="user-role">{{ roleLabel }}</p>
          </div>
          
          <nav class="user-nav">
            <a
              class="nav-item"
              :class="{ active: activeTab === 'profile' }"
              @click="activeTab = 'profile'"
            >
              <i class="icon-user"></i> 个人资料
            </a>
            <a
              class="nav-item"
              :class="{ active: activeTab === 'security' }"
              @click="activeTab = 'security'"
            >
              <i class="icon-lock"></i> 安全设置
            </a>
            <a
              class="nav-item"
              :class="{ active: activeTab === 'role-change' }"
              @click="activeTab = 'role-change'"
            >
              <i class="icon-id-card"></i> 身份切换
            </a>
          </nav>
        </aside>
        
        <main class="user-content">
          <div v-if="activeTab === 'profile'" class="content-section">
            <h2>个人资料</h2>
            <form class="profile-form" @submit.prevent="handleUpdateProfile">
              <div class="form-group">
                <label>用户名</label>
                <input type="text" :value="user.username" disabled />
              </div>
              
              <div class="form-group">
                <label>昵称</label>
                <input v-model="profileForm.nickname" type="text" placeholder="请输入昵称" />
              </div>
              
              <div class="form-group">
                <label>手机号码</label>
                <input v-model="profileForm.phone" type="tel" placeholder="请输入手机号码" />
              </div>
              
              <div class="form-group">
                <label>角色</label>
                <input type="text" :value="roleLabel" disabled />
              </div>
              
              <button type="submit" class="btn btn-primary" :disabled="loading">
                <i v-if="loading" class="icon-loading"></i>
                {{ loading ? '保存中...' : '保存修改' }}
              </button>
            </form>
          </div>
          
          <div v-if="activeTab === 'security'" class="content-section">
            <h2>修改密码</h2>
            <form class="password-form" @submit.prevent="handleChangePassword">
              <div class="form-group">
                <label>当前密码</label>
                <input v-model="passwordForm.currentPassword" type="password" required />
              </div>
              
              <div class="form-group">
                <label>新密码</label>
                <input v-model="passwordForm.newPassword" type="password" required minlength="6" />
              </div>
              
              <div class="form-group">
                <label>确认新密码</label>
                <input v-model="passwordForm.confirmPassword" type="password" required />
              </div>
              
              <button type="submit" class="btn btn-primary" :disabled="loading">
                <i v-if="loading" class="icon-loading"></i>
                {{ loading ? '修改中...' : '修改密码' }}
              </button>
            </form>
          </div>
          
          <div v-if="activeTab === 'role-change'" class="content-section">
            <h2>身份切换申请</h2>
            <div class="role-request-form">
              <div class="form-group">
                <label>当前身份</label>
                <input type="text" :value="roleLabel" disabled />
              </div>
              <div class="form-group">
                <label>目标身份</label>
                <select v-model="roleRequest.targetRole">
                  <option value="student" v-if="user.role === 'teacher'">学生</option>
                  <option value="teacher" v-if="user.role === 'student'">老师</option>
                </select>
              </div>
              <div class="form-group">
                <label>申请理由</label>
                <textarea v-model="roleRequest.reason" placeholder="请说明您申请切换身份的理由..." rows="4"></textarea>
              </div>
              <button class="btn btn-primary" @click="handleSubmitRoleRequest" :disabled="loading">
                {{ loading ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/user-api'

export default {
  name: 'UserCenter',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    
    const user = computed(() => store.state.user)
    const canUpload = computed(() => store.getters.canUpload)
    
    const activeTab = ref(route.query.tab || 'profile')
    const loading = ref(false)
    
    const profileForm = reactive({
      nickname: '',
      phone: ''
    })
    
    const passwordForm = reactive({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const roleRequest = reactive({
      targetRole: '',
      reason: ''
    })
    
    const fileInput = ref(null)
    
    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'
    
    const roleLabel = computed(() => {
      const roles = {
        student: '学生',
        teacher: '老师',
        admin: '管理员'
      }
      return roles[user.value.role] || '用户'
    })
    
    const loadUserData = async () => {
      try {
        profileForm.nickname = user.value.nickname
        profileForm.phone = user.value.phone
      } catch (err) {
        console.error('Failed to load user data:', err)
      }
    }
    
    const handleUpdateProfile = async () => {
      loading.value = true
      try {
        // Clear any existing invalid token and re-login
        if (!store.state.token) {
          alert('请先登录')
          router.push('/login')
          loading.value = false
          return
        }
        
        const response = await userApi.updateProfile(profileForm)
        if (response.success) {
          store.commit('UPDATE_USER_INFO', response.data)
          alert('个人资料更新成功！')
        } else {
          alert(response.message || '更新个人资料失败')
        }
      } catch (err) {
        console.error('Failed to update profile:', err)
        if (err.response && err.response.status === 401) {
          alert('登录已过期，请重新登录')
          store.dispatch('logout')
          router.push('/login')
        } else if (err.response) {
          alert(`请求失败: ${err.response.data?.message || err.response.statusText}`)
        } else {
          alert('网络错误，请稍后重试')
        }
      } finally {
        loading.value = false
      }
    }
    
    const handleChangePassword = async () => {
      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        alert('两次输入的密码不一致')
        return
      }
      
      loading.value = true
      try {
        const response = await userApi.changePassword({
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword
        })
        if (response.success) {
          alert('密码修改成功！')
          passwordForm.currentPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
        } else {
          alert(response.message || '修改密码失败')
        }
      } catch (err) {
        alert('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    const handleSubmitRoleRequest = async () => {
      if (!roleRequest.targetRole || !roleRequest.reason) {
        alert('请填写完整申请信息')
        return
      }
      
      loading.value = true
      try {
        const response = await userApi.submitRoleRequest({
          currentRole: user.value.role,
          targetRole: roleRequest.targetRole,
          reason: roleRequest.reason
        })
        if (response.success) {
          alert('申请提交成功，请等待管理员审核')
          roleRequest.reason = ''
          activeTab.value = 'profile'
        } else {
          alert(response.message || '提交申请失败')
        }
      } catch (err) {
        alert('网络错误，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    const triggerFileInput = () => {
      fileInput.value.click()
    }
    
    const handleAvatarUpload = async (e) => {
      const file = e.target.files[0]
      if (!file) return
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件！')
        return
      }
      
      // Validate file size (1MB limit)
      if (file.size > 1024 * 1024) {
        alert('图片大小不能超过1MB！')
        return
      }
      
      try {
        loading.value = true
        const response = await userApi.uploadAvatar(file)
        
        if (response.success) {
          // Update user avatar in store
          store.dispatch('updateUserInfo', { ...user.value, avatar: response.data })
          alert('头像上传成功！')
        } else {
          alert(response.message || '头像上传失败')
        }
      } catch (err) {
        console.error('Failed to upload avatar:', err)
        if (err.response && err.response.status === 401) {
          alert('登录已过期，请重新登录')
          store.dispatch('logout')
          router.push('/login')
        } else {
          alert('网络错误，请稍后重试')
        }
      } finally {
        loading.value = false
        // Reset file input
        if (fileInput.value) {
          fileInput.value.value = ''
        }
      }
    }
    
    onMounted(async () => {
      loadUserData()
      
      // Validate token when entering user center
      if (store.getters.isLoggedIn) {
        try {
          console.log('Validating token on user center entry')
          const response = await userApi.getUserInfo()
          console.log('Token validation successful')
        } catch (error) {
          console.error('Token validation failed on user center entry:', error)
          if (error.response && error.response.status === 401) {
            alert('登录已过期，请重新登录')
            store.dispatch('logout')
            router.push('/login')
          }
        }
      }
    })

    return {
      user,
      canUpload,
      activeTab,
      loading,
      profileForm,
      passwordForm,
      defaultAvatar,
      roleLabel,
      fileInput,
      handleUpdateProfile,
      handleChangePassword,
      triggerFileInput,
      handleAvatarUpload,
      handleSubmitRoleRequest,
      roleRequest
    }
  }
}
</script>

<style scoped lang="less">
.user-center-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}

.user-center-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.user-sidebar {
  .user-profile-card {
    background: white;
    border-radius: 12px;
    padding: 32px;
    text-align: center;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    .avatar-section {
      position: relative;
      display: inline-block;
      margin-bottom: 16px;
      
      .user-avatar {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        object-fit: cover;
        border: 3px solid #667eea;
      }
      
      .btn-avatar-upload {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #667eea;
        color: white;
        border: 2px solid white;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        
        &:hover {
          background: #5568d3;
        }
      }
    }
    
    .user-name {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }
    
    .user-role {
      font-size: 14px;
      color: #999;
    }
  }
  
  .user-nav {
    background: white;
    border-radius: 12px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      color: #666;
      text-decoration: none;
      margin-bottom: 4px;
      
      &:hover {
        background: #f5f7fa;
        color: #667eea;
      }
      
      &.active {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }
    }
  }
}

.user-content {
  .content-section {
    background: white;
    border-radius: 12px;
    padding: 32px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
      margin-bottom: 32px;
      padding-bottom: 16px;
      border-bottom: 2px solid #f0f0f0;
    }
    
    .role-request-form {
      max-width: 600px;
      
      .form-group {
        margin-bottom: 24px;
        
        label {
          display: block;
          font-size: 14px;
          font-weight: 500;
          color: #333;
          margin-bottom: 8px;
        }
        
        input, select, textarea {
          width: 100%;
          padding: 12px 16px;
          border: 2px solid #e0e0e0;
          border-radius: 8px;
          font-size: 14px;
          transition: all 0.3s;
          
          &:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
          }
          
          &:disabled {
            background: #f5f7fa;
            cursor: not-allowed;
          }
        }
        
        textarea {
          resize: vertical;
          min-height: 100px;
        }
      }
    }
    
    .form-group {
      margin-bottom: 24px;
      
      label {
        display: block;
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 8px;
      }
      
      input {
        width: 100%;
        padding: 12px 16px;
        border: 2px solid #e0e0e0;
        border-radius: 8px;
        font-size: 14px;
        transition: all 0.3s;
        
        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        
        &:disabled {
          background: #f5f7fa;
          cursor: not-allowed;
        }
      }
    }
  }
  
  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    
    i {
      font-size: 64px;
      margin-bottom: 16px;
      display: block;
    }
    
    p {
      font-size: 16px;
      margin-bottom: 24px;
    }
  }
  
  .resources-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    
    .resource-card {
      background: #f8f9fa;
      border-radius: 12px;
      padding: 20px;
      transition: all 0.3s;
      position: relative;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      .resource-icon {
        width: 48px;
        height: 48px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
        
        i {
          font-size: 24px;
          color: white;
        }
      }
      
      .resource-info {
        h4 {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin-bottom: 8px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        p {
          font-size: 13px;
          color: #666;
          margin-bottom: 12px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .resource-meta {
          display: flex;
          gap: 16px;
          font-size: 12px;
          color: #999;
          
          span {
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }
      }
      
      .resource-actions {
        position: absolute;
        top: 16px;
        right: 16px;
        display: flex;
        gap: 8px;
        opacity: 0;
        transition: opacity 0.3s;
        
        .resource-card:hover & {
          opacity: 1;
        }
        
        .btn {
          width: 32px;
          height: 32px;
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
          border: none;
          cursor: pointer;
          transition: all 0.3s;
          
          &.btn-edit {
            background: #e3f2fd;
            color: #2196f3;
            
            &:hover {
              background: #2196f3;
              color: white;
            }
          }
          
          &.btn-delete {
            background: #ffebee;
            color: #f44336;
            
            &:hover {
              background: #f44336;
              color: white;
            }
          }
          
          &.btn-view {
            background: #e8f5e9;
            color: #4caf50;
            
            &:hover {
              background: #4caf50;
              color: white;
            }
          }
          
          &.btn-uncollect {
            background: #fff3e0;
            color: #ff9800;
            
            &:hover {
              background: #ff9800;
              color: white;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .user-center-layout {
    grid-template-columns: 1fr;
  }
  
  .user-sidebar {
    .user-nav {
      display: flex;
      overflow-x: auto;
      gap: 8px;
      
      .nav-item {
        white-space: nowrap;
        margin-bottom: 0;
      }
    }
  }
}
</style>
