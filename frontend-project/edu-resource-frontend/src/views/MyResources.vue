<template>
  <div class="my-resources-page">
    <div class="container">
      <div class="page-header">
        <h2>我的资源</h2>
        <router-link v-if="canUpload" to="/upload" class="btn btn-primary">
          <i class="icon-upload"></i> 上传资源
        </router-link>
      </div>
      
      <div v-if="loading" class="loading-state">
        <i class="icon-loading"></i>
        <p>正在加载资源...</p>
      </div>
      
      <div v-else-if="resources.length === 0" class="empty-state">
        <i class="icon-folder-open"></i>
        <p>暂无上传资源</p>
        <router-link v-if="canUpload" to="/upload" class="btn btn-primary">
          上传资源
        </router-link>
      </div>
      
      <div v-else class="resources-grid">
        <div
          v-for="resource in resources"
          :key="resource.id"
          class="resource-card"
          @click="handleOpenResource(resource.id)"
        >
          <div class="resource-icon">
            <i :class="getResourceIcon(resource.fileType)"></i>
          </div>
          <div class="resource-info">
            <h4>{{ resource.name }}</h4>
            <p>{{ resource.description }}</p>
            <div class="resource-meta">
              <span><i class="icon-download"></i> {{ resource.downloadCount }}</span>
              <span><i class="icon-calendar"></i> {{ formatDate(resource.createdAt) }}</span>
            </div>
          </div>
          <div class="resource-actions">
            <button class="btn btn-sm btn-edit" @click.stop="handleEditResource(resource)">
              <i class="icon-edit"></i>
            </button>
            <button class="btn btn-sm btn-delete" @click.stop="handleDeleteResource(resource.id)">
              <i class="icon-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { resourceApi } from '../api/resource-api'

export default {
  name: 'MyResources',
  setup() {
    const router = useRouter()
    const store = useStore()
    const resources = ref([])
    const loading = ref(false)
    
    const canUpload = computed(() => store.getters.canUpload)
    
    const loadMyResources = async () => {
      loading.value = true
      try {
        const response = await resourceApi.getMyResources({ page: 1, pageSize: 100 })
        if (response.success) {
          resources.value = response.data?.list ?? response.data ?? []
        }
      } catch (err) {
        console.error('Failed to load resources:', err)
      } finally {
        loading.value = false
      }
    }
    
    const handleEditResource = (resource) => {
      alert('编辑资源功能即将上线！')
    }

    const handleOpenResource = (id) => {
      router.push(`/resources/${id}`)
    }
    
    const handleDeleteResource = async (id) => {
      if (!confirm('确定要删除这个资源吗？')) return
      
      try {
        const response = await resourceApi.deleteResource(id)
        if (response.success) {
          resources.value = resources.value.filter(r => r.id !== id)
          alert('资源删除成功！')
        } else {
          alert(response.message || '删除资源失败')
        }
      } catch (err) {
        alert('网络错误，请稍后重试')
      }
    }
    
    const getResourceIcon = (fileType) => {
      const icons = {
        pdf: 'icon-file-pdf',
        doc: 'icon-file-word',
        docx: 'icon-file-word',
        ppt: 'icon-file-ppt',
        pptx: 'icon-file-ppt',
        xls: 'icon-file-excel',
        xlsx: 'icon-file-excel',
        jpg: 'icon-file-image',
        jpeg: 'icon-file-image',
        png: 'icon-file-image',
        gif: 'icon-file-image',
        mp4: 'icon-file-video',
        avi: 'icon-file-video',
        mov: 'icon-file-video'
      }
      return icons[fileType?.toLowerCase()] || 'icon-file'
    }
    
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }
    
    onMounted(() => {
      loadMyResources()
    })
    
    return {
      resources,
      loading,
      canUpload,
      handleEditResource,
      handleOpenResource,
      handleDeleteResource,
      getResourceIcon,
      formatDate
    }
  }
}
</script>

<style scoped lang="less">
.my-resources-page {
  min-height: 100vh;
  background: transparent;
  padding: 28px 0 56px;
  
  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
    
    h2 {
      font-size: 24px;
      font-weight: 600;
      color: var(--text);
    }
  }
  
  .loading-state, .empty-state {
    text-align: center;
    padding: 60px 0;
    
    i {
      font-size: 48px;
      color: rgba(15, 23, 42, 0.18);
      margin-bottom: 16px;
    }
    
    p {
      color: var(--text-3);
    }
  }
  
  .resources-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
    
    .resource-card {
      background: rgba(255, 255, 255, 0.82);
      border-radius: 18px;
      padding: 18px;
      border: 1px solid rgba(15, 23, 42, 0.10);
      box-shadow: var(--shadow-md);
      transition: transform var(--transition), box-shadow var(--transition), border-color var(--transition);
      cursor: pointer;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-lg);
        border-color: rgba(15, 23, 42, 0.14);
      }
      
      .resource-icon {
        width: 48px;
        height: 48px;
        background: rgba(79, 109, 255, 0.10);
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
        
        i {
          font-size: 24px;
          color: rgba(15, 23, 42, 0.82);
        }
      }
      
      .resource-info {
        margin-bottom: 20px;
        
        h4 {
          font-size: 16px;
          font-weight: 600;
          color: var(--text);
          margin-bottom: 8px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        p {
          font-size: 14px;
          color: var(--text-2);
          line-height: 1.5;
          height: 42px;
          overflow: hidden;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
        
        .resource-meta {
          display: flex;
          gap: 16px;
          margin-top: 12px;
          font-size: 12px;
          color: var(--text-3);
          
          span {
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }
      }
      
      .resource-actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        padding-top: 16px;
        border-top: 1px solid rgba(15, 23, 42, 0.08);
        
        .btn-sm {
          width: 32px;
          height: 32px;
          padding: 0;
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
          border: none;
          cursor: pointer;
          transition: all 0.2s;
          
          &.btn-edit {
            background: #e2e8f0;
            color: #4a5568;
            &:hover { background: #cbd5e0; }
          }
          
          &.btn-delete {
            background: #fed7d7;
            color: #e53e3e;
            &:hover { background: #feb2b2; }
          }
        }
      }
    }
  }
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #667eea;
  color: white;
  padding: 8px 16px;
  border-radius: 6px;
  text-decoration: none;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
  
  &:hover {
    background: #5a67d8;
  }
}
</style>
