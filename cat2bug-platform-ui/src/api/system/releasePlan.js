import request from '@/utils/request'

// 查询发版计划列表
export function listReleasePlan(query) {
  return request({
    url: '/system/releasePlan/list',
    method: 'get',
    params: query
  })
}

// 查询发版计划详细
export function getReleasePlan(releasePlanId) {
  return request({
    url: '/system/releasePlan/' + releasePlanId,
    method: 'get'
  })
}

// 查询发版计划下的缺陷列表
export function listDefectOfReleasePlan(releasePlanId, query) {
  return request({
    url: '/system/releasePlan/' + releasePlanId + '/defect/list',
    method: 'get',
    params: query
  })
}

// 新增发版计划
export function addReleasePlan(data) {
  return request({
    url: '/system/releasePlan',
    method: 'post',
    data: data
  })
}

// 修改发版计划
export function updateReleasePlan(data) {
  return request({
    url: '/system/releasePlan',
    method: 'put',
    data: data
  })
}

// 批量关联缺陷到发版计划
export function associateDefects(releasePlanId, data) {
  return request({
    url: '/system/releasePlan/' + releasePlanId + '/defect',
    method: 'put',
    data: data
  })
}

// 删除发版计划
export function delReleasePlan(releasePlanId) {
  return request({
    url: '/system/releasePlan/' + releasePlanId,
    method: 'delete'
  })
}
