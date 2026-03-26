import { request } from './request'
import type { Activity, ActivityChangeLog, PageResponse, Registration, Result } from './types'

export type ActivityQuery = {
  page?: number
  size?: number
  keyword?: string
  status?: string
  createdBy?: number
}

export type ActivityCreate = {
  title: string
  summary?: string
  coverUrl?: string
  content?: string
  contentType?: string
  formSchema?: string
  channels?: string
  whitelistEnabled?: number
  stockTotal?: number
  perUserLimit?: number
}

export type ActivityUpdate = Partial<Omit<Activity, 'id'>> & { version: number }

export type WorkflowReq = {
  version: number
  publishAt?: string
  offlineAt?: string
  reason?: string
}

export async function createActivity(data: ActivityCreate) {
  const resp = await request.post<Result<number>>('/admin/activities', data)
  return resp.data
}

export async function pageActivities(params: ActivityQuery) {
  const resp = await request.get<Result<PageResponse<Activity>>>('/admin/activities', { params })
  return resp.data
}

export async function getActivity(id: number) {
  const resp = await request.get<Result<Activity>>(`/admin/activities/${id}`)
  return resp.data
}

export async function updateActivity(id: number, data: ActivityUpdate) {
  const resp = await request.put<Result<null>>(`/admin/activities/${id}`, data)
  return resp.data
}

export async function deleteActivity(id: number, version: number) {
  const resp = await request.delete<Result<null>>(`/admin/activities/${id}`, { params: { version } })
  return resp.data
}

export async function submitReview(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/submit-review`, data)
  return resp.data
}

export async function withdraw(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/withdraw`, data)
  return resp.data
}

export async function approve(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/approve`, data)
  return resp.data
}

export async function reject(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/reject`, data)
  return resp.data
}

export async function revokeSchedule(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/revoke-schedule`, data)
  return resp.data
}

export async function offline(id: number, data: WorkflowReq) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/offline`, data)
  return resp.data
}

export async function pageChangeLogs(id: number, page = 1, size = 20) {
  const resp = await request.get<Result<PageResponse<ActivityChangeLog>>>(`/admin/activities/${id}/change-logs`, {
    params: { page, size },
  })
  return resp.data
}

export async function rollback(id: number, changeLogId: number, version: number) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/rollback/${changeLogId}`, null, {
    params: { version },
  })
  return resp.data
}

export async function listWhitelist(id: number) {
  const resp = await request.get<Result<number[]>>(`/admin/activities/${id}/whitelist`)
  return resp.data
}

export async function addWhitelist(id: number, ids: number[]) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/whitelist/add`, { ids })
  return resp.data
}

export async function removeWhitelist(id: number, ids: number[]) {
  const resp = await request.post<Result<null>>(`/admin/activities/${id}/whitelist/remove`, { ids })
  return resp.data
}

export async function pageRegistrations(id: number, page = 1, size = 20) {
  const resp = await request.get<Result<PageResponse<Registration>>>(`/admin/activities/${id}/registrations`, {
    params: { page, size },
  })
  return resp.data
}

export function exportActivitiesUrl(params: ActivityQuery) {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    sp.set(k, String(v))
  })
  return `/api/v1/admin/activities/export?${sp.toString()}`
}

