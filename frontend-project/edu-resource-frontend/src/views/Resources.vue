<template>
  <div class="resources-page">
    <div class="container">
      <div class="page-header">
        <h1>资源</h1>
        <router-link v-if="canUpload" to="/upload" class="btn btn-primary btn-cta">
          <i class="icon-upload"></i> 上传资源
        </router-link>
        <router-link v-else-if="isLoggedIn" to="/user-center?tab=role-change" class="btn btn-outline">
          <i class="icon-upload"></i> 申请上传权限
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
    const isLoggedIn = computed(() => store.getters.isLoggedIn)
    
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
      const defaultCategories = [
        '语文', '数学', '英语', '物理', '化学', '生物', '地理', '历史', '政治',
        '信息技术', '编程入门', '数据结构与算法', '软件工程', '操作系统', '计算机网络', '数据库',
        '前端开发', '后端开发', '移动开发', '人工智能(AI)', '机器学习', '深度学习', '数据科学', '大数据',
        '网络安全', '云计算', 'DevOps', '物联网(IoT)', '机器人',
        '产品设计', 'UI/UX 设计', '数字媒体', 'AIGC', '区块链', 'AR/VR/XR', '量子计算',
        '经济学', '管理学', '心理学', '教育学', '医学基础', '法律基础',
        '考研', '公考', '职业技能', '竞赛/科研'
      ]

      resourceApi.getCategories().then((res) => {
        if (res && res.success) {
          const list = Array.isArray(res.data) ? res.data : []
          const merged = Array.from(new Set([...defaultCategories, ...list].map(v => (v == null ? '' : String(v)).trim()).filter(Boolean)))
          categories.value = merged
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
      isLoggedIn,
      handleSearch,
      handleSearchImmediate,
      handleFilterChange,
      handlePageChange,
      handleViewResource,
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
  background: transparent;
  padding: 28px 0 56px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    color: var(--text);
    letter-spacing: -0.02em;
  }
}

.filters-section {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  padding: 18px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);
  margin-bottom: 24px;
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
    }
    
    i {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-3);
      font-size: 18px;
    }
  }
  
  .filter-options {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    
    select {
      padding: 12px 16px;
      border: 1px solid var(--border);
      border-radius: 14px;
      font-size: 14px;
      background: rgba(255, 255, 255, 0.78);
      cursor: pointer;
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

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-3);
  
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
  
  .resource-item {
    background: rgba(255, 255, 255, 0.82);
    border-radius: 20px;
    padding: 18px;
    display: grid;
    grid-template-columns: auto 1fr auto auto;
    gap: 18px;
    align-items: center;
    border: 1px solid rgba(15, 23, 42, 0.10);
    box-shadow: var(--shadow-md);
    cursor: pointer;
    transition: transform var(--transition), box-shadow var(--transition), border-color var(--transition);
    backdrop-filter: blur(14px);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: var(--shadow-lg);
      border-color: rgba(15, 23, 42, 0.14);
      background: rgba(255, 255, 255, 0.90);
    }
    
    .resource-icon {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, rgba(79, 109, 255, 0.95) 0%, rgba(123, 92, 255, 0.95) 100%);
      border-radius: 18px;
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
        font-weight: 750;
        color: var(--text);
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        letter-spacing: -0.015em;
      }
      
      p {
        font-size: 14px;
        color: var(--text-2);
        margin-bottom: 12px;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        line-height: 1.5;
      }
      
      .resource-meta {
        display: flex;
        gap: 12px;
        align-items: center;
        font-size: 12px;
        
        .category-tag {
          background: rgba(79, 109, 255, 0.12);
          color: rgba(15, 23, 42, 0.82);
          padding: 4px 12px;
          border-radius: 12px;
          font-weight: 500;
        }
        
        .file-size,
        .file-type {
          color: var(--text-3);
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
        color: var(--text-2);
        padding: 6px 10px;
        border-radius: 999px;
        border: 1px solid rgba(15, 23, 42, 0.08);
        background: rgba(255, 255, 255, 0.70);
        transition: background var(--transition), border-color var(--transition), transform var(--transition);
        
        i {
          color: rgba(15, 23, 42, 0.55);
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
        border: 1px solid rgba(15, 23, 42, 0.10);
      }
      
      .uploader-info {
        display: flex;
        flex-direction: column;
        gap: 2px;
        
        .uploader-name {
          font-size: 14px;
          font-weight: 500;
          color: var(--text);
        }
        
        .upload-date {
          font-size: 12px;
          color: var(--text-3);
        }
      }
    }
  }
}

.pagination-section {
  margin-top: 32px;
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
