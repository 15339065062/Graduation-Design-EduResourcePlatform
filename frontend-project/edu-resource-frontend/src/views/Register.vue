<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h2 class="platform-title">教育资源共享平台</h2>
        <h1>创建账号</h1>
        <p>加入我们的教育资源平台</p>
      </div>
      
      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名 *</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            placeholder="请选择用户名"
            required
            minlength="3"
          />
          <span class="form-hint">至少3个字符</span>
        </div>
        
        <div class="form-group">
          <label for="password">密码 *</label>
          <input
            id="password"
            v-model="formData.password"
            type="password"
            placeholder="创建密码"
            required
            minlength="6"
          />
          <span class="form-hint">至少6个字符</span>
        </div>
        
        <div class="form-group">
          <label for="confirmPassword">确认密码 *</label>
          <input
            id="confirmPassword"
            v-model="formData.confirmPassword"
            type="password"
            placeholder="确认密码"
            required
          />
          <span v-if="passwordMismatch" class="form-error">密码不匹配</span>
        </div>
        
        <div class="form-group">
          <label for="nickname">昵称</label>
          <input
            id="nickname"
            v-model="formData.nickname"
            type="text"
            placeholder="您的显示名称"
          />
        </div>
        
        <div class="form-group">
          <label for="phone">手机号码 *</label>
          <input
            id="phone"
            v-model="formData.phone"
            type="tel"
            placeholder="请输入手机号码"
            required
            pattern="[0-9]{11}"
          />
          <span class="form-hint">11位手机号码</span>
        </div>
        
        <div class="form-group">
          <label for="role">角色 *</label>
          <select id="role" v-model="formData.role" required>
            <option value="">选择您的角色</option>
            <option value="student">学生</option>
            <option value="teacher">老师</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="formData.agreeTerms" type="checkbox" required />
            <span>我同意 <a href="#" @click.prevent>服务条款</a> 和 <a href="#" @click.prevent>隐私政策</a></span>
          </label>
        </div>
        
        <button type="submit" class="btn btn-primary btn-cta btn-block" :disabled="loading || !isFormValid">
          <i v-if="loading" class="icon-loading"></i>
          {{ loading ? '创建账号中...' : '创建账号' }}
        </button>
        
        <div v-if="error" class="error-message">
          <i class="icon-error"></i>
          {{ error }}
        </div>
      </form>
      
      <div class="register-footer">
        <p>已有账号？<router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/user-api'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const formData = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      phone: '',
      role: '',
      agreeTerms: false
    })
    
    const loading = ref(false)
    const error = ref('')
    
    const passwordMismatch = computed(() => {
      return formData.confirmPassword && formData.password !== formData.confirmPassword
    })
    
    const isFormValid = computed(() => {
      return formData.username.length >= 3 &&
             formData.password.length >= 6 &&
             formData.password === formData.confirmPassword &&
             formData.phone.length === 11 &&
             formData.role &&
             formData.agreeTerms
    })
    
    const handleRegister = async () => {
      if (passwordMismatch.value) {
        error.value = '密码不匹配'
        return
      }
      
      if (!isFormValid.value) {
        error.value = '请正确填写所有必填字段'
        return
      }
      
      error.value = ''
      loading.value = true
      
      try {
        const response = await userApi.register({
          username: formData.username,
          password: formData.password,
          nickname: formData.nickname || formData.username,
          phone: formData.phone,
          role: formData.role
        })
        
        if (response.success) {
          // 注册成功后跳转到登录页面
          router.push('/login')
        } else {
          error.value = response.message || '注册失败，请重试'
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
      passwordMismatch,
      isFormValid,
      handleRegister
    }
  }
}
</script>

<style scoped lang="less">
.register-page {
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

.register-container {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 22px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 40px 36px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  backdrop-filter: blur(14px);
}

.register-header {
    text-align: center;
    margin-bottom: 32px;
    
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

.register-form {
  .form-group {
    margin-bottom: 20px;
    
    label {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: var(--text);
      margin-bottom: 8px;
      
      a {
        color: rgba(15, 23, 42, 0.86);
        text-decoration: none;
        
        &:hover {
          color: rgba(15, 23, 42, 1);
        }
      }
    }
    
    input[type="text"],
    input[type="password"],
    input[type="tel"],
    select {
      width: 100%;
      padding: 12px 16px;
      border: 1px solid var(--border);
      border-radius: 14px;
      font-size: 14px;
      transition: box-shadow var(--transition), border-color var(--transition), background var(--transition);
      background: rgba(255, 255, 255, 0.78);
      
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
    
    .form-hint {
      display: block;
      font-size: 12px;
      color: var(--text-3);
      margin-top: 4px;
    }
    
    .form-error {
      display: block;
      font-size: 12px;
      color: var(--danger);
      margin-top: 4px;
    }
    
    .checkbox-label {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      cursor: pointer;
      
      input[type="checkbox"] {
        width: 18px;
        height: 18px;
        cursor: pointer;
        margin-top: 2px;
      }
      
      span {
        font-size: 13px;
        color: var(--text-2);
        line-height: 1.5;
      }
    }
  }
  
  .btn-block {
    width: 100%;
    padding: 14px;
    font-size: 16px;
    font-weight: 600;
    margin-top: 24px;
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

.register-footer {
  text-align: center;
  margin-top: 24px;
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
  .register-container {
    padding: 28px 18px;
    border-radius: 18px;
  }
}
</style>
