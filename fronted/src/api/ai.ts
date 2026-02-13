import type { Result } from '@/types/recipe'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

export type AiChatMessage = { role: 'user' | 'assistant'; content: string }

export type AiChatResponse = {
  reply: string
  sources?: string[]
  usedRemoteModel?: boolean
}

export const AiAPI = {
  chat: async (payload: { message: string; history?: AiChatMessage[] }): Promise<Result<AiChatResponse>> => {
    const res = await http.post<BackendResult<AiChatResponse>>('/ai/chat', payload)
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  }
}

