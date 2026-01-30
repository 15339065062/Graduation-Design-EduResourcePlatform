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
  padding: 96px 0 56px;
  color: var(--text);
  
  .hero-content {
    text-align: center;
    max-width: 860px;
    margin: 0 auto;
    background: rgba(255, 255, 255, 0.76);
    border: 1px solid var(--border);
    box-shadow: var(--shadow-lg);
    border-radius: 24px;
    padding: 52px clamp(18px, 4vw, 56px);
    backdrop-filter: blur(14px);
    
    h1 {
      font-size: clamp(30px, 4vw, 46px);
      font-weight: 800;
      margin-bottom: 14px;
      line-height: 1.15;
      letter-spacing: -0.03em;
    }
    
    p {
      font-size: clamp(15px, 1.6vw, 18px);
      color: var(--text-2);
      margin-bottom: 28px;
    }
    
    .hero-actions {
      display: flex;
      gap: 12px;
      justify-content: center;
      flex-wrap: wrap;
      
      .btn-lg {
        padding: 12px 18px;
        font-size: 15px;
      }
    }
  }
}

.stats-section {
  padding: 0 0 56px;
  
  .stats-box {
    background: rgba(255, 255, 255, 0.82);
    border: 1px solid var(--border);
    border-radius: 22px;
    padding: 22px;
    box-shadow: var(--shadow-md);
    max-width: 1120px;
    margin: -34px auto 0;
    position: relative;
    z-index: 10;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    
    .stat-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 14px 14px;
      background: rgba(255, 255, 255, 0.82);
      border: 1px solid rgba(15, 23, 42, 0.08);
      border-radius: 16px;
      transition: transform var(--transition), box-shadow var(--transition), border-color var(--transition);
      
      &:hover {
        transform: translateY(-1px);
        box-shadow: var(--shadow-md);
        border-color: rgba(15, 23, 42, 0.14);
      }
      
      .stat-icon {
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        background: linear-gradient(135deg, rgba(79, 109, 255, 0.95) 0%, rgba(123, 92, 255, 0.95) 100%);
        border-radius: 14px;
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
          color: var(--text);
          margin-bottom: 2px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        p {
          font-size: 14px;
          color: var(--text-2);
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
  padding: 56px 0;
  background: transparent;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
    
    h2 {
      font-size: 28px;
      font-weight: 700;
      color: var(--text);
    }
    
    .btn-link {
      color: rgba(15, 23, 42, 0.72);
      text-decoration: none;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 4px;
      
      &:hover {
        color: rgba(15, 23, 42, 0.92);
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
    gap: 16px;
    
    .resource-card {
      background: rgba(255, 255, 255, 0.82);
      border-radius: 18px;
      padding: 18px;
      border: 1px solid rgba(15, 23, 42, 0.10);
      box-shadow: var(--shadow-md);
      cursor: pointer;
      transition: transform var(--transition), box-shadow var(--transition), border-color var(--transition);
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-lg);
        border-color: rgba(15, 23, 42, 0.14);
      }
      
      .resource-icon {
        width: 56px;
        height: 56px;
        background: linear-gradient(135deg, rgba(79, 109, 255, 0.95) 0%, rgba(123, 92, 255, 0.95) 100%);
        border-radius: 16px;
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
          color: var(--text);
          margin-bottom: 8px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        p {
          font-size: 14px;
          color: var(--text-2);
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
          color: var(--text-3);
          
          .category-tag {
            background: rgba(79, 109, 255, 0.12);
            color: rgba(15, 23, 42, 0.82);
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
          color: var(--text-2);
          
          .uploader-avatar {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            object-fit: cover;
          }
        }
        
        .upload-date {
          font-size: 12px;
          color: var(--text-3);
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 0 40px;
    
    .hero-content {
      padding: 36px 18px;
    }
  }
  
  .stats-section {
    padding-bottom: 40px;
    
    .stats-box {
      padding: 16px;
      margin-top: -22px;
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
