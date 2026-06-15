export function isTokenExpired(token: string): boolean {
  try {
    const segment = token.split('.')[1]
    if (!segment) {
      return true
    }
    const payload = JSON.parse(atob(segment.replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number }
    if (typeof payload.exp !== 'number') {
      return false
    }
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

export function clearAuthSession(): void {
  localStorage.removeItem('zest-llm-token')
  localStorage.removeItem('zest-llm-user')
  localStorage.removeItem('zest-llm-role')
  localStorage.removeItem('zest-llm-sso-login')
}
