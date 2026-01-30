<template>
  <div class="comment-list">
    <div class="comment-list-header">
      <h3>Comments ({{ comments.length }})</h3>
      <div v-if="isLoggedIn" class="comment-actions">
        <button class="btn btn-sm btn-secondary" @click="showForm = !showForm">
          <i :class="showForm ? 'icon-close' : 'icon-plus'"></i>
          {{ showForm ? 'Cancel' : 'Add Comment' }}
        </button>
      </div>
    </div>

    <CommentForm
      v-if="showForm && isLoggedIn"
      :resource-id="resourceId"
      @submit="handleSubmit"
    />

    <div v-if="loading" class="loading-state">
      <i class="icon-loading"></i>
      <p>Loading comments...</p>
    </div>

    <div v-else-if="comments.length === 0" class="empty-state">
      <i class="icon-comment"></i>
      <p>No comments yet. Be the first to comment!</p>
    </div>

    <div v-else class="comments-container">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @reply="handleReply"
        @delete="handleDelete"
      />
    </div>

    <div v-if="hasMore" class="load-more">
      <button class="btn btn-secondary btn-block" @click="loadMore" :disabled="loading">
        <i v-if="loading" class="icon-loading"></i>
        {{ loading ? 'Loading...' : 'Load More Comments' }}
      </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { commentApi } from '../api/comment-api'
import CommentForm from './CommentForm.vue'
import CommentItem from './CommentItem.vue'

export default {
  name: 'CommentList',
  components: {
    CommentForm,
    CommentItem
  },
  props: {
    resourceId: {
      type: [String, Number],
      required: true
    }
  },
  setup(props) {
    const store = useStore()

    const comments = ref([])
    const loading = ref(false)
    const showForm = ref(false)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)
    const hasMore = computed(() => comments.value.length < totalItems.value)

    const isLoggedIn = computed(() => store.getters.isLoggedIn)

    const loadComments = async (page = 1) => {
      loading.value = true
      try {
        const response = await commentApi.getComments(props.resourceId, {
          page,
          pageSize: pageSize.value
        })

        if (response.success) {
          if (page === 1) {
            comments.value = response.data.list
          } else {
            comments.value = [...comments.value, ...response.data.list]
          }
          totalItems.value = response.data.total
        }
      } catch (err) {
        console.error('Failed to load comments:', err)
      } finally {
        loading.value = false
      }
    }

    const loadMore = () => {
      currentPage.value++
      loadComments(currentPage.value)
    }

    const handleSubmit = (commentData) => {
      comments.value.unshift(commentData)
      totalItems.value++
      showForm.value = false
    }

    const handleReply = (replyData) => {
      const parentComment = comments.value.find(c => c.id === replyData.parentId)
      if (parentComment) {
        if (!parentComment.replies) {
          parentComment.replies = []
        }
        parentComment.replies.push(replyData)
        parentComment.replyCount++
      }
    }

    const handleDelete = (commentId) => {
      const deleteComment = (commentList) => {
        const index = commentList.findIndex(c => c.id === commentId)
        if (index !== -1) {
          commentList.splice(index, 1)
          return true
        }
        for (const comment of commentList) {
          if (comment.replies && deleteComment(comment.replies)) {
            comment.replyCount--
            return true
          }
        }
        return false
      }

      deleteComment(comments.value)
      totalItems.value--
    }

    onMounted(() => {
      loadComments()
    })

    return {
      comments,
      loading,
      showForm,
      hasMore,
      isLoggedIn,
      loadMore,
      handleSubmit,
      handleReply,
      handleDelete
    }
  }
}
</script>

<style scoped>
.comment-list {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
}

.comment-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e5e7eb;
}

.comment-list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.comment-actions {
  display: flex;
  gap: 10px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-state i,
.empty-state i {
  font-size: 48px;
  margin-bottom: 10px;
  display: block;
}

.comments-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.load-more {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 14px;
}
</style>
