import request from '../utils/request'

export function getStatsOverview() {
  return request({
    url: '/admin/stats/overview',
    method: 'get'
  })
}

export function getActivityTrends(params) {
  return request({
    url: '/admin/stats/trends',
    method: 'get',
    params
  })
}

export function getParticipationRanking(params) {
  return request({
    url: '/admin/stats/ranking',
    method: 'get',
    params
  })
}
