<template>
  <div class="comment-item" :class="{ 'is-reply': isReply }">
    <div class="comment-avatar">
      <img :src="comment.user.avatar || defaultAvatar" :alt="comment.user.username" />
    </div>

    <div class="comment-content">
      <div class="comment-header">
        <div class="comment-author">
          <span class="author-name">{{ comment.user.nickname || comment.user.username }}</span>
          <span v-if="comment.user.role" class="author-role">{{ roleLabel }}</span>
          <span v-if="comment.isUploader" class="uploader-badge">
            <i class="icon-star"></i> Uploader
          </span>
        </div>
        <div class="comment-meta">
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
          <button
            v-if="canDelete"
            class="btn-delete"
            @click="handleDelete"
            title="Delete comment"
          >
            <i class="icon-trash"></i>
          </button>
        </div>
      </div>

      <div class="comment-body">
        <p>{{ comment.content }}</p>
      </div>

      <div class="comment-footer">
        <button class="btn-action" @click="toggleReplyForm">
          <i :class="showReplyForm ? 'icon-close' : 'icon-reply'"></i>
          {{ showReplyForm ? 'Cancel' : 'Reply' }}
        </button>
        <button class="btn-action" @click="handleLike" :class="{ 'is-liked': isLiked }">
          <i :class="isLiked ? 'icon-heart-filled' : 'icon-heart'"></i>
          {{ comment.likeCount || 0 }}
        </button>
      </div>

      <CommentForm
        v-if="showReplyForm"
        :resource-id="resourceId"
        :reply-to="comment"
        show-cancel
        @submit="handleReplySubmit"
        @cancel="showReplyForm = false"
      />

      <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
        <div class="replies-header">
          <span>{{ comment.replies.length }} {{ comment.replies.length === 1 ? 'reply' : 'replies' }}</span>
          <button
            v-if="comment.replies.length > 3 && !showAllReplies"
            class="btn-show-replies"
            @click="showAllReplies = true"
          >
            View all replies
          </button>
        </div>

        <div class="replies-list">
          <CommentItem
            v-for="reply in displayedReplies"
            :key="reply.id"
            :comment="reply"
            :resource-id="resourceId"
            :is-reply="true"
            @reply="handleNestedReply"
            @delete="handleNestedDelete"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useStore } from 'vuex'
import { commentApi } from '../api/comment-api'
import CommentForm from './CommentForm.vue'

export default {
  name: 'CommentItem',
  components: {
    CommentForm
  },
  props: {
    comment: {
      type: Object,
      required: true
    },
    resourceId: {
      type: [String, Number],
      required: true
    },
    isReply: {
      type: Boolean,
      default: false
    }
  },
  emits: ['reply', 'delete'],
  setup(props, { emit }) {
    const store = useStore()

    const showReplyForm = ref(false)
    const showAllReplies = ref(false)
    const isLiked = ref(false)

    const currentUser = computed(() => store.state.user)
    const canDelete = computed(() => {
      return currentUser.value && (
        currentUser.value.id === props.comment.user.id ||
        currentUser.value.role === 'admin'
      )
    })

    const roleLabel = computed(() => {
      const roles = {
        student: 'Student',
        teacher: 'Teacher',
        admin: 'Admin'
      }
      return roles[props.comment.user.role] || ''
    })

    const displayedReplies = computed(() => {
      if (!props.comment.replies) return []
      if (showAllReplies.value) return props.comment.replies
      return props.comment.replies.slice(0, 3)
    })

    const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23667eea"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'

    const formatDate = (dateString) => {
      if (!dateString) return ''

      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date

      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)

      if (minutes < 1) return 'Just now'
      if (minutes < 60) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`
      if (hours < 24) return `${hours} hour${hours > 1 ? 's' : ''} ago`
      if (days < 7) return `${days} day${days > 1 ? 's' : ''} ago`

      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    const toggleReplyForm = () => {
      showReplyForm.value = !showReplyForm.value
    }

    const handleReplySubmit = (replyData) => {
      emit('reply', replyData)
      showReplyForm.value = false
    }

    const handleNestedReply = (replyData) => {
      emit('reply', replyData)
    }

    const handleLike = async () => {
      if (!currentUser.value) {
        alert('Please login to like comments')
        return
      }

      try {
        const response = isLiked.value
          ? await commentApi.unlikeComment(props.comment.id)
          : await commentApi.likeComment(props.comment.id)

        if (response.success) {
          isLiked.value = !isLiked.value
          props.comment.likeCount = isLiked.value
            ? (props.comment.likeCount || 0) + 1
            : Math.max(0, (props.comment.likeCount || 0) - 1)
        }
      } catch (err) {
        console.error('Failed to like comment:', err)
      }
    }

    const handleDelete = async () => {
      if (!confirm('Are you sure you want to delete this comment?')) {
        return
      }

      try {
        const response = await commentApi.deleteComment(props.comment.id)
        if (response.success) {
          emit('delete', props.comment.id)
        } else {
          alert(response.message || 'Failed to delete comment')
        }
      } catch (err) {
        console.error('Failed to delete comment:', err)
        alert('Network error. Please try again.')
      }
    }

    const handleNestedDelete = (commentId) => {
      emit('delete', commentId)
    }

    return {
      showReplyForm,
      showAllReplies,
      isLiked,
      currentUser,
      canDelete,
      roleLabel,
      displayedReplies,
      defaultAvatar,
      formatDate,
      toggleReplyForm,
      handleReplySubmit,
      handleNestedReply,
      handleLike,
      handleDelete,
      handleNestedDelete
    }
  }
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.comment-item.is-reply {
  padding: 12px;
  background: #f9fafb;
  margin-left: 48px;
  border-left: 2px solid #e5e7eb;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background: #e5e7eb;
}

.comment-item.is-reply .comment-avatar img {
  width: 32px;
  height: 32px;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.author-name {
  font-weight: 600;
  color: #1f2937;
}

.author-role {
  font-size: 12px;
  padding: 2px 8px;
  background: #e0e7ff;
  color: #4338ca;
  border-radius: 12px;
}

.uploader-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 12px;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-date {
  font-size: 12px;
  color: #9ca3af;
}

.btn-delete {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.comment-item:hover .btn-delete {
  opacity: 1;
}

.btn-delete:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 4px;
}

.comment-body {
  margin-bottom: 12px;
}

.comment-body p {
  margin: 0;
  color: #374151;
  line-height: 1.6;
  word-wrap: break-word;
}

.comment-footer {
  display: flex;
  gap: 16px;
}

.btn-action {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 13px;
  color: #6b7280;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.btn-action:hover {
  color: #667eea;
}

.btn-action.is-liked {
  color: #ef4444;
}

.comment-replies {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.replies-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.replies-header span {
  font-size: 13px;
  color: #6b7280;
}

.btn-show-replies {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: #667eea;
  padding: 0;
}

.btn-show-replies:hover {
  text-decoration: underline;
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
