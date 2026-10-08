import request from '@/utils/request'

/** 成员操作统计列表（所有项目维度，日/周/月） */
export function listMemberOperationStatistic(query) {
  return request({
    url: '/system/member-operation/statistic/list',
    method: 'get',
    params: query
  })
}

/** 手动触发指定日期日结 */
export function generateMemberOperationStatistic(date) {
  return request({
    url: '/system/member-operation/statistic/generate',
    method: 'post',
    params: { date }
  })
}
