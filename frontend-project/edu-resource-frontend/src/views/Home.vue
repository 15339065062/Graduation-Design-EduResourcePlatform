<template>
  <div class="home-page">
    <div class="hero-section">
      <div class="container">
        <div class="hero-content">
          <h1>教育资源共享平台</h1>
          <p>发现、分享和协作教育资源</p>
          <div class="hero-actions">
            <router-link to="/resources" class="btn btn-primary btn-lg">
              <i class="icon-search"></i> 浏览资源
            </router-link>
            <router-link v-if="canUpload" to="/upload" class="btn btn-secondary btn-lg">
              <i class="icon-upload"></i> 上传资源
            </router-link>
            <router-link v-else to="/register" class="btn btn-secondary btn-lg">
              <i class="icon-user-plus"></i> 立即加入
            </router-link>
          </div>
        </div>
      </div>
    </div>
    
    <div class="stats-section">
      <div class="container">
        <div class="stats-box">
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon">
                <i class="icon-file"></i>
              </div>
              <div class="stat-content">
                <h3>{{ stats.totalResources }}</h3>
                <p>资源总数</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <i class="icon-users"></i>
              </div>
              <div class="stat-content">
                <h3>{{ stats.totalUsers }}</h3>
                <p>活跃用户</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <i class="icon-download"></i>
              </div>
              <div class="stat-content">
                <h3>{{ stats.totalDownloads }}</h3>
                <p>下载总数</p>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <i class="icon-star"></i>
              </div>
              <div class="stat-content">
                <h3>{{ stats.totalCollections }}</h3>
                <p>收藏总数</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="featured-section">
      <div class="container">
        <div class="section-header">
          <h2>精选资源</h2>
          <router-link to="/resources" class="btn btn-link">
            查看全部 <i class="icon-arrow-right"></i>
          </router-link>
        </div>
        
        <div v-if="loading" class="loading-state">
          <i class="icon-loading"></i>
          <p>正在加载资源...</p>
        </div>
        
        <div v-else-if="featuredResources.length === 0" class="empty-state">
          <i class="icon-folder-open"></i>
          <p>暂无资源</p>
        </div>
        
        <div v-else class="resources-grid">
          <div
            v-for="resource in featuredResources"
            :key="resource.id"
            class="resource-card"
            @click="handleViewResource(resource.id)"
          >
            <div class="resource-icon">
              <i :class="getResourceIcon(resource.fileType)"></i>
            </div>
            <div class="resource-info">
              <h4>{{ resource.name }}</h4>
              <p>{{ resource.description }}</p>
              <div class="resource-meta">
                <span class="category-tag">{{ resource.category }}</span>
                <span><i class="icon-download"></i> {{ resource.downloadCount }}</span>
              </div>
            </div>
            <div class="resource-footer">
              <div class="uploader-info">
                <img :src="resource.uploader.avatar || defaultAvatar" alt="Avatar" class="uploader-avatar" />
                <span>{{ resource.uploader.nickname || resource.uploader.username }}</span>
              </div>
              <span class="upload-date">{{ formatDate(resource.createdAt) }}</span>
            </div>
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
  name: 'Home',
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const loading = ref(false)
    const featuredResources = ref([])
    
    const stats = ref({
      totalResources: 0,
      totalUsers: 0,
      totalDownloads: 0,
      totalCollections: 0
    })
    
    const canUpload = computed(() => store.getters.canUpload)
    
    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'
    
    const loadFeaturedResources = async () => {
      loading.value = true
      try {
        const response = await resourceApi.getResources({ 
          page: 1, 
          pageSize: 8,
          sortBy: 'recommend'
        })
        if (response.success) {
          featuredResources.value = response.data.list
        }
      } catch (err) {
        console.error('Failed to load resources:', err)
      } finally {
        loading.value = false
      }
    }
    
    const loadStats = async () => {
      try {
        const response = await resourceApi.getStats()
        if (response.success) {
          stats.value = {
             totalResources: response.data.resourceCount,
             totalUsers: response.data.userCount,
             totalDownloads: response.data.downloadCount,
             totalCollections: response.data.collectionCount
          }
        }
      } catch (err) {
        console.error('Failed to load stats:', err)
      }
    }
    
    const handleViewResource = (id) => {
      router.push(`/resources/${id}`)
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
      loadFeaturedResources()
      loadStats()
    })
    
    return {
      loading,
      featuredResources,
      stats,
      canUpload,
      defaultAvatar,
      handleViewResource,
      getResourceIcon,
      formatDate
    }
  }
}
</script>

