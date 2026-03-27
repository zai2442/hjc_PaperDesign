import request from '../utils/request'

export function getLogs(params) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params
  })
}
