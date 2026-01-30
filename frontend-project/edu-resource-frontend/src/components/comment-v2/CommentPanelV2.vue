<template>
  <div class="comment-panel">
    <div class="title">{{ titleText }} ({{ total }})</div>

    <div v-if="!isLoggedIn" class="login-tip">
      <span>请登录后发表评论</span>
    </div>

    <CommentComposerV2
      v-else
      v-model="newText"
      :placeholder="t('writeComment')"
      :submitting="submitting"
      @imagesChange="files => newImages = files"
      @submit="handleCreateRoot"
    />

    <div class="list" role="list">
      <div v-if="loading && !comments.length" class="loading">加载中...</div>

      <CommentItemV2
        v-for="c in comments"
        :key="c.id"
        :comment="c"
        :children="childrenByParentId[c.id] || []"
        :child-map="childrenByParentId"
        :current-user-id="currentUserId"
        :is-admin="isAdmin"
        @reply="openReply"
        @delete="handleDelete"
        @loadReplies="loadMoreReplies"
        role="listitem"
      />

      <div v-if="hasMore" class="more-root">
        <button class="btn-more" type="button" @click="loadMoreRoot" :disabled="loading">
          {{ t('loadMore') }}
        </button>
      </div>
    </div>

    <div v-if="replyingTo" class="reply-overlay" role="dialog" aria-modal="true">
      <div class="reply-card">
        <div class="reply-title">
          {{ t('reply') }} @{{ replyingTo.user?.nickname || replyingTo.user?.username || '用户' }}
        </div>
        <CommentComposerV2
          v-model="replyText"
          :placeholder="t('replyTo', { name: replyingTo.user?.nickname || replyingTo.user?.username || '用户' })"
          :show-cancel="true"
          :submitting="replySubmitting"
          @cancel="closeReply"
          @imagesChange="files => replyImages = files"
          @submit="handleCreateReply"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import CommentComposerV2 from './CommentComposerV2.vue'
import CommentItemV2 from './CommentItemV2.vue'
import { commentApiV2 } from '@/api/comment-api-v2'
import { t } from '@/utils/i18n'
import { trackEvent } from '@/utils/analytics'

