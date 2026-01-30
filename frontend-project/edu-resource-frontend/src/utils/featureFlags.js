export function isCommentV2Enabled() {
  const v = localStorage.getItem('commentV2')
  if (v === '0') return false
  if (v === '1') return true
  return true
}

