<template>
  <div class="resource-detail-page">
    <div class="container">
      <div v-if="loading" class="loading-state">
        <i class="icon-loading"></i>
        <p>加载资源中...</p>
      </div>
      
      <div v-else-if="!resource" class="error-state">
        <i class="icon-error"></i>
        <p>资源未找到</p>
        <router-link to="/resources" class="btn btn-primary">返回资源列表</router-link>
      </div>
      
      <div v-else class="resource-detail">
        <div class="resource-header">
          <div class="resource-icon-large">
            <i :class="getResourceIcon(resource.fileType)"></i>
          </div>
          
          <div class="resource-info">
            <h1>{{ resource.name }}</h1>
            <p class="resource-description">{{ resource.description }}</p>
            
            <div class="resource-meta">
              <span class="category-tag">{{ resource.category }}</span>
              <span class="file-type">{{ resource.fileType?.toUpperCase() }}</span>
              <span class="file-size">{{ formatFileSize(resource.fileSize) }}</span>
              <span class="upload-date">
                <i class="icon-calendar"></i> {{ formatDate(resource.createdAt) }}
              </span>
            </div>
          </div>
          
          <div class="resource-actions">
            <button class="btn btn-primary btn-lg" @click="handleDownload">
              <i class="icon-download"></i> 下载
            </button>
            <button class="btn btn-secondary" @click="handleCollect" :disabled="collecting">
              <i :class="isCollected ? 'icon-star-filled' : 'icon-star'"></i>
              {{ isCollected ? '已收藏' : '收藏' }}
            </button>
            <button 
              v-if="canDelete" 
              class="btn btn-danger" 
              @click="handleDeleteResource"
            >
              <i class="icon-trash"></i> 删除
            </button>
          </div>
        </div>
        
        <div class="resource-content-grid">
          <div class="main-content">
            <!-- Preview Section -->
            <div class="preview-section" v-if="resource.fileType">
              <div v-if="['mp4', 'webm', 'ogg', 'mov'].includes(resource.fileType.toLowerCase())" class="video-preview">
                <video controls :src="getPreviewUrl(resource.id)">
                  您的浏览器不支持视频播放
                </video>
              </div>
              <div v-else-if="['mp3', 'wav', 'ogg'].includes(resource.fileType.toLowerCase())" class="audio-preview">
                <audio controls :src="getPreviewUrl(resource.id)">
                  您的浏览器不支持音频播放
                </audio>
              </div>
              <div v-else-if="['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(resource.fileType.toLowerCase())" class="image-preview">
                <img :src="getPreviewUrl(resource.id)" :alt="resource.name" />
              </div>
              <div v-else-if="['pdf'].includes(resource.fileType.toLowerCase())" class="pdf-preview">
                <iframe :src="getPreviewUrl(resource.id)" class="preview-frame" frameborder="0"></iframe>
              </div>
              <div v-else class="no-preview">
                <i class="icon-file"></i>
                <p>此文件类型暂不支持在线预览，请下载查看</p>
              </div>
            </div>

            <div class="uploader-card">
              <h3>上传者</h3>
              <div class="uploader-info">
              <img :src="resource.uploader?.avatar || defaultAvatar" alt="Avatar" class="uploader-avatar" />
              <div class="uploader-details">
                <span class="uploader-name">{{ resource.uploader?.nickname || resource.uploader?.username }}</span>
              <span class="uploader-role">{{ roleLabel === 'User' ? '用户' : roleLabel === 'Teacher' ? '老师' : roleLabel === 'Student' ? '学生' : '管理员' }}</span>
              </div>
            </div>
            </div>
            
            <div class="stats-card">
              <h3>统计信息</h3>
              <div class="stats-grid">
                <div class="stat-item">
                  <i class="icon-download"></i>
                  <div>
                    <span class="stat-value">{{ resource.downloadCount }}</span>
                    <span class="stat-label">下载量</span>
                  </div>
                </div>
                <div class="stat-item">
                  <i class="icon-star"></i>
                  <div>
                    <span class="stat-value">{{ resource.collectionCount }}</span>
                    <span class="stat-label">收藏数</span>
                  </div>
                </div>
                <div class="stat-item">
                  <i class="icon-comment"></i>
                  <div>
                    <span class="stat-value">{{ resource.commentCount }}</span>
                    <span class="stat-label">评论数</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="comments-section">
              <CommentPanelV2 v-if="useCommentV2" :resource-id="resourceId" />
              <template v-else>
                <h3>评论 ({{ comments.length }})</h3>
                
                <div v-if="isLoggedIn" class="comment-form">
                  <textarea
                    v-model="newComment"
                    placeholder="写下你的评论..."
                    rows="3"
                  ></textarea>
                  <button class="btn btn-primary" @click="handleSubmitComment" :disabled="!newComment.trim() || submitting">
                    {{ submitting ? '发布中...' : '发布评论' }}
                  </button>
                </div>
                
                <div v-else class="login-prompt">
                  <p>请 <router-link to="/login">登录</router-link> 后发表评论</p>
                </div>
                
                <div v-if="comments.length === 0" class="empty-comments">
                  <i class="icon-comment"></i>
                  <p>暂无评论</p>
                </div>
                
                <div v-else class="comments-list">
                  <div v-for="comment in comments" :key="comment.id" class="comment-item">
                    <img :src="comment.avatar || defaultAvatar" alt="Avatar" class="comment-avatar" />
                    <div class="comment-content">
                      <div class="comment-header">
                        <span class="comment-author">{{ comment.nickname || comment.username }}</span>
                        <span class="comment-date">{{ formatDate(comment.createTime) }}</span>
                      </div>
                      <p class="comment-text">{{ comment.content }}</p>
                      
                      <div class="comment-actions">
                        <button v-if="isLoggedIn" class="btn-action" @click="handleReply(comment)">
                          <i class="icon-reply"></i> 回复
                        </button>
                        <template v-if="isLoggedIn && currentUser && currentUser.id === comment.userId">
                          <button class="btn-action" @click="startEdit(comment)">
                            <i class="icon-edit"></i> 编辑
                          </button>
                          <button class="btn-action delete" @click="handleDeleteComment(comment.id)">
                            <i class="icon-trash"></i> 删除
                          </button>
                        </template>
                      </div>

                      <div v-if="replyingCommentId === comment.id" class="reply-form">
                        <div class="textarea-wrapper">
                          <textarea
                            v-model="replyContent"
                            :placeholder="replyToUser ? `回复 @${replyToUser.nickname || replyToUser.username}...` : '写下你的回复...'"
                            rows="2"
                            maxlength="500"
                          ></textarea>
                          <span class="char-count">{{ replyContent.length }}/500</span>
                        </div>
                        <div class="reply-actions">
                          <button class="btn btn-sm btn-primary" @click="handleSubmitReply(comment.id)" :disabled="submitting">
                            {{ submitting ? '发送中...' : '发送' }}
                          </button>
                          <button class="btn btn-sm btn-default" @click="cancelReply">取消</button>
                        </div>
                      </div>

                      <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
                        <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                          <img :src="reply.avatar || defaultAvatar" alt="Avatar" class="reply-avatar" />
                          <div class="reply-content">
                            <div class="reply-header">
                              <span class="reply-author">
                                {{ reply.nickname || reply.username }}
                                <template v-if="reply.replyToNickname">
                                  <span style="margin: 0 4px; color: #999; font-weight: normal;">回复</span>
                                  <span style="color: #667eea;">@{{ reply.replyToNickname }}</span>
                                </template>
                              </span>
                              <span class="reply-date">{{ formatDate(reply.createTime) }}</span>
                            </div>
                            <p class="reply-text">{{ reply.content }}</p>
                            <div class="reply-actions" style="margin-top: 4px; display: flex; gap: 12px;">
                              <button v-if="isLoggedIn" class="btn-action" @click="handleReply(comment, reply)" style="border:none; background:none; color:#667eea; cursor:pointer; font-size:12px; padding:0; display:flex; align-items:center; gap:2px;">
                                <i class="icon-reply" style="font-size:12px;"></i> 回复
                              </button>
                              <button v-if="isLoggedIn && currentUser && currentUser.id === reply.userId" class="btn-action delete btn-xs" @click="handleDeleteComment(reply.id)" style="border:none; background:none; color:#ff4d4f; cursor:pointer; font-size:12px; padding:0; display:flex; align-items:center; gap:2px;">
                                <i class="icon-trash" style="font-size:12px;"></i> 删除
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
          
          <aside class="sidebar">
            <div class="related-resources">
              <h3>相关资源</h3>
              <div v-if="relatedResources.length === 0" class="empty-related">
                <p>暂无相关资源</p>
              </div>
              <div v-else class="related-list">
                <div
                  v-for="item in relatedResources"
                  :key="item.id"
                  class="related-item"
                  @click="handleViewResource(item.id)"
                >
                  <div class="related-icon">
                    <i :class="getResourceIcon(item.fileType)"></i>
                  </div>
                  <div class="related-info">
                    <h4>{{ item.name }}</h4>
                    <p>{{ item.downloadCount }} 次下载</p>
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { resourceApi } from '../api/resource-api'
import { commentApi } from '../api/comment-api'
import { collectionApi } from '../api/collection-api'
import CommentPanelV2 from '@/components/comment-v2/CommentPanelV2.vue'
import { isCommentV2Enabled } from '@/utils/featureFlags'

