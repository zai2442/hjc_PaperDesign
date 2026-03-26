export type Result<T> = {
  code: number
  message: string
  data: T
}

export type PageResponse<T> = {
  total: number
  page: number
  size: number
  records: T[]
}

export type Activity = {
  id: number
  title: string
  summary?: string
  coverUrl?: string
  content?: string
  contentType?: string
  formSchema?: string
  channels?: string
  whitelistEnabled?: number
  stockTotal?: number
  stockAvailable?: number
  perUserLimit?: number
  currentVariant?: string
  status: string
  auditReason?: string
  auditBy?: number
  auditAt?: string
  publishAt?: string
  offlineAt?: string
  createdBy: number
  updatedBy: number
  createdAt?: string
  updatedAt?: string
  version: number
}

export type ActivityChangeLog = {
  id: number
  activityId: number
  operatorId?: number
  opType: string
  beforeData?: string
  afterData?: string
  diffData?: string
  createdAt?: string
}

export type Registration = {
  id: number
  activityId: number
  userId: number
  status: string
  extraData?: string
  createdAt?: string
}

