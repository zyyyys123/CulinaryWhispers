import type { Result } from '@/types/recipe'
import { http } from './http'

type BackendResult<T> = { code: number; message: string; data: T }

type UploadResponse = {
  url: string
  name: string
  size: number
  contentType: string
}

export const FileAPI = {
  uploadImage: async (file: File): Promise<Result<UploadResponse>> => {
    const form = new FormData()
    form.append('file', file)
    const res = await http.post<BackendResult<UploadResponse>>('/file/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return { code: res.data.code, message: res.data.message, data: res.data.data }
  }
}