<style scoped lang="less">
.home-page {
  min-height: 100vh;
}

.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 100px 20px;
  color: white;
  
  .hero-content {
    text-align: center;
    max-width: 800px;
    margin: 0 auto;
    
    h1 {
      font-size: 48px;
      font-weight: 700;
      margin-bottom: 20px;
      line-height: 1.2;
    }
    
    p {
      font-size: 20px;
      opacity: 0.9;
      margin-bottom: 40px;
    }
    
    .hero-actions {
      display: flex;
      gap: 16px;
      justify-content: center;
      flex-wrap: wrap;
      
      .btn-lg {
        padding: 16px 32px;
        font-size: 16px;
        font-weight: 600;
      }
      
      .btn-primary {
        background: white;
        color: #667eea;
        
        &:hover {
          background: #f5f7fa;
          transform: translateY(-2px);
        }
      }
      
      .btn-secondary {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        backdrop-filter: blur(10px);
        
        &:hover {
          background: rgba(255, 255, 255, 0.3);
          transform: translateY(-2px);
        }
      }
    }
  }
}

.stats-section {
  padding: 0 20px 60px;
  background: transparent;
  
  .stats-box {
    background: white;
    border: 2px solid #764ba2;
    border-radius: 16px;
    padding: 30px;
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.15);
    max-width: 1100px;
    margin: -60px auto 0;
    position: relative;
    z-index: 10;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    
    .stat-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 12px;
      transition: transform 0.3s;
      
      &:hover {
        transform: translateY(-2px);
      }
      
      .stat-icon {
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        i {
          font-size: 24px;
          color: white;
        }
      }
      
      .stat-content {
        flex: 1;
        min-width: 0; /* 防止内容溢出 */
        
        h3 {
          font-size: 24px;
          font-weight: 700;
          color: #333;
          margin-bottom: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        p {
          font-size: 14px;
          color: #666;
          margin: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }
}

.featured-section {
  padding: 60px 20px;
  background: #f5f7fa;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
    
    h2 {
      font-size: 28px;
      font-weight: 700;
      color: #333;
    }
    
    .btn-link {
      color: #667eea;
      text-decoration: none;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 4px;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
  
  .loading-state,
  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    
    i {
      font-size: 48px;
      margin-bottom: 16px;
      display: block;
    }
    
    p {
      font-size: 16px;
    }
  }
  
  .resources-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
    
    .resource-card {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      .resource-icon {
        width: 56px;
        height: 56px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
        
        i {
          font-size: 28px;
          color: white;
        }
      }
      
      .resource-info {
        h4 {
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
          margin-bottom: 16px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .resource-meta {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 13px;
          color: #999;
          
          .category-tag {
            background: #e3f2fd;
            color: #2196f3;
            padding: 4px 12px;
            border-radius: 12px;
            font-weight: 500;
          }
          
          span:not(.category-tag) {
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }
      }
      
      .resource-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px solid #f0f0f0;
        
        .uploader-info {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          color: #666;
          
          .uploader-avatar {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            object-fit: cover;
          }
        }
        
        .upload-date {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 20px;
    
    .hero-content {
      h1 {
        font-size: 32px;
      }
      
      p {
        font-size: 16px;
      }
    }
  }
  
  .stats-section {
    padding-bottom: 40px;
    
    .stats-box {
      padding: 20px;
      margin-top: -40px;
    }

    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }
  }
  
  @media (max-width: 480px) {
    .stats-section {
      .stats-grid {
        grid-template-columns: 1fr;
      }
      
      .stat-card {
        padding: 16px;
      }
    }
  }
  
  .featured-section {
    .section-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
    }
  }
}
</style>
