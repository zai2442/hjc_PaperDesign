import request from '../utils/request'

export function getUserInfo() {
  return request.get('/users/me')
}

export function updateUserInfo(data) {
  return request.put('/users/me', data)
}

export function changePassword(data) {
  return request.post('/users/me/password', data)
}

export function getMyRegistrations(params) {
  return request.get('/registrations/my', { params })
}
