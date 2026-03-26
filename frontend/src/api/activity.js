import request from '../utils/request'

export function getActivityList(params) {
  return request({
    url: '/admin/activities',
    method: 'get',
    params
  })
}

export function getActivityDetail(id) {
  return request({
    url: `/admin/activities/${id}`,
    method: 'get'
  })
}

export function createActivity(data) {
  return request({
    url: '/admin/activities',
    method: 'post',
    data
  })
}

export function updateActivity(id, data) {
  return request({
    url: `/admin/activities/${id}`,
    method: 'put',
    data
  })
}

export function deleteActivity(id, version) {
  return request({
    url: `/admin/activities/${id}`,
    method: 'delete',
    params: { version }
  })
}

export function submitReview(id, data) {
  return request({
    url: `/admin/activities/${id}/submit-review`,
    method: 'post',
    data
  })
}

export function withdrawReview(id, data) {
  return request({
    url: `/admin/activities/${id}/withdraw`,
    method: 'post',
    data
  })
}

export function approveActivity(id, data) {
  return request({
    url: `/admin/activities/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectActivity(id, data) {
  return request({
    url: `/admin/activities/${id}/reject`,
    method: 'post',
    data
  })
}

export function offlineActivity(id, data) {
  return request({
    url: `/admin/activities/${id}/offline`,
    method: 'post',
    data
  })
}

export function batchOfflineActivities(ids) {
  return request({
    url: '/admin/activities/batch-offline',
    method: 'post',
    data: { ids }
  })
}

export function batchDeleteActivities(ids) {
  return request({
    url: '/admin/activities/batch-delete',
    method: 'post',
    data: { ids }
  })
}

export function revokeSchedule(id, data) {
  return request({
    url: `/admin/activities/${id}/revoke-schedule`,
    method: 'post',
    data
  })
}

export function getActivityChangeLogs(id, params) {
  return request({
    url: `/admin/activities/${id}/change-logs`,
    method: 'get',
    params
  })
}

export function rollbackActivity(id, changeLogId, version) {
  return request({
    url: `/admin/activities/${id}/rollback/${changeLogId}`,
    method: 'post',
    params: { version }
  })
}

export function getTagList() {
  return request({
    url: '/admin/tags',
    method: 'get'
  })
}

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
