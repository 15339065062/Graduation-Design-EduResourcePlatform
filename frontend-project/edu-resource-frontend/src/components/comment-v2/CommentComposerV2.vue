<template>
  <div class="comment-composer">
    <div class="composer-row">
      <img :src="avatarUrl" alt="" class="avatar" />
      <div class="input-area">
        <textarea
          v-model="text"
          class="textarea"
          :placeholder="placeholder"
          rows="2"
          maxlength="2000"
          aria-label="comment input"
        ></textarea>
        <div v-if="images.length" class="images-preview">
          <div v-for="(img, idx) in images" :key="idx" class="preview-item">
            <img :src="img.preview" alt="" />
            <button class="remove" type="button" @click="removeImage(idx)" aria-label="remove image">×</button>
          </div>
        </div>
        <div class="actions">
          <div class="left">
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png"
              multiple
              style="display:none"
              @change="onFilesChange"
            />
            <button class="btn btn-light" type="button" @click="openFile" :disabled="submitting" aria-label="add image">
              图片
            </button>
          </div>
          <div class="right">
            <button v-if="showCancel" class="btn btn-secondary" type="button" @click="handleCancel" :disabled="submitting">
              {{ cancelText }}
            </button>
            <button class="btn btn-primary" type="button" @click="handleSubmit" :disabled="!canSubmit || submitting">
              {{ submitting ? submittingText : submitText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { t } from '@/utils/i18n'

export default {
  name: 'CommentComposerV2',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: () => t('writeComment')
    },
    showCancel: {
      type: Boolean,
      default: false
    },
    submitting: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'submit', 'cancel', 'imagesChange'],
  setup(props, { emit }) {
    const store = useStore()
    const fileInput = ref(null)
    const text = ref(props.modelValue)
    const images = ref([])

    watch(
      () => props.modelValue,
      (v) => {
        if (v !== text.value) text.value = v
      }
    )

    watch(text, (v) => emit('update:modelValue', v))

    const currentUser = computed(() => store.state.user)
    const avatarUrl = computed(() => currentUser.value?.avatar || '/uploads/avatars/default.png')

    const canSubmit = computed(() => {
      return (text.value && text.value.trim().length > 0) || images.value.length > 0
    })

    const submitText = computed(() => t('send'))
    const cancelText = computed(() => t('cancel'))
    const submittingText = computed(() => t('uploading'))

    const openFile = () => {
      if (images.value.length >= 3) return
      fileInput.value && fileInput.value.click()
    }

    const onFilesChange = (e) => {
      const files = Array.from(e.target.files || [])
      if (!files.length) return
      const remain = Math.max(0, 3 - images.value.length)
      files.slice(0, remain).forEach((f) => {
        if (!['image/jpeg', 'image/png'].includes(f.type)) return
        if (f.size > 5 * 1024 * 1024) return
        const preview = URL.createObjectURL(f)
        images.value.push({ file: f, preview })
      })
      emit('imagesChange', images.value.map(x => x.file))
      e.target.value = ''
    }

    const removeImage = (idx) => {
      const item = images.value[idx]
      if (item?.preview) URL.revokeObjectURL(item.preview)
      images.value.splice(idx, 1)
      emit('imagesChange', images.value.map(x => x.file))
    }

    const handleSubmit = () => {
      emit('submit', { content: text.value, images: images.value.map(x => x.file) })
    }

    const handleCancel = () => emit('cancel')

    const reset = () => {
      text.value = ''
      images.value.forEach(x => x.preview && URL.revokeObjectURL(x.preview))
      images.value = []
      emit('imagesChange', [])
    }

    return {
      fileInput,
      text,
      images,
      avatarUrl,
      canSubmit,
      submitText,
      cancelText,
      submittingText,
      openFile,
      onFilesChange,
      removeImage,
      handleSubmit,
      handleCancel,
      reset
    }
  }
}
</script>

<style scoped lang="less">
.comment-composer {
  width: 100%;
}

.composer-row {
  display: flex;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.input-area {
  flex: 1;
}

.textarea {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.4;
  resize: vertical;
}

.images-preview {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.preview-item {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f9fafb;

  img {
    width: 100%;
    height: 90px;
    object-fit: cover;
    display: block;
  }
}

.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  cursor: pointer;
}

.actions {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.btn {
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  border: 1px solid transparent;
  cursor: pointer;
}

.btn-light {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.btn-primary {
  background: #667eea;
  color: #fff;
}

.btn-secondary {
  background: #fff;
  border-color: #e5e7eb;
  color: #111827;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

