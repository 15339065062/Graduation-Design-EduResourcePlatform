<template>
  <div class="comment-form">
    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <textarea
          v-model="content"
          placeholder="Write your comment here..."
          rows="4"
          maxlength="500"
          required
        ></textarea>
        <div class="char-count">
          {{ content.length }} / 500
        </div>
      </div>

      <div v-if="replyTo" class="reply-info">
        <span>Replying to <strong>{{ replyTo.user.nickname || replyTo.user.username }}</strong></span>
        <button type="button" class="btn-close" @click="handleCancelReply">
          <i class="icon-close"></i>
        </button>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="submitting || !content.trim()">
          <i v-if="submitting" class="icon-loading"></i>
          {{ submitting ? 'Posting...' : 'Post Comment' }}
        </button>
        <button v-if="showCancel" type="button" class="btn btn-secondary" @click="handleCancel">
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { useStore } from 'vuex'
import { commentApi } from '../api/comment-api'

export default {
  name: 'CommentForm',
  props: {
    resourceId: {
      type: [String, Number],
      required: true
    },
    replyTo: {
      type: Object,
      default: null
    },
    showCancel: {
      type: Boolean,
      default: false
    }
  },
  emits: ['submit', 'cancel'],
  setup(props, { emit }) {
    const store = useStore()

    const content = ref('')
    const submitting = ref(false)

    const handleSubmit = async () => {
      if (!content.value.trim()) {
        return
      }

      submitting.value = true

      try {
        const commentData = {
          resourceId: props.resourceId,
          content: content.value.trim()
        }

        if (props.replyTo) {
          commentData.parentId = props.replyTo.id
          commentData.replyToUserId = props.replyTo.user.id
        }

        const response = await commentApi.createComment(commentData)

        if (response.success) {
          content.value = ''
          emit('submit', response.data)
        } else {
          alert(response.message || 'Failed to post comment')
        }
      } catch (err) {
        console.error('Failed to post comment:', err)
        alert('Network error. Please try again.')
      } finally {
        submitting.value = false
      }
    }

    const handleCancel = () => {
      content.value = ''
      emit('cancel')
    }

    const handleCancelReply = () => {
      emit('cancel')
    }

    watch(() => props.replyTo, () => {
      if (props.replyTo) {
        content.value = `@${props.replyTo.user.nickname || props.replyTo.user.username} `
      }
    })

    return {
      content,
      submitting,
      handleSubmit,
      handleCancel,
      handleCancelReply
    }
  }
}
</script>

<style scoped>
.comment-form {
  background: #f9fafb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.form-group {
  position: relative;
  margin-bottom: 15px;
}

.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.char-count {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.reply-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #e0e7ff;
  border-radius: 6px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #4338ca;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #4338ca;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: rgba(67, 56, 202, 0.1);
  border-radius: 4px;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5a67d8;
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover {
  background: #d1d5db;
}

.icon-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
