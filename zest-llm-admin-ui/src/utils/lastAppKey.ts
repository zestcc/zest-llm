const STORAGE_KEY = 'zest-llm-last-app-key'

export function getLastAppKey(): string {
  return localStorage.getItem(STORAGE_KEY) || ''
}

export function setLastAppKey(appKey: string) {
  if (appKey) {
    localStorage.setItem(STORAGE_KEY, appKey)
  }
}

export function filterTasksByApp<T extends { appKey?: string }>(tasks: T[], appKey: string): T[] {
  if (!appKey) return tasks
  return tasks.filter((t) => t.appKey === appKey)
}

export function syncTaskCode(tasks: { code: string }[], current: string): string {
  if (current && tasks.some((t) => t.code === current)) return current
  return tasks[0]?.code || ''
}