export default {
  name: 'ResourceDetail',
  components: { CommentPanelV2 },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    
    const loading = ref(false)
    const resource = ref(null)
    const comments = ref([])
    const totalComments = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(5)
    const relatedResources = ref([])
    
    const newComment = ref('')
    const editingCommentId = ref(null)
    const editContent = ref('')
    const replyingCommentId = ref(null)
    const replyToUser = ref(null)
    const replyContent = ref('')
    const submitting = ref(false)
    const collecting = ref(false)
    const isCollected = ref(false)

    const resourceId = computed(() => parseInt(route.params.id))
    const useCommentV2 = computed(() => isCommentV2Enabled())
    
    const isLoggedIn = computed(() => store.getters.isLoggedIn)
    const currentUser = computed(() => store.state.user)
    
    // Broadcast Channel for cross-tab synchronization
    const collectionChannel = new BroadcastChannel('collection_channel')
    let pollInterval = null
    
    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'
    
    const roleLabel = computed(() => {
      const roles = {
        student: 'Student',
        teacher: 'Teacher',
        admin: 'Administrator'
      }
      return roles[resource.value?.uploader?.role] || 'User'
    })

    const canDelete = computed(() => {
      if (!isLoggedIn.value || !resource.value) return false
      return currentUser.value.role === 'admin' || currentUser.value.id === resource.value.uploaderId
    })
    
    const loadResource = async () => {
      loading.value = true
      try {
        const response = await resourceApi.getResourceDetail(route.params.id)
        if (response.success) {
          resource.value = response.data
        } else {
          resource.value = null
        }
      } catch (err) {
        console.error('Failed to load resource:', err)
        resource.value = null
      } finally {
        loading.value = false
      }
    }
    
    const loadComments = async () => {
      try {
        const response = await commentApi.getComments(route.params.id, {
          page: currentPage.value,
          pageSize: pageSize.value
        })
        if (response.success) {
          comments.value = response.data.comments
          totalComments.value = response.data.total
        }
      } catch (err) {
        console.error('Failed to load comments:', err)
      }
    }
    
    const handlePageChange = (page) => {
      currentPage.value = page
      loadComments()
    }

    const handleDeleteComment = async (commentId) => {
      if (!confirm('确定要删除这条评论吗？')) return
      
      try {
        const response = await commentApi.deleteComment(commentId)
        if (response.success) {
          // If it's the last item on the page and not the first page, go back one page
          if (comments.value.length === 1 && currentPage.value > 1) {
            currentPage.value--
          }
          loadComments()
          if (resource.value) resource.value.commentCount--
        } else {
          alert(response.message || '删除失败')
        }
      } catch (err) {
        console.error('Delete comment error:', err)
        alert('删除失败，请重试')
      }
    }

    const startEdit = (comment) => {
      editingCommentId.value = comment.id
      editContent.value = comment.content
    }

    const cancelEdit = () => {
      editingCommentId.value = null
      editContent.value = ''
    }

    const handleUpdateComment = async (commentId) => {
      if (!editContent.value.trim()) {
        alert('评论内容不能为空')
        return
      }
      
      try {
        const response = await commentApi.updateComment(commentId, {
          content: editContent.value
        })
        
        if (response.success) {
          cancelEdit()
          loadComments()
        } else {
          alert(response.message || '更新失败')
        }
      } catch (err) {
        console.error('Update comment error:', err)
        alert('更新失败，请重试')
      }
    }
    
    const loadRelatedResources = async () => {
      try {
        const response = await resourceApi.getRelatedResources(route.params.id)
        if (response.success) {
          relatedResources.value = response.data
        }
      } catch (err) {
        console.error('Failed to load related resources:', err)
      }
    }
    
    const checkCollectionStatus = async () => {
      if (!isLoggedIn.value) return
      
      try {
        const response = await collectionApi.checkStatus(route.params.id)
        if (response.success) {
          isCollected.value = response.data
        } else {
          console.error('Failed to check collection status:', response.message)
        }
      } catch (err) {
        console.error('Failed to check collection status:', err)
        // 401 handled by interceptor
      }
    }
    
    const handleDownload = async () => {
      try {
        const response = await resourceApi.downloadResource(route.params.id)
        // 直接处理文件流响应
        const url = window.URL.createObjectURL(new Blob([response]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `${resource.value.name}.${resource.value.fileType}`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        resource.value.downloadCount++
      } catch (err) {
        alert('下载失败，请重试')
      }
    }

    const handleDeleteResource = async () => {
      if (!confirm('确定要删除这个资源吗？此操作不可恢复。')) return
      
      try {
        const response = await resourceApi.deleteResource(resource.value.id)
        if (response.success) {
          alert('删除成功')
          router.replace('/resources')
        } else {
          alert(response.message || '删除失败')
        }
      } catch (err) {
        console.error('Delete resource error:', err)
        alert('删除失败，请重试')
      }
    }
    
    const handleCollect = async () => {
      if (!isLoggedIn.value) {
        router.push('/login')
        return
      }
      
      // Optimistic update
      const previousState = isCollected.value
      const previousCount = resource.value.collectionCount
      
      isCollected.value = !isCollected.value
      resource.value.collectionCount += isCollected.value ? 1 : -1
      
      collecting.value = true
      
      try {
        const resourceId = parseInt(route.params.id)
        const response = await collectionApi.toggleCollection(resourceId)
        
        if (response.success) {
          // Sync with server state
          const { isCollected: serverCollected, count } = response.data
          if (isCollected.value !== serverCollected) {
             isCollected.value = serverCollected
             resource.value.collectionCount = count
          }
          
          // Notify other tabs
          collectionChannel.postMessage({
            type: 'collection_updated',
            resourceId: resourceId,
            isCollected: serverCollected,
            count: count
          })
          
          // Show feedback
          // alert(isCollected.value ? '收藏成功' : '取消收藏成功') 
        } else {
          // Revert on failure
          isCollected.value = previousState
          resource.value.collectionCount = previousCount
          alert(response.message || '操作失败')
        }
      } catch (err) {
        console.error('Collection operation failed:', err)
        // Revert on error
        isCollected.value = previousState
        resource.value.collectionCount = previousCount
        
        if (err.response && err.response.status === 401) {
          alert('登录已过期，请重新登录')
          store.dispatch('logout')
          router.push('/login')
        } else {
          alert('网络错误，请重试')
        }
      } finally {
        collecting.value = false
      }
    }
    
    const startPolling = () => {
      if (pollInterval) clearInterval(pollInterval)
      pollInterval = setInterval(() => {
        if (isLoggedIn.value && !document.hidden) {
           checkCollectionStatus()
        }
      }, 5000) // Poll every 5 seconds
    }
    
    const getPreviewUrl = (id) => {
      return `/api/resources/${id}/download?inline=true`
    }

    onMounted(() => {
      loadResource()
      loadComments()
      loadRelatedResources()
      checkCollectionStatus()
      startPolling()
      
      // Listen for cross-tab updates
      collectionChannel.onmessage = (event) => {
        if (event.data.type === 'collection_updated' && event.data.resourceId === parseInt(route.params.id)) {
          isCollected.value = event.data.isCollected
          if (resource.value) {
             resource.value.collectionCount = event.data.count
          }
        }
      }
    })
    
    onUnmounted(() => {
      if (pollInterval) clearInterval(pollInterval)
      collectionChannel.close()
    })

    const handleSubmitComment = async () => {
      if (!newComment.value.trim()) return
      
      submitting.value = true
      try {
        const resourceId = parseInt(route.params.id)
        const commentData = {
          resourceId: resourceId,
          userId: currentUser.value.id,
          content: newComment.value
        }
        console.log('Submitting comment:', commentData)
        
        const response = await commentApi.createComment(commentData)
        console.log('Comment submission response:', response)
        
        if (response.success) {
          // Reload comments to get the correct order and total count
          currentPage.value = 1
          loadComments()
          if (resource.value) resource.value.commentCount++
          newComment.value = ''
        } else {
          alert(response.message || '发布评论失败')
        }
      } catch (err) {
        console.error('Submit comment error:', err)
        if (err.response && err.response.data) {
             alert(`发布失败: ${err.response.data.message || '未知错误'}`)
        } else {
             alert('网络错误，请检查控制台日志')
        }
      } finally {
        submitting.value = false
      }
    }
    
    const handleReply = (comment, targetUser = null) => {
      replyingCommentId.value = comment.id
      replyToUser.value = targetUser
      replyContent.value = ''
    }
    
    const cancelReply = () => {
      replyingCommentId.value = null
      replyToUser.value = null
      replyContent.value = ''
    }

    const handleSubmitReply = async (parentId) => {
      if (!replyContent.value.trim()) return
      
      submitting.value = true
      try {
        const resourceId = parseInt(route.params.id)
        const commentData = {
          resourceId: resourceId,
          userId: currentUser.value.id,
          content: replyContent.value,
          parentId: parentId,
          replyToUserId: replyToUser.value ? replyToUser.value.id : null
        }
        
        const response = await commentApi.createComment(commentData)
        
        if (response.success) {
          // Ideally, append the reply to the comment's replies list locally
          // But reloading is safer for consistency
          loadComments()
          if (resource.value) resource.value.commentCount++
          cancelReply()
        } else {
          alert(response.message || '回复失败')
        }
      } catch (err) {
        console.error('Submit reply error:', err)
        alert('回复失败，请重试')
      } finally {
        submitting.value = false
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
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    onMounted(() => {
      loadResource()
      loadComments()
      loadRelatedResources()
      checkCollectionStatus()
    })
    
    return {
      loading,
      resourceId,
      useCommentV2,
      resource,
      comments,
      relatedResources,
      newComment,
      submitting,
      collecting,
      isCollected,
      isLoggedIn,
      currentUser,
      defaultAvatar,
      roleLabel,
      handleDownload,
      handleDeleteResource,
      handleCollect,
      handleSubmitComment,
      handleReply,
      cancelReply,
      handleSubmitReply,
      handleDeleteComment,
      startEdit,
      cancelEdit,
      handleUpdateComment,
      handlePageChange,
      currentPage,
      pageSize,
      totalComments,
      editingCommentId,
      editContent,
      replyingCommentId,
      replyContent,
      replyToUser,
      handleViewResource,
      getResourceIcon,
      formatFileSize,
      formatDate,
      getPreviewUrl,
      canDelete
    }
  }
}
</script>

<style scoped lang="less">
.preview-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  
  .preview-media {
    width: 100%;
    max-height: 500px;
    border-radius: 8px;
    object-fit: contain;
    background: #000;
  }

  .video-preview video {
    width: 100%;
    aspect-ratio: 16/9;
    background: #000;
    object-fit: contain;
  }

  .audio-preview {
    display: flex;
    justify-content: center;
    padding: 20px 0;
    
    audio {
      width: 100%;
      outline: none;
    }
  }

  .image-preview {
    display: flex;
    justify-content: center;
    background: #f9f9f9;
    border-radius: 8px;
    padding: 20px;
    
    img {
      max-width: 100%;
      max-height: 600px;
      width: auto;
      height: auto;
      border-radius: 4px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
  }
  
  .preview-frame {
    width: 100%;
    height: 600px;
    border-radius: 8px;
    border: 1px solid #eee;
  }
  
  .no-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    background: #f9f9f9;
    border-radius: 8px;
    color: #999;
    
    i {
      font-size: 48px;
      margin-bottom: 16px;
    }
  }
}

.resource-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  
  i {
    font-size: 64px;
    margin-bottom: 16px;
    display: block;
  }
  
  p {
    font-size: 18px;
    margin-bottom: 24px;
  }
}

.resource-detail {
  max-width: 1200px;
  margin: 0 auto;
  
  .resource-header {
    background: white;
    border-radius: 12px;
    padding: 32px;
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 32px;
    align-items: start;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .resource-icon-large {
      width: 100px;
      height: 100px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      i {
        font-size: 48px;
        color: white;
      }
    }
    
    .resource-info {
      h1 {
        font-size: 28px;
        font-weight: 700;
        color: #333;
        margin-bottom: 12px;
      }
      
      .resource-description {
        font-size: 16px;
        color: #666;
        margin-bottom: 16px;
        line-height: 1.6;
      }
      
      .resource-meta {
        display: flex;
        gap: 12px;
        align-items: center;
        flex-wrap: wrap;
        font-size: 14px;
        
        .category-tag {
          background: #e3f2fd;
          color: #2196f3;
          padding: 6px 16px;
          border-radius: 16px;
          font-weight: 500;
        }
        
        .file-type,
        .file-size,
        .upload-date {
          color: #999;
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
    
    .resource-actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .btn-lg {
        padding: 14px 32px;
        font-size: 16px;
        font-weight: 600;
      }
    }
  }
  
  .resource-content-grid {
    display: grid;
    grid-template-columns: 1fr 320px;
    gap: 24px;
    
    .main-content {
      display: flex;
      flex-direction: column;
      gap: 24px;
      
      .uploader-card,
      .stats-card {
        background: white;
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        
        h3 {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 16px;
        }
      }
      
      .uploader-card {
        .uploader-info {
          display: flex;
          align-items: center;
          gap: 16px;
          
          .uploader-avatar {
            width: 56px;
            height: 56px;
            border-radius: 50%;
            object-fit: cover;
          }
          
          .uploader-details {
            display: flex;
            flex-direction: column;
            gap: 4px;
            
            .uploader-name {
              font-size: 16px;
              font-weight: 600;
              color: #333;
            }
            
            .uploader-role {
              font-size: 14px;
              color: #999;
            }
          }
        }
      }
      
      .stats-card {
        .stats-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 16px;
          
          .stat-item {
            display: flex;
            align-items: center;
            gap: 12px;
            
            i {
              font-size: 24px;
              color: #667eea;
            }
            
            div {
              display: flex;
              flex-direction: column;
              gap: 2px;
              
              .stat-value {
                font-size: 20px;
                font-weight: 600;
                color: #333;
              }
              
              .stat-label {
                font-size: 12px;
                color: #999;
              }
            }
          }
        }
      }
      
      .comments-section {
        background: white;
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        
        h3 {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 20px;
        }
        
        .comment-form {
          margin-bottom: 24px;
          
          textarea {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 14px;
            font-family: inherit;
            resize: vertical;
            margin-bottom: 12px;
            
            &:focus {
              outline: none;
              border-color: #667eea;
              box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
            }
          }
        }
        
        .login-prompt {
          padding: 16px;
          background: #f5f7fa;
          border-radius: 8px;
          text-align: center;
          margin-bottom: 24px;
          
          p {
            font-size: 14px;
            color: #666;
            
            a {
              color: #667eea;
              text-decoration: none;
              font-weight: 600;
            }
          }
        }
        
        .empty-comments {
          text-align: center;
          padding: 40px 20px;
          color: #999;
          
          i {
            font-size: 48px;
            margin-bottom: 12px;
            display: block;
          }
          
          p {
            font-size: 14px;
          }
        }
        
        .comments-list {
          display: flex;
          flex-direction: column;
          gap: 20px;
          
          .comment-item {
            display: flex;
            gap: 16px;
            padding-bottom: 20px;
            border-bottom: 1px solid #f0f0f0;
            
            &:last-child {
              border-bottom: none;
              padding-bottom: 0;
            }
            
            .comment-avatar {
              width: 40px;
              height: 40px;
              border-radius: 50%;
              object-fit: cover;
              flex-shrink: 0;
            }
            
            .comment-content {
              flex: 1;
              
              .comment-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 8px;
                
                .comment-author {
                  font-size: 14px;
                  font-weight: 600;
                  color: #333;
                }
                
                .comment-date {
                  font-size: 12px;
                  color: #999;
                }
              }
              
              .comment-text {
                font-size: 14px;
                color: #666;
                line-height: 1.6;
                margin-bottom: 12px;
                white-space: pre-wrap;
              }

              .edit-mode {
                margin-bottom: 12px;
                
                .textarea-wrapper {
                  position: relative;
                  margin-bottom: 8px;
                  
                  textarea {
                    width: 100%;
                    padding: 8px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    resize: vertical;
                  }
                  
                  .char-count {
                    position: absolute;
                    bottom: 4px;
                    right: 8px;
                    font-size: 11px;
                    color: #999;
                  }
                }

                .edit-actions {
                  display: flex;
                  gap: 8px;
                  
                  .btn-sm {
                    padding: 6px 12px;
                    font-size: 12px;
                    border-radius: 4px;
                    cursor: pointer;
                  }
                  
                  .btn-primary {
                    background: #667eea;
                    color: white;
                    border: none;
                  }
                  
                  .btn-default {
                    background: #f0f0f0;
                    color: #666;
                    border: none;
                  }
                }
              }
              
              .comment-actions {
                display: flex;
                gap: 16px;
                
                .btn-action {
                  background: none;
                  border: none;
                  color: #667eea;
                  font-size: 13px;
                  cursor: pointer;
                  display: flex;
                  align-items: center;
                  gap: 4px;
                  padding: 0;
                  
                  &:hover {
                    text-decoration: underline;
                  }
                  
                  &.delete {
                    color: #ff4d4f;
                  }
                }
              }

              .comment-replies {
                margin-top: 16px;
                background: #f9f9f9;
                padding: 16px;
                border-radius: 8px;
                
                .reply-item {
                  display: flex;
                  gap: 12px;
                  margin-bottom: 16px;
                  padding-bottom: 16px;
                  border-bottom: 1px solid #eee;
                  
                  &:last-child {
                    margin-bottom: 0;
                    padding-bottom: 0;
                    border-bottom: none;
                  }
                  
                  .reply-avatar {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    object-fit: cover;
                    flex-shrink: 0;
                  }
                  
                  .reply-content {
                    flex: 1;
                    
                    .reply-header {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      margin-bottom: 4px;
                      
                      .reply-author {
                        font-size: 13px;
                        font-weight: 600;
                        color: #333;
                      }
                      
                      .reply-date {
                        font-size: 12px;
                        color: #999;
                      }
                    }
                    
                    .reply-text {
                      font-size: 13px;
                      color: #666;
                      line-height: 1.5;
                    }
                  }
                }
              }
            }
          }
        }
        
        .pagination {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 16px;
          margin-top: 24px;
          padding-top: 24px;
          border-top: 1px solid #f0f0f0;
          
          .page-btn {
            padding: 8px 16px;
            background: white;
            border: 1px solid #e0e0e0;
            border-radius: 6px;
            cursor: pointer;
            color: #666;
            transition: all 0.2s;
            
            &:hover:not(:disabled) {
              border-color: #667eea;
              color: #667eea;
            }
            
            &:disabled {
              background: #f5f5f5;
              color: #ccc;
              cursor: not-allowed;
            }
          }
          
          .page-info {
            color: #666;
            font-size: 14px;
          }
        }
      }
    }
    
    .sidebar {
      .related-resources {
        background: white;
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        position: sticky;
        top: 20px;
        
        h3 {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 16px;
        }
        
        .empty-related {
          text-align: center;
          padding: 32px 20px;
          color: #999;
          
          p {
            font-size: 14px;
          }
        }
        
        .related-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
          
          .related-item {
            display: flex;
            gap: 12px;
            padding: 12px;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
            
            &:hover {
              background: #f5f7fa;
            }
            
            .related-icon {
              width: 40px;
              height: 40px;
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              border-radius: 8px;
              display: flex;
              align-items: center;
              justify-content: center;
              flex-shrink: 0;
              
              i {
                font-size: 20px;
                color: white;
              }
            }
            
            .related-info {
              flex: 1;
              min-width: 0;
              
              h4 {
                font-size: 14px;
                font-weight: 500;
                color: #333;
                margin-bottom: 4px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }
              
              p {
                font-size: 12px;
                color: #999;
              }
            }
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .resource-detail {
    .resource-header {
      grid-template-columns: auto 1fr;
      grid-template-rows: auto auto;
      
      .resource-icon-large {
        grid-row: 1 / 3;
      }
      
      .resource-actions {
        grid-column: 1 / -1;
        flex-direction: row;
      }
    }
    
    .resource-content-grid {
      grid-template-columns: 1fr;
      
      .sidebar {
        .related-resources {
          position: static;
        }
      }
    }
  }
}
</style>
