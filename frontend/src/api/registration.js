import request from '../utils/request'

export function createRegistration(data) {
  return request({
    url: '/registrations',
    method: 'post',
    data
  })
}

export function cancelRegistration(id) {
  return request({
    url: `/registrations/${id}/cancel`,
    method: 'post'
  })
}

export function getMyRegistrations(params) {
  return request({
    url: '/registrations/my',
    method: 'get',
    params
  })
}

export function getRegistrationDetail(id) {
  return request({
    url: `/registrations/${id}`,
    method: 'get'
  })
}

export function getAdminRegistrations(params) {
  return request({
    url: '/admin/registrations',
    method: 'get',
    params
  })
}

export function auditRegistration(id, data) {
  return request({
    url: `/admin/registrations/${id}/audit`,
    method: 'post',
    data
  })
}

export function getRegistrationStats(params) {
  return request({
    url: '/admin/registrations/stats',
    method: 'get',
    params
  })
}

