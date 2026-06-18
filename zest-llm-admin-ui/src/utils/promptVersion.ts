/** 从已有版本号列表推断下一个版本（v1 → v2） */
export function suggestNextPromptVersion(versions: string[]): string {
  let max = 0
  for (const raw of versions) {
    const m = /^v?(\d+)$/i.exec((raw || '').trim())
    if (m) {
      max = Math.max(max, parseInt(m[1], 10))
    }
  }
  return `v${max + 1}`
}

/** 模板摘要：按行展示，过滤空行 */
export function templateSummaryLines(body: string | undefined, maxLines = 5): string[] {
  if (!body) return []
  const lines = body
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .filter((line) => line.trim().length > 0)
  if (lines.length <= maxLines) return lines
  return [...lines.slice(0, maxLines), '…']
}
