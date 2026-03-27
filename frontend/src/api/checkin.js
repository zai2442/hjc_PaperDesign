import request from '../utils/request'

export const startCheckIn = (activityId) => {
  return request.post(`/activities/${activityId}/checkin/start`)
}

export const stopCheckIn = (activityId) => {
  return request.post(`/activities/${activityId}/checkin/stop`)
}

export const getCheckInQrcode = (activityId) => {
  return request.get(`/activities/${activityId}/checkin/qrcode`)
}

export const scanCheckInQrcode = (activityId, token) => {
  return request.post(`/activities/${activityId}/checkin/scan`, { token })
}

export const manualCheckIn = (activityId, userId) => {
  return request.post(`/activities/${activityId}/checkin/manual`, { userId })
}

export const getCheckInStats = (activityId) => {
  return request.get(`/activities/${activityId}/checkin/stats`)
}
