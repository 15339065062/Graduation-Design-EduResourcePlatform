<template>
  <div class="my-collections-page">
    <div class="container">
      <div class="page-header">
        <h2>我的收藏</h2>
        <router-link to="/resources" class="btn btn-primary">
          浏览更多资源
        </router-link>
      </div>
      
      <div v-if="loading" class="loading-state">
        <i class="icon-loading"></i>
        <p>正在加载收藏...</p>
      </div>
      
      <div v-else-if="collections.length === 0" class="empty-state">
        <i class="icon-star"></i>
        <p>暂无收藏</p>
        <router-link to="/resources" class="btn btn-primary">
          去浏览
        </router-link>
      </div>
      
      <div v-else class="resources-grid">
        <div v-for="item in collections" :key="item.id" class="resource-card">
          <div class="resource-icon" @click="handleViewResource(item.id)">
            <i :class="getResourceIcon(item.fileType)"></i>
          </div>
          <div class="resource-info">
            <h4 @click="handleViewResource(item.id)">{{ item.name }}</h4>
            <p @click="handleViewResource(item.id)">{{ item.description }}</p>
            <div class="resource-meta">
              <span><i class="icon-user"></i> {{ item.uploader?.nickname || item.uploader?.username }}</span>
              <span><i class="icon-calendar"></i> 发布于 {{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="resource-actions">
            <button class="btn btn-sm btn-view" @click="handleViewResource(item.id)">
              <i class="icon-eye"></i>
            </button>
            <button class="btn btn-sm btn-uncollect" @click="handleUncollect(item.id)">
              <i class="icon-star-off"></i>
            </button>
          </div>
        </div>
      </div>
      
      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button 
          class="btn-page" 
          :disabled="currentPage === 1"
          @click="changePage(currentPage - 1)"
        >
          <i class="icon-chevron-left"></i>
        </button>
        
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        
        <button 
          class="btn-page" 
          :disabled="currentPage === totalPages"
          @click="changePage(currentPage + 1)"
        >
          <i class="icon-chevron-right"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { collectionApi } from '../api/collection-api'

export default {
  name: 'MyCollections',
  setup() {
    const router = useRouter()
    const collections = ref([])
    const loading = ref(false)
    const currentPage = ref(1)
    const totalPages = ref(1)
    const pageSize = 20
    const collectionChannel = new BroadcastChannel('collection_channel')
    let pollInterval = null
    
    const loadCollections = async (page = 1) => {
      loading.value = true
      try {
        console.log(`Loading collections page ${page}`)
        const response = await collectionApi.getCollections(page, pageSize)
        console.log('Collection response:', response)
        
        if (response.success) {
          // Backend returns { list, total, page, pageSize }
          if (response.data && response.data.list) {
             console.log('Collections list:', response.data.list)
             collections.value = response.data.list
             const total = response.data.total
             totalPages.value = Math.ceil(total / pageSize)
             
             // If current page is empty and we are not on page 1, go to previous page
             if (collections.value.length === 0 && page > 1 && total > 0) {
                 changePage(Math.max(1, totalPages.value))
             } else {
                 currentPage.value = page
             }
          } else if (Array.isArray(response.data)) {
             // Fallback for old API structure
             console.log('Collections array (old format):', response.data)
             collections.value = response.data
          } else {
             console.warn('Unexpected data format:', response.data)
             collections.value = []
          }
        } else {
          console.error('Failed to load collections:', response.message)
        }
      } catch (err) {
        console.error('Failed to load collections:', err)
      } finally {
        loading.value = false
      }
    }
    
    const changePage = (page) => {
      if (page >= 1 && page <= totalPages.value) {
        loadCollections(page)
        window.scrollTo({ top: 0, behavior: 'smooth' })
      }
    }
    
    const handleViewResource = (id) => {
      router.push(`/resources/${id}`)
    }
    
    const handleUncollect = async (resourceId) => {
      // Optimistic remove
      const previousList = [...collections.value]
      collections.value = collections.value.filter(c => c.id !== resourceId)
      
      try {
        const response = await collectionApi.toggleCollection(resourceId)
        if (response.success) {
          // Notify other tabs
          collectionChannel.postMessage({
            type: 'collection_updated',
            resourceId: resourceId,
            isCollected: false,
            count: response.data.count
          })
          
          alert('已取消收藏！')
        } else {
          // Revert
          collections.value = previousList
          alert(response.message || '取消收藏失败')
        }
      } catch (err) {
        collections.value = previousList
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
    
    const startPolling = () => {
      stopPolling()
      if (!document.hidden) {
        pollInterval = setInterval(() => loadCollections(currentPage.value), 10000) // Poll every 10s for list
      }
    }
    
    const stopPolling = () => {
      if (pollInterval) {
        clearInterval(pollInterval)
        pollInterval = null
      }
    }
    
    onMounted(() => {
      loadCollections()
      startPolling()
      
      collectionChannel.onmessage = (event) => {
        if (event.data.type === 'collection_updated') {
           loadCollections(currentPage.value)
        }
      }
    })
    
    onActivated(() => {
      loadCollections(currentPage.value)
    })
    
    onUnmounted(() => {
      stopPolling()
      collectionChannel.close()
    })
    
    return {
      collections,
      loading,
      currentPage,
      totalPages,
      changePage,
      handleViewResource,
      handleUncollect,
      getResourceIcon,
      formatDate
    }
  }
}
</script>

<style scoped lang="less">
.my-collections-page {
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
        cursor: pointer;
        transition: transform 0.2s;
        
        &:hover {
          transform: scale(1.05);
        }
        
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
          cursor: pointer;
          transition: color 0.2s;
          
          &:hover {
            color: rgba(15, 23, 42, 0.92);
          }
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
          cursor: pointer;
          
          &:hover {
            color: rgba(15, 23, 42, 0.78);
          }
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
          
          &.btn-view {
            background: rgba(15, 23, 42, 0.06);
            color: rgba(15, 23, 42, 0.72);
            &:hover { background: rgba(15, 23, 42, 0.08); }
          }
          
          &.btn-uncollect {
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
  background: linear-gradient(135deg, rgba(79, 109, 255, 0.96) 0%, rgba(123, 92, 255, 0.96) 100%);
  color: white;
  padding: 8px 16px;
  border-radius: 12px;
  text-decoration: none;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: transform var(--transition), box-shadow var(--transition);
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(79, 109, 255, 0.22);
  }
  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid #edf2f7;
    
    .btn-page {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      border: 1px solid #e2e8f0;
      background: rgba(255, 255, 255, 0.82);
      color: rgba(15, 23, 42, 0.72);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover:not(:disabled) {
        border-color: rgba(15, 23, 42, 0.18);
        color: rgba(15, 23, 42, 0.92);
        background: rgba(15, 23, 42, 0.04);
      }
      
      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        background: rgba(15, 23, 42, 0.04);
      }
      
      i {
        font-size: 16px;
      }
    }
    
    .page-info {
      font-size: 14px;
      font-weight: 500;
      color: rgba(15, 23, 42, 0.72);
    }
  }
}
</style>
