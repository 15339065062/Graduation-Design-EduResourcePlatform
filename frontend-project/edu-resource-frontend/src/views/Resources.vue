<template>
  <div class="resources-page">
    <div class="container">
      <div class="page-header">
        <h1>资源</h1>
        <router-link v-if="canUpload" to="/upload" class="btn btn-primary">
          <i class="icon-upload"></i> 上传资源
        </router-link>
      </div>
      
      <div class="filters-section">
        <div class="search-box">
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="搜索资源..."
            @input="handleSearch"
            @keyup.enter="handleSearchImmediate"
          />
          <i class="icon-search"></i>
        </div>
        
        <div class="filter-options">
          <select v-model="filters.category" @change="handleFilterChange">
            <option value="">全部学科</option>
            <option v-for="cat in categories" :key="cat" :value="cat">
              {{ cat }}
            </option>
          </select>

          <select v-model="filters.fileType" @change="handleFilterChange">
            <option value="">全部类型</option>
            <option value="pdf">PDF</option>
            <option value="doc">DOC</option>
            <option value="docx">DOCX</option>
            <option value="ppt">PPT</option>
            <option value="pptx">PPTX</option>
            <option value="xls">XLS</option>
            <option value="xlsx">XLSX</option>
            <option value="jpg">JPG</option>
            <option value="png">PNG</option>
            <option value="gif">GIF</option>
            <option value="mp4">MP4</option>
            <option value="mov">MOV</option>
            <option value="mp3">MP3</option>
            <option value="wav">WAV</option>
          </select>
          
          <select v-model="filters.sortBy" @change="handleFilterChange">
            <option value="latest">最新</option>
            <option value="recommend">推荐</option>
            <option value="popular">最受欢迎</option>
            <option value="downloads">最多下载</option>
          </select>
        </div>
      </div>
      
      <div v-if="loading" class="loading-state">
        <i class="icon-loading"></i>
        <p>正在加载资源...</p>
      </div>
      
      <div v-else-if="resources.length === 0" class="empty-state">
        <i class="icon-folder-open"></i>
        <p>未找到资源</p>
      </div>
      
      <div v-else class="resources-list">
        <div
          v-for="resource in resources"
          :key="resource.id"
          class="resource-item"
          @click="handleViewResource(resource.id)"
        >
          <div class="resource-icon">
            <i :class="getResourceIcon(resource.fileType)"></i>
          </div>
          
          <div class="resource-content">
            <h3>{{ resource.name }}</h3>
            <p>{{ resource.description }}</p>
            
            <div class="resource-meta">
              <span class="category-tag">{{ resource.category }}</span>
              <span class="file-size">{{ formatFileSize(resource.fileSize) }}</span>
              <span class="file-type">{{ resource.fileType?.toUpperCase() }}</span>
            </div>
          </div>
          
          <div class="resource-stats">
            <div class="stat-item">
              <i class="icon-download"></i>
              <span>{{ resource.downloadCount }}</span>
            </div>
            <div class="stat-item">
              <i class="icon-star"></i>
              <span>{{ resource.collectionCount }}</span>
            </div>
            <div class="stat-item">
              <i class="icon-comment"></i>
              <span>{{ resource.commentCount }}</span>
            </div>
          </div>
          
          <div class="resource-uploader">
            <img :src="resource.uploader.avatar || defaultAvatar" alt="Avatar" class="uploader-avatar" />
            <div class="uploader-info">
              <span class="uploader-name">{{ resource.uploader.nickname || resource.uploader.username }}</span>
              <span class="upload-date">{{ formatDate(resource.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="totalPages > 1" class="pagination-section">
        <PagePagination
          :current-page="currentPage"
          :total-items="totalItems"
          :page-size="pageSize"
          @page-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { resourceApi } from '../api/resource-api'
import PagePagination from '../components/PagePagination.vue'

export default {
  name: 'Resources',
  components: {
    PagePagination
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    
    const loading = ref(false)
    const resources = ref([])
    
    const filters = reactive({
      keyword: '',
      category: '',
      fileType: '',
      sortBy: 'latest'
    })
    
    const currentPage = ref(1)
    const pageSize = ref(12)
    const totalItems = ref(0)
    const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))
    
    const categories = ref([])
    let searchTimer = null
    
    const canUpload = computed(() => store.getters.canUpload)
    
    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'
    
    const loadResources = async () => {
      loading.value = true
      try {
        const response = await resourceApi.getResources({
          page: currentPage.value,
          pageSize: pageSize.value,
          keyword: filters.keyword,
          category: filters.category,
          fileType: filters.fileType,
          sortBy: filters.sortBy
        })
        
        if (response.success) {
          resources.value = response.data.list
          totalItems.value = response.data.total
        }
      } catch (err) {
        console.error('Failed to load resources:', err)
      } finally {
        loading.value = false
      }
    }
    
    const handleSearch = () => {
      currentPage.value = 1
      if (searchTimer) clearTimeout(searchTimer)
      searchTimer = setTimeout(() => {
        updateQuery()
        loadResources()
      }, 300)
    }

    const handleSearchImmediate = () => {
      currentPage.value = 1
      if (searchTimer) clearTimeout(searchTimer)
      updateQuery()
      loadResources()
    }
    
    const handleFilterChange = () => {
      currentPage.value = 1
      updateQuery()
      loadResources()
    }
    
    const handlePageChange = (page) => {
      currentPage.value = page
      loadResources()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    
    const handleViewResource = (id) => {
      router.push(`/resources/${id}`)
    }

    const updateQuery = () => {
      const q = {}
      if (filters.keyword) q.keyword = filters.keyword
      if (filters.category) q.category = filters.category
      if (filters.fileType) q.fileType = filters.fileType
      if (filters.sortBy && filters.sortBy !== 'latest') q.sortBy = filters.sortBy
      router.replace({ query: q })
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
    
    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
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
    
    const applyQuery = (q) => {
      filters.keyword = q.keyword || ''
      filters.category = q.category || ''
      filters.fileType = q.fileType || ''
      filters.sortBy = q.sortBy || 'latest'
    }

    watch(() => route.query, (newQuery) => {
      applyQuery(newQuery || {})
      loadResources()
    }, { immediate: true })
    
    onMounted(() => {
      resourceApi.getCategories().then((res) => {
        if (res && res.success) {
          categories.value = res.data || []
        }
      }).catch(() => {})
    })
    
    return {
      loading,
      resources,
      filters,
      currentPage,
      totalItems,
      totalPages,
      categories,
      canUpload,
      defaultAvatar,
      handleSearch,
      handleSearchImmediate,
      handleFilterChange,
      handlePageChange,
      handleViewResource,
      getResourceIcon,
      formatFileSize,
      formatDate
    }
  }
}
</script>

<style scoped lang="less">
.resources-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    color: #333;
  }
}

.filters-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
  
  .search-box {
    flex: 1;
    min-width: 200px;
    position: relative;
    
    input {
      width: 100%;
      padding: 12px 16px 12px 44px;
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      font-size: 14px;
      transition: all 0.3s;
      
      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }
    }
    
    i {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: #999;
      font-size: 18px;
    }
  }
  
  .filter-options {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    
    select {
      padding: 12px 16px;
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      font-size: 14px;
      background: white;
      cursor: pointer;
      transition: all 0.3s;
      
      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }
    }
  }
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  max-width: 1200px;
  margin: 0 auto;
  
  i {
    font-size: 64px;
    margin-bottom: 16px;
    display: block;
  }
  
  p {
    font-size: 18px;
  }
}

