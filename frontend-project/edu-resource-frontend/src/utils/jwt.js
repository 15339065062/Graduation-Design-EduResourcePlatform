export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('Failed to parse JWT', e);
    return null;
  }
}

export function isTokenExpired(token) {
    const payload = parseJwt(token);
    if (!payload) return true;
    
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < currentTime;
}

// 检查 Token 是否即将过期 (例如 5 分钟内)
export function isTokenExpiringSoon(token, bufferSeconds = 300) {
    const payload = parseJwt(token);
    if (!payload) return true;
    
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < (currentTime + bufferSeconds);
}
