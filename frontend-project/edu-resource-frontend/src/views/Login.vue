<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h2 class="platform-title">教育资源共享平台</h2>
        <h1>欢迎回来</h1>
        <p>登录以访问教育资源</p>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <div class="password-input-wrapper">
            <input
              id="password"
              v-model="formData.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              required
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showPassword = !showPassword"
              tabindex="-1"
              :title="showPassword ? '隐藏密码' : '显示密码'"
            >
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="formData.rememberMe" type="checkbox" />
            <span>记住我</span>
          </label>
        </div>
        
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          <i v-if="loading" class="icon-loading"></i>
          {{ loading ? '登录中...' : '登录' }}
        </button>
        
        <div v-if="error" class="error-message">
          <i class="icon-error"></i>
          {{ error }}
        </div>
      </form>
      
      <div class="login-footer">
        <p>还没有账号？<router-link to="/register">立即注册</router-link></p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/user-api'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const formData = reactive({
      username: '',
      password: '',
      rememberMe: false
    })
    
    const loading = ref(false)
    const error = ref('')
    const showPassword = ref(false)

    // Check for saved credentials on mount
    onMounted(() => {
      const savedUsername = localStorage.getItem('savedUsername')
      const savedPassword = localStorage.getItem('savedPassword')
      const rememberMe = localStorage.getItem('rememberMe') === 'true'
      
      if (rememberMe && savedUsername) {
        formData.username = savedUsername
        if (savedPassword) {
          try {
            // Simple base64 decoding
            formData.password = atob(savedPassword)
          } catch (e) {
            console.error('Failed to decode password', e)
          }
        }
        formData.rememberMe = true
      }
    })
    
    const handleLogin = async () => {
      error.value = ''
      loading.value = true
      
      try {
        const response = await userApi.login(
          formData.username,
          formData.password
        )
        
        if (response.success) {
          store.dispatch('login', {
            user: response.data.user,
            token: response.data.token
          })
          
          if (formData.rememberMe) {
            localStorage.setItem('rememberMe', 'true')
            localStorage.setItem('savedUsername', formData.username)
            // Simple base64 encoding
            localStorage.setItem('savedPassword', btoa(formData.password))
          } else {
            localStorage.removeItem('rememberMe')
            localStorage.removeItem('savedUsername')
            localStorage.removeItem('savedPassword')
          }
          
          // Redirect to home for all users
          router.push('/')
        } else {
          error.value = response.message || '登录失败，请重试'
        }
      } catch (err) {
        error.value = err.message || '网络错误，请检查您的连接'
      } finally {
        loading.value = false
      }
    }
    
    return {
      formData,
      loading,
      error,
      showPassword,
      handleLogin
    }
  }
}
</script>

<style scoped lang="less">
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  padding: 20px;
  z-index: 2000; /* Ensure it covers everything */
}

.login-container {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 22px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 40px 36px;
  width: 100%;
  max-width: 420px;
  backdrop-filter: blur(14px);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  
  .platform-title {
    font-size: 20px;
    color: rgba(15, 23, 42, 0.78);
    font-weight: 700;
    margin-bottom: 16px;
    letter-spacing: 0.6px;
  }
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    color: var(--text);
    margin-bottom: 12px;
    letter-spacing: -0.03em;
  }
  
  p {
    font-size: 16px;
    color: var(--text-2);
  }
}

.login-form {
  .form-group {
    margin-bottom: 24px;
    
    label {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: var(--text);
      margin-bottom: 8px;
    }
    
    input[type="text"],
    input[type="password"] {
      width: 100%;
      padding: 12px 16px;
      border: 1px solid var(--border);
      border-radius: 14px;
      font-size: 14px;
      background: rgba(255, 255, 255, 0.78);
      transition: box-shadow var(--transition), border-color var(--transition), background var(--transition);
      
      &:focus {
        outline: none;
        border-color: rgba(79, 109, 255, 0.55);
        box-shadow: var(--focus);
        background: rgba(255, 255, 255, 0.96);
      }
      
      &::placeholder {
        color: var(--text-3);
      }
    }
    
    .password-input-wrapper {
      position: relative;
      
      input {
        padding-right: 40px;
      }
      
      .toggle-password {
        position: absolute;
        right: 12px;
        top: 50%;
        transform: translateY(-50%);
        background: none;
        border: none;
        cursor: pointer;
        color: var(--text-3);
        padding: 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: color 0.3s;
        
        &:hover {
          color: rgba(15, 23, 42, 0.86);
        }
        
        i {
          font-size: 18px;
        }
      }
    }
    
    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      
      input[type="checkbox"] {
        width: 18px;
        height: 18px;
        cursor: pointer;
      }
      
      span {
        font-size: 14px;
        color: var(--text-2);
      }
    }
  }
  
  .btn-block {
    width: 100%;
    padding: 14px;
    font-size: 16px;
    font-weight: 600;
    margin-top: 32px;
  }
  
  .error-message {
    margin-top: 20px;
    padding: 12px 16px;
    background: rgba(194, 71, 77, 0.10);
    border: 1px solid rgba(194, 71, 77, 0.18);
    border-radius: 14px;
    color: rgba(15, 23, 42, 0.86);
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  
  p {
    font-size: 14px;
    color: var(--text-2);
    
    a {
      color: rgba(15, 23, 42, 0.86);
      text-decoration: none;
      font-weight: 600;
      
      &:hover {
        color: rgba(15, 23, 42, 1);
      }
    }
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 28px 18px;
    border-radius: 18px;
  }
}
</style>