.resources-list {
  display: grid;
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
  
  .resource-item {
    background: white;
    border-radius: 12px;
    padding: 24px;
    display: grid;
    grid-template-columns: auto 1fr auto auto;
    gap: 24px;
    align-items: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      transform: translateX(4px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    }
    
    .resource-icon {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      
      i {
        font-size: 32px;
        color: white;
      }
    }
    
    .resource-content {
      min-width: 0;
      
      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      p {
        font-size: 14px;
        color: #666;
        margin-bottom: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .resource-meta {
        display: flex;
        gap: 12px;
        align-items: center;
        font-size: 12px;
        
        .category-tag {
          background: #e3f2fd;
          color: #2196f3;
          padding: 4px 12px;
          border-radius: 12px;
          font-weight: 500;
        }
        
        .file-size,
        .file-type {
          color: #999;
        }
      }
    }
    
    .resource-stats {
      display: flex;
      gap: 16px;
      
      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #666;
        
        i {
          color: #999;
        }
      }
    }
    
    .resource-uploader {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
      
      .uploader-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        object-fit: cover;
      }
      
      .uploader-info {
        display: flex;
        flex-direction: column;
        gap: 2px;
        
        .uploader-name {
          font-size: 14px;
          font-weight: 500;
          color: #333;
        }
        
        .upload-date {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

.pagination-section {
  margin-top: 32px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .filters-section {
    flex-direction: column;
    align-items: stretch;
    
    .filter-options {
      flex-direction: column;
      
      select {
        width: 100%;
      }
    }
  }
  
  .resources-list {
    .resource-item {
      grid-template-columns: auto 1fr;
      grid-template-rows: auto auto auto;
      gap: 16px;
      
      .resource-icon {
        grid-row: 1 / 3;
      }
      
      .resource-content {
        grid-column: 2;
      }
      
      .resource-stats {
        grid-column: 1 / -1;
        justify-content: flex-start;
      }
      
      .resource-uploader {
        grid-column: 1 / -1;
      }
    }
  }
}
</style>
