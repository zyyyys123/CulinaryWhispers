export type NotificationVO = {
  id: string
  type: number
  targetType: number
  targetId: string
  content: string
  isRead: boolean
  createTime: string
  fromUserId: string
  fromNickname?: string
  fromAvatarUrl?: string
}

