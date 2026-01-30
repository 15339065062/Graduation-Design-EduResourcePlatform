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
        
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading || !isFormValid">
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  z-index: 2000; /* Ensure it covers everything */
}

.register-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 48px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
    text-align: center;
    margin-bottom: 32px;
    
    .platform-title {
      font-size: 20px;
      color: #667eea;
      font-weight: 600;
      margin-bottom: 16px;
      letter-spacing: 1px;
    }
    
    h1 {
    font-size: 32px;
    font-weight: 700;
    color: #333;
    margin-bottom: 12px;
  }
  
  p {
    font-size: 16px;
    color: #666;
  }
}

.register-form {
  .form-group {
    margin-bottom: 20px;
    
    label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 8px;
      
      a {
        color: #667eea;
        text-decoration: none;
        
        &:hover {
          text-decoration: underline;
        }
      }
    }
    
    input[type="text"],
    input[type="password"],
    input[type="tel"],
    select {
      width: 100%;
      padding: 12px 16px;
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      font-size: 14px;
      transition: all 0.3s;
      background: white;
      
      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }
      
      &::placeholder {
        color: #999;
      }
    }
    
    .form-hint {
      display: block;
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
    
    .form-error {
      display: block;
      font-size: 12px;
      color: #c33;
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
        color: #666;
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
    background: #fee;
    border: 1px solid #fcc;
    border-radius: 8px;
    color: #c33;
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
  border-top: 1px solid #eee;
  
  p {
    font-size: 14px;
    color: #666;
    
    a {
      color: #667eea;
      text-decoration: none;
      font-weight: 600;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
