<template>
  <div class="comment-item" :class="{ nested: depth > 0 }">
    <router-link v-if="comment.userId" class="user-link" :to="`/user/${comment.userId}`">
      <img v-if="comment.user?.avatar" :src="comment.user.avatar" alt="" class="avatar" />
      <div v-else class="avatar avatar-ph" aria-hidden="true">{{ initials }}</div>
    </router-link>
    <template v-else>
      <img v-if="comment.user?.avatar" :src="comment.user.avatar" alt="" class="avatar" />
      <div v-else class="avatar avatar-ph" aria-hidden="true">{{ initials }}</div>
    </template>
    <div class="body">
      <div class="header">
        <router-link v-if="comment.userId" class="name user-link" :to="`/user/${comment.userId}`">
          {{ comment.user?.nickname || comment.user?.username || '用户' }}
        </router-link>
        <span v-else class="name">{{ comment.user?.nickname || comment.user?.username || '用户' }}</span>
        <span class="time">{{ formatTime(comment.createTime) }}</span>
      </div>
      <div class="content" v-html="renderContent(comment)"></div>
      <CommentImageGrid :images="comment.images" />
      <div class="footer">
        <button class="link" type="button" @click="$emit('reply', comment)" aria-label="reply">
          {{ replyText }}
        </button>
        <button v-if="canDelete" class="link danger" type="button" @click="$emit('delete', comment)" aria-label="delete">
          删除
        </button>
      </div>

      <div v-if="comment.previewReplies && comment.previewReplies.length && depth === 0" class="preview">
        <CommentItemV2
          v-for="r in comment.previewReplies"
          :key="r.id"
          :comment="r"
          :depth="depth + 1"
          :current-user-id="currentUserId"
          :is-admin="isAdmin"
          @reply="$emit('reply', $event)"
          @delete="$emit('delete', $event)"
          @loadReplies="$emit('loadReplies', $event)"
        />
      </div>

      <div v-if="hasMoreReplies" class="more">
        <button class="link" type="button" @click="$emit('loadReplies', comment)" :aria-label="expandReplies">
          {{ expandText }}
        </button>
      </div>

      <div v-if="children && children.length" class="children">
        <CommentItemV2
          v-for="c in children"
          :key="c.id"
          :comment="c"
          :children="childMap ? childMap[c.id] : []"
          :child-map="childMap"
          :depth="depth + 1"
          :current-user-id="currentUserId"
          :is-admin="isAdmin"
          @reply="$emit('reply', $event)"
          @delete="$emit('delete', $event)"
          @loadReplies="$emit('loadReplies', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
import CommentImageGrid from './CommentImageGrid.vue'
import { t } from '@/utils/i18n'

export default {
  name: 'CommentItemV2',
  components: { CommentImageGrid },
  props: {
    comment: {
      type: Object,
      required: true
    },
    children: {
      type: Array,
      default: () => []
    },
    childMap: {
      type: Object,
      default: null
    },
    depth: {
      type: Number,
      default: 0
    },
    currentUserId: {
      type: Number,
      default: null
    },
    isAdmin: {
      type: Boolean,
      default: false
    }
  },
  emits: ['reply', 'delete', 'loadReplies'],
  setup(props) {
    const initials = computed(() => {
      const s = props.comment.user?.nickname || props.comment.user?.username || '用户'
      return s.slice(0, 1)
    })

    const replyText = computed(() => t('reply'))
    const expandText = computed(() => t('expandReplies'))

    const canDelete = computed(() => {
      return props.isAdmin || (props.currentUserId && props.comment.userId === props.currentUserId)
    })

    const hasMoreReplies = computed(() => {
      if (props.depth > 0) return false
      const count = props.comment.replyCount || 0
      const previews = props.comment.previewReplies?.length || 0
      const loaded = props.children?.length || 0
      return count > previews + loaded
    })

    const escapeNewlines = (s) => String(s || '').replaceAll('\n', '<br/>')

    const renderContent = (c) => {
      const prefix = c.replyTo?.nickname || c.replyTo?.username ? `回复 <span class="mention">@${c.replyTo.nickname || c.replyTo.username}</span>：` : ''
      return prefix + escapeNewlines(c.content || '')
    }

    const formatTime = (ts) => {
      const n = Number(ts)
      if (!n) return ''
      const d = new Date(n)
      return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    }

    return {
      initials,
      replyText,
      expandText,
      canDelete,
      hasMoreReplies,
      renderContent,
      formatTime
    }
  }
}
</script>

<style scoped lang="less">
.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
}

.comment-item.nested {
  padding: 10px 0;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex: 0 0 auto;
}

.avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 700;
  font-size: 14px;
}

.body {
  flex: 1;
  min-width: 0;
}

.header {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.user-link {
  text-decoration: none;
  color: inherit;
}

.time {
  font-size: 12px;
  color: #9ca3af;
}

.content {
  margin-top: 4px;
  font-size: 14px;
  color: #111827;
  line-height: 1.55;
  word-break: break-word;
}

.footer {
  margin-top: 6px;
  display: flex;
  gap: 12px;
}

.link {
  border: none;
  background: transparent;
  color: #667eea;
  font-size: 13px;
  padding: 0;
  cursor: pointer;
}

.link.danger {
  color: #ff4d4f;
}

.preview, .children {
  margin-top: 10px;
  padding-left: 10px;
  border-left: 2px solid #f3f4f6;
}

.more {
  margin-top: 8px;
}

:deep(.mention) {
  color: #667eea;
}
</style>

