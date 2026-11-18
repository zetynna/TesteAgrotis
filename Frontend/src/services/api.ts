const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, init)
  } catch (error) {
    const reason = error instanceof Error ? ` (${error.message})` : ''
    throw new Error(`Não foi possível conectar ao servidor.${reason}`)
  }

  if (!response.ok) {
    let message = `A solicitação falhou (${response.status} ${response.statusText}).`
    const bodyText = await response.text().catch(() => '')

    try {
      const body: unknown = JSON.parse(bodyText)
      if (isRecord(body)) {
        if (typeof body.message === 'string') message = body.message
        else if (typeof body.error === 'string') message = body.error

        if (isRecord(body.details) && isRecord(body.details.campos)) {
          const fieldErrors = Object.entries(body.details.campos)
            .map(([field, reason]) => `${field}: ${String(reason)}`)
            .join('\n')
          if (fieldErrors) message = `${message}\n${fieldErrors}`
        }
      }
    } catch {
      if (bodyText) message = `${message}\n${bodyText}`
    }

    throw new Error(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
