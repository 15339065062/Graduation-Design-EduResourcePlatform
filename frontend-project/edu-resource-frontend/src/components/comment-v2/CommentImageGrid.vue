<template>
  <div v-if="images && images.length" class="grid" role="list">
    <button
      v-for="(img, idx) in images"
      :key="idx"
      class="cell"
      type="button"
      @click="open(img.url)"
      :aria-label="`open image ${idx + 1}`"
      role="listitem"
    >
      <img :src="img.thumbUrl || img.url" alt="" loading="lazy" />
    </button>
  </div>
</template>

<script>
export default {
  name: 'CommentImageGrid',
  props: {
    images: {
      type: Array,
      default: () => []
    }
  },
  setup() {
    const open = (url) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    }
    return { open }
  }
}
</script>

<style scoped lang="less">
.grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.cell {
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.cell img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  display: block;
}
</style>