export default {
  name: 'CommentPanelV2',
  components: { CommentComposerV2, CommentItemV2 },
  props: {
    resourceId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const store = useStore()
    const isLoggedIn = computed(() => !!store.state.token)
    const currentUserId = computed(() => store.state.user?.id || null)
    const isAdmin = computed(() => store.state.user?.role === 'admin')

    const loading = ref(false)
    const comments = ref([])
    const total = ref(0)
    const page = ref(1)
    const pageSize = ref(20)
    const hasMore = computed(() => comments.value.length < total.value)

    const childrenByParentId = ref({})
    const replyPageByParentId = ref({})
    const replyTotalByParentId = ref({})
    const replyLoadingByParentId = ref({})

    const newText = ref('')
    const newImages = ref([])
    const submitting = ref(false)

    const replyingTo = ref(null)
    const replyText = ref('')
    const replyImages = ref([])
    const replySubmitting = ref(false)

    const titleText = computed(() => t('comments'))

    const loadRoot = async (reset = false) => {
      if (loading.value) return
      loading.value = true
      try {
        const p = reset ? 1 : page.value
        const res = await commentApiV2.listRoot(props.resourceId, { page: p, pageSize: pageSize.value })
        if (res.success) {
          total.value = res.data.total || 0
          page.value = res.data.page || p
          const list = res.data.list || []
          if (reset) {
            comments.value = list
          } else {
            comments.value = comments.value.concat(list)
          }
          list.forEach((c) => {
            if (c.previewReplies && c.previewReplies.length) {
              childrenByParentId.value[c.id] = (childrenByParentId.value[c.id] || []).concat(c.previewReplies)
            }
            replyTotalByParentId.value[c.id] = c.replyCount || 0
            replyPageByParentId.value[c.id] = 1
          })
        }
      } catch (e) {
      } finally {
        loading.value = false
      }
    }

    const loadMoreRoot = async () => {
      if (loading.value || !hasMore.value) return
      page.value += 1
      await loadRoot(false)
    }

    const openReply = (comment) => {
      if (!isLoggedIn.value) return
      replyingTo.value = comment
      replyText.value = ''
      replyImages.value = []
      trackEvent('comment_reply_open', { resourceId: props.resourceId, commentId: comment.id })
    }

    const closeReply = () => {
      replyingTo.value = null
      replyText.value = ''
      replyImages.value = []
    }

    const handleCreateRoot = async ({ content, images }) => {
      if (!isLoggedIn.value) return
      if (submitting.value) return
      submitting.value = true
      try {
        const fd = new FormData()
        fd.append('resourceId', String(props.resourceId))
        fd.append('content', content || '')
        ;(images || []).forEach(f => fd.append('images', f))
        const res = await commentApiV2.createComment(fd)
        if (res.success) {
          comments.value = [res.data].concat(comments.value)
          total.value += 1
          newText.value = ''
          newImages.value = []
          trackEvent('comment_create', { resourceId: props.resourceId, commentId: res.data.id, hasImages: (images || []).length > 0 })
        }
      } catch (e) {
      } finally {
        submitting.value = false
      }
    }

    const handleCreateReply = async ({ content, images }) => {
      if (!replyingTo.value || replySubmitting.value) return
      replySubmitting.value = true
      try {
        const parent = replyingTo.value
        const fd = new FormData()
        fd.append('resourceId', String(props.resourceId))
        fd.append('content', content || '')
        fd.append('parentId', String(parent.id))
        fd.append('replyToUserId', String(parent.userId))
        ;(images || []).forEach(f => fd.append('images', f))
        const res = await commentApiV2.createComment(fd)
        if (res.success) {
          childrenByParentId.value[parent.id] = (childrenByParentId.value[parent.id] || []).concat([res.data])
          replyTotalByParentId.value[parent.id] = (replyTotalByParentId.value[parent.id] || 0) + 1
          total.value += 1
          closeReply()
          trackEvent('comment_reply_create', { resourceId: props.resourceId, parentId: parent.id, commentId: res.data.id, hasImages: (images || []).length > 0 })
        }
      } catch (e) {
      } finally {
        replySubmitting.value = false
      }
    }

    const loadMoreReplies = async (parent) => {
      if (!parent) return
      const pid = parent.id
      if (replyLoadingByParentId.value[pid]) return
      const totalReplies = replyTotalByParentId.value[pid] || parent.replyCount || 0
      const loaded = (childrenByParentId.value[pid] || []).length
      if (loaded >= totalReplies) return

      const nextPage = (replyPageByParentId.value[pid] || 1) + 1
      replyLoadingByParentId.value[pid] = true
      try {
        const res = await commentApiV2.listReplies(pid, { page: nextPage, pageSize: 20 })
        if (res.success) {
          const list = res.data.list || []
          childrenByParentId.value[pid] = (childrenByParentId.value[pid] || []).concat(list)
          replyPageByParentId.value[pid] = res.data.page || nextPage
          trackEvent('comment_replies_load_more', { resourceId: props.resourceId, parentId: pid, loaded: list.length })
        }
      } catch (e) {
      } finally {
        replyLoadingByParentId.value[pid] = false
      }
    }

    const handleDelete = async (comment) => {
      if (!comment) return
      const ok = window.confirm('确定删除这条评论吗？')
      if (!ok) return
      try {
        const res = await commentApiV2.deleteComment(comment.id)
        if (res.success) {
          removeFromTree(comment.id)
          total.value = Math.max(0, total.value - 1)
          trackEvent('comment_delete', { resourceId: props.resourceId, commentId: comment.id })
        }
      } catch (e) {
      }
    }

    const removeFromTree = (id) => {
      comments.value = comments.value.filter(c => c.id !== id)
      const map = childrenByParentId.value
      Object.keys(map).forEach(k => {
        map[k] = (map[k] || []).filter(x => x.id !== id)
      })
      delete map[id]
    }

    onMounted(() => {
      loadRoot(true)
    })

    return {
      t,
      titleText,
      isLoggedIn,
      currentUserId,
      isAdmin,
      loading,
      comments,
      total,
      hasMore,
      childrenByParentId,
      newText,
      newImages,
      submitting,
      replyingTo,
      replyText,
      replyImages,
      replySubmitting,
      loadMoreRoot,
      openReply,
      closeReply,
      handleCreateRoot,
      handleCreateReply,
      loadMoreReplies,
      handleDelete
    }
  }
}
</script>

<style scoped lang="less">
.comment-panel {
  margin-top: 20px;
}

.title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
}

.login-tip {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  color: #6b7280;
  margin-bottom: 14px;
}

.list {
  margin-top: 10px;
}

.loading {
  padding: 14px 0;
  color: #6b7280;
}

.more-root {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.btn-more {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
}

.btn-more:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reply-overlay {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 18px;
  z-index: 999;
}

.reply-card {
  width: 100%;
  max-width: 720px;
  background: #fff;
  border-radius: 16px;
  padding: 14px;
}

.reply-title {
  font-weight: 700;
  margin-bottom: 10px;
}
</style>

