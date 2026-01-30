<template>
  <div class="upload-page">
    <div class="container">
      <div class="upload-container">
        <div class="upload-header">
          <h1>上传资源</h1>
          <p>与社区分享您的教育资源</p>
        </div>
        
        <form class="upload-form" @submit.prevent="handleSubmit">
          <div class="form-section">
            <h2>基本信息</h2>
            
            <div class="form-group">
              <label for="resourceName">资源名称 *</label>
              <input
                id="resourceName"
                v-model="formData.name"
                type="text"
                placeholder="请输入资源的描述性名称"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="resourceCategory">分类 *</label>
              <select id="resourceCategory" v-model="formData.category" required>
                <option value="">选择分类</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.name">
                  {{ cat.name }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="resourceDescription">描述 *</label>
              <textarea
                id="resourceDescription"
                v-model="formData.description"
                placeholder="详细描述您的资源"
                rows="4"
                required
              ></textarea>
            </div>
          </div>
          
          <div class="form-section">
            <h2>文件上传</h2>
            
            <div
              class="upload-zone"
              :class="{ 'drag-over': isDragOver, 'has-file': selectedFiles.length > 0 }"
              @dragover.prevent="handleDragOver"
              @dragleave.prevent="handleDragLeave"
              @drop.prevent="handleDrop"
              @click="handleFileSelect"
            >
              <input
                ref="fileInput"
                type="file"
                multiple
                accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.jpg,.jpeg,.png,.gif,.mp4,.avi,.mov,.mp3,.wav"
                @change="handleFileChange"
                style="display: none"
              />
              
              <div v-if="selectedFiles.length === 0" class="upload-prompt">
                <i class="icon-cloud-upload"></i>
                <h3>将文件拖放到此处</h3>
                <p>或点击浏览文件</p>
                <span class="file-types">支持格式：PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, JPG, PNG, GIF, MP4, AVI, MOV, MP3, WAV</span>
              </div>
              
              <div v-else class="file-preview-list">
                <div v-for="(file, index) in selectedFiles" :key="index" class="file-preview">
                  <div class="file-icon">
                    <i :class="getFileIcon(file.type)"></i>
                  </div>
                  <div class="file-info">
                    <h4>{{ file.name }}</h4>
                    <p>{{ formatFileSize(file.size) }}</p>
                  </div>
                  <button type="button" class="btn-remove" @click.stop="handleRemoveFile(index)">
                    <i class="icon-close"></i>
                  </button>
                </div>
                <div class="file-count-info">
                  <p>已选择 {{ selectedFiles.length }} 个文件</p>
                </div>
              </div>
            </div>
            
            <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <p>上传中... {{ uploadProgress }}%</p>
            </div>
          </div>
          
          <div class="form-section">
            <h2>附加设置</h2>
            
            <div class="form-group">
              <label class="checkbox-label">
                <input v-model="formData.allowComments" type="checkbox" />
                <span>允许对该资源评论</span>
              </label>
            </div>
            
            <div class="form-group">
              <label class="checkbox-label">
                <input v-model="formData.isPublic" type="checkbox" checked />
                <span>公开该资源（对所有用户可见）</span>
              </label>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleCancel">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="loading || !isFormValid">
              <i v-if="loading" class="icon-loading"></i>
              {{ loading ? '上传中...' : '上传资源' }}
            </button>
          </div>
          
          <div v-if="error" class="error-message">
          <i class="icon-error"></i>
          {{ error }}
        </div>
        
        <div v-if="success" class="success-message">
          <i class="icon-check"></i>
          {{ success }}
        </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { resourceApi } from '../api/resource-api'

export default {
  name: 'ResourceUpload',
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const formData = reactive({
      name: '',
      category: '',
      description: '',
      allowComments: true,
      isPublic: true
    })
    
    const selectedFiles = ref([])
    const isDragOver = ref(false)
    const loading = ref(false)
    const uploadProgress = ref(0)
    const error = ref('')
    const success = ref('')
    const fileInput = ref(null)
    
    const categories = [
      { id: 1, name: '数学' },
      { id: 2, name: '科学' },
      { id: 3, name: '语言' },
      { id: 4, name: '历史' },
      { id: 5, name: '技术' },
      { id: 6, name: '艺术' }
    ]
    
    const isFormValid = computed(() => {
      return formData.name.trim() &&
             formData.category &&
             formData.description.trim() &&
             selectedFiles.value.length > 0
    })
    
    const handleDragOver = () => {
      isDragOver.value = true
    }
    
    const handleDragLeave = () => {
      isDragOver.value = false
    }
    
    const handleDrop = (e) => {
      isDragOver.value = false
      const files = e.dataTransfer.files
      if (files.length > 0) {
        for (let i = 0; i < files.length; i++) {
          validateAndSetFile(files[i])
        }
      }
    }
    
    const handleFileSelect = () => {
      fileInput.value.click()
    }
    
    const handleFileChange = (e) => {
      const files = e.target.files
      if (files.length > 0) {
        for (let i = 0; i < files.length; i++) {
          validateAndSetFile(files[i])
        }
      }
    }
    
    const validateAndSetFile = (file) => {
      const maxSize = 100 * 1024 * 1024
      const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'image/jpeg',
        'image/png',
        'image/gif',
        'video/mp4',
        'video/avi',
        'video/quicktime',
        'audio/mpeg',
        'audio/mp3',
        'audio/wav',
        'audio/ogg'
      ]

      const nameParts = (file.name || '').split('.')
      const ext = nameParts.length > 1 ? nameParts[nameParts.length - 1].toLowerCase() : ''
      const allowedExtensions = [
        'pdf',
        'doc',
        'docx',
        'ppt',
        'pptx',
        'xls',
        'xlsx',
        'jpg',
        'jpeg',
        'png',
        'gif',
        'mp4',
        'avi',
        'mov',
        'mp3',
        'wav'
      ]
      
      const isAllowedByMime = file.type && allowedTypes.includes(file.type)
      const isAllowedByExt = ext && allowedExtensions.includes(ext)
      if (!isAllowedByMime && !isAllowedByExt) {
        error.value = `文件类型不支持: ${file.name}。请上传有效的文件。`
        return
      }
      
      if (file.size > maxSize) {
        error.value = `文件大小超过100MB限制: ${file.name}。`
        return
      }
      
      // 检查文件是否已存在
      const exists = selectedFiles.value.some(f => f.name === file.name && f.size === file.size)
      if (!exists) {
        selectedFiles.value.push(file)
        error.value = ''
        
        if (!formData.name && selectedFiles.value.length === 1) {
          formData.name = file.name.replace(/\.[^/.]+$/, '')
        }
      }
    }
    
    const handleRemoveFile = (index) => {
      selectedFiles.value.splice(index, 1)
      if (selectedFiles.value.length === 0 && fileInput.value) {
        fileInput.value.value = ''
      }
    }
    
    const handleSubmit = async () => {
      if (!isFormValid.value) {
        error.value = '请填写所有必填字段并选择文件。'
        return
      }
      
      loading.value = true
      uploadProgress.value = 0
      error.value = ''
      
      try {
        // 逐个上传文件
        let uploadedCount = 0
        const totalFiles = selectedFiles.value.length
        
        for (let i = 0; i < totalFiles; i++) {
          const file = selectedFiles.value[i]
          const formDataToSend = new FormData()
          formDataToSend.append('file', file)
          formDataToSend.append('name', formData.name || file.name.replace(/\.[^/.]+$/, ''))
          formDataToSend.append('category', formData.category)
          formDataToSend.append('description', formData.description)
          formDataToSend.append('allowComments', formData.allowComments)
          formDataToSend.append('isPublic', formData.isPublic)
          
          const response = await resourceApi.uploadResource(formDataToSend, (progress) => {
            uploadProgress.value = Math.floor(((uploadedCount * 100) + progress) / totalFiles)
          })
          
          if (!response.success) {
            throw new Error(`文件 ${file.name} 上传失败: ${response.message || '未知错误'}`)
          }
          
          uploadedCount++
          uploadProgress.value = Math.floor((uploadedCount / totalFiles) * 100)
        }
        
        success.value = `成功上传 ${uploadedCount} 个资源！`
        // 延迟跳转，让用户看到成功消息
        setTimeout(() => {
          router.push('/my-resources')
        }, 1500)
      } catch (err) {
        error.value = err.message || '网络错误。请检查您的连接并重试。'
      } finally {
        loading.value = false
        uploadProgress.value = 0
      }
    }
    
    const handleCancel = () => {
      if (formData.name || formData.description || selectedFiles.value.length > 0) {
        if (confirm('确定要取消吗？所有未保存的更改都将丢失。')) {
          router.back()
        }
      } else {
        router.back()
      }
    }
    
    const getFileIcon = (mimeType) => {
      const icons = {
        'application/pdf': 'icon-file-pdf',
        'application/msword': 'icon-file-word',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'icon-file-word',
        'application/vnd.ms-powerpoint': 'icon-file-ppt',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'icon-file-ppt',
        'application/vnd.ms-excel': 'icon-file-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'icon-file-excel',
        'image/jpeg': 'icon-file-image',
        'image/png': 'icon-file-image',
        'image/gif': 'icon-file-image',
        'video/mp4': 'icon-file-video',
        'video/avi': 'icon-file-video',
        'video/quicktime': 'icon-file-video',
        'audio/mpeg': 'icon-file-video',
        'audio/mp3': 'icon-file-video',
        'audio/wav': 'icon-file-video',
        'audio/ogg': 'icon-file-video'
      }
      return icons[mimeType] || 'icon-file'
    }
    
    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }
    
    return {
      formData,
      selectedFiles,
      isDragOver,
      loading,
      uploadProgress,
      error,
      success,
      fileInput,
      categories,
      isFormValid,
      handleDragOver,
      handleDragLeave,
      handleDrop,
      handleFileSelect,
      handleFileChange,
      handleRemoveFile,
      handleSubmit,
      handleCancel,
      getFileIcon,
      formatFileSize
    }
  }
}
</script>

