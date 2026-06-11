export type SseEventHandler = (event: string, data: string) => void

function parseSseData(raw: string): string {
  const trimmed = raw.trim()
  if (!trimmed) {
    return ''
  }
  try {
    const parsed: unknown = JSON.parse(trimmed)
    if (typeof parsed === 'string') {
      return parsed
    }
    return trimmed
  } catch {
    return trimmed
  }
}

/**
 * POST + SSE（fetch 流式读取），兼容 Spring SseEmitter 的 event/data 帧。
 */
export async function postSse(
  url: string,
  body: unknown,
  onEvent: SseEventHandler,
  signal?: AbortSignal
): Promise<void> {
  const token = localStorage.getItem('zest-llm-token')
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body),
    signal
  })

  if (!response.ok) {
    let message = `HTTP ${response.status}`
    try {
      const errBody: unknown = await response.json()
      if (errBody && typeof errBody === 'object' && 'message' in errBody) {
        const msg = (errBody as { message?: unknown }).message
        if (typeof msg === 'string' && msg) {
          message = msg
        }
      }
    } catch {
      const text = await response.text().catch(() => '')
      if (text) {
        message = text
      }
    }
    throw new Error(message)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('响应体不可读')
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let eventName = 'message'
  let dataLines: string[] = []

  const flush = () => {
    if (dataLines.length === 0) {
      return
    }
    onEvent(eventName, parseSseData(dataLines.join('\n')))
    dataLines = []
    eventName = 'message'
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        flush()
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      } else if (line === '\r' || line === '') {
        flush()
      }
    }
  }
  if (buffer.length > 0) {
    if (buffer.startsWith('event:')) {
      flush()
      eventName = buffer.slice(6).trim()
    } else if (buffer.startsWith('data:')) {
      dataLines.push(buffer.slice(5).trimStart())
    }
  }
  flush()
}
