<template>
  <div class="resource-card" @click="handleClick">
    <div class="resource-icon">
      <i :class="getResourceIcon(resource.fileType)"></i>
    </div>

    <div class="resource-info">
      <h3 class="resource-name">{{ resource.name }}</h3>
      <p class="resource-description">{{ resource.description }}</p>

      <div class="resource-meta">
        <span class="category-tag">{{ resource.category }}</span>
        <span class="file-size">{{ formatFileSize(resource.fileSize) }}</span>
        <span class="file-type">{{ resource.fileType?.toUpperCase() }}</span>
      </div>
    </div>

    <div class="resource-stats">
      <div class="stat-item" title="Downloads">
        <i class="icon-download"></i>
        <span>{{ resource.downloadCount || 0 }}</span>
      </div>
      <div class="stat-item" title="Collections">
        <i class="icon-star"></i>
        <span>{{ resource.collectionCount || 0 }}</span>
      </div>
      <div class="stat-item" title="Comments">
        <i class="icon-comment"></i>
        <span>{{ resource.commentCount || 0 }}</span>
      </div>
    </div>

    <div class="resource-footer">
      <div class="uploader-info">
        <img :src="resource.uploader?.avatar || defaultAvatar" alt="Avatar" />
        <span>{{ resource.uploader?.nickname || resource.uploader?.username || 'Unknown' }}</span>
      </div>
      <span class="upload-date">{{ formatDate(resource.createdAt) }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ResourceCard',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'

    const handleClick = () => {
      emit('click', props.resource.id)
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
        mov: 'icon-file-video',
        mp3: 'icon-file-video',
        wav: 'icon-file-video',
        ogg: 'icon-file-video'
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

    const formatDate = (dateString) => {
      if (!dateString) return ''

      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date

      const days = Math.floor(diff / 86400000)

      if (days < 1) return 'Today'
      if (days < 7) return `${days} day${days > 1 ? 's' : ''} ago`

      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      })
    }

    return {
      defaultAvatar,
      handleClick,
      getResourceIcon,
      formatFileSize,
      formatDate
    }
  }
}
</script>

<style scoped>
.resource-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.resource-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  font-size: 28px;
}

.resource-info {
  flex: 1;
}

.resource-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-description {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 42px;
}

.resource-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.category-tag {
  padding: 4px 10px;
  background: #e0e7ff;
  color: #4338ca;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.file-size,
.file-type {
  font-size: 12px;
  color: #9ca3af;
}

.resource-stats {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.stat-item i {
  font-size: 16px;
}

.resource-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.uploader-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-info img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.uploader-info span {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100px;
}

.upload-date {
  font-size: 12px;
  color: #9ca3af;
}
</style>