<style scoped lang="less">
.upload-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}

.upload-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.upload-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px;
  text-align: center;
  color: white;
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 12px;
  }
  
  p {
    font-size: 16px;
    opacity: 0.9;
  }
}

.upload-form {
  padding: 40px;
  
  .form-section {
    margin-bottom: 40px;
    
    &:last-of-type {
      margin-bottom: 0;
    }
    
    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 2px solid #f0f0f0;
    }
    
    .form-group {
      margin-bottom: 24px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      label {
        display: block;
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 8px;
      }
      
      input[type="text"],
      select,
      textarea {
        width: 100%;
        padding: 12px 16px;
        border: 2px solid #e0e0e0;
        border-radius: 8px;
        font-size: 14px;
        transition: all 0.3s;
        font-family: inherit;
        
        &:focus {
          outline: none;
          border-color: #667eea;
          box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        
        &::placeholder {
          color: #999;
        }
      }
      
      textarea {
        resize: vertical;
        min-height: 100px;
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
          color: #666;
        }
      }
    }
  }
  
  .upload-zone {
    border: 2px dashed #e0e0e0;
    border-radius: 12px;
    padding: 48px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;
    background: #fafafa;
    
    &:hover {
      border-color: #667eea;
      background: #f5f7fa;
    }
    
    &.drag-over {
      border-color: #667eea;
      background: #e3f2fd;
    }
    
    &.has-file {
      border-style: solid;
      padding: 24px;
    }
    
    .upload-prompt {
      i {
        font-size: 64px;
        color: #667eea;
        margin-bottom: 16px;
        display: block;
      }
      
      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
      }
      
      p {
        font-size: 14px;
        color: #666;
        margin-bottom: 12px;
      }
      
      .file-types {
        font-size: 12px;
        color: #999;
      }
    }
    
    .file-preview-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .file-preview {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 16px;
        background: #fafafa;
        border-radius: 8px;
        border: 1px solid #e0e0e0;
        
        .file-icon {
          width: 48px;
          height: 48px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          
          i {
            font-size: 24px;
            color: white;
          }
        }
        
        .file-info {
          flex: 1;
          text-align: left;
          
          h4 {
            font-size: 14px;
            font-weight: 600;
            color: #333;
            margin-bottom: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
          
          p {
            font-size: 12px;
            color: #666;
          }
        }
        
        .btn-remove {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background: #ffebee;
          color: #f44336;
          border: none;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.3s;
          
          &:hover {
            background: #f44336;
            color: white;
          }
        }
      }
      
      .file-count-info {
        padding: 12px;
        background: #e3f2fd;
        border: 1px solid #bbdefb;
        border-radius: 8px;
        text-align: center;
        
        p {
          font-size: 14px;
          color: #1976d2;
          font-weight: 500;
        }
      }
    }
  }
  
  .upload-progress {
    margin-top: 16px;
    
    .progress-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 8px;
      
      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
        transition: width 0.3s;
      }
    }
    
    p {
      font-size: 14px;
      color: #666;
      text-align: center;
    }
  }
  
  .form-actions {
    display: flex;
    gap: 16px;
    justify-content: flex-end;
    margin-top: 40px;
    padding-top: 32px;
    border-top: 2px solid #f0f0f0;
  }
  
  .error-message {
    margin-top: 24px;
    padding: 16px;
    background: #fee;
    border: 1px solid #fcc;
    border-radius: 8px;
    color: #c33;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .success-message {
    margin-top: 24px;
    padding: 16px;
    background: #efe;
    border: 1px solid #cfc;
    border-radius: 8px;
    color: #3a3;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .upload-header {
    padding: 32px 20px;
    
    h1 {
      font-size: 24px;
    }
  }
  
  .upload-form {
    padding: 24px;
    
    .form-actions {
      flex-direction: column;
      
      .btn {
        width: 100%;
      }
    }
  }
}
</style>
