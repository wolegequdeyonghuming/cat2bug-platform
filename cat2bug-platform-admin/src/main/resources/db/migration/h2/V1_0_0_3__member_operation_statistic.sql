-- 成员操作统计：新增日统计表 sys_member_operation_statistic + 定时任务 + 顶级菜单与权限
-- H2 幂等写法：CREATE TABLE IF NOT EXISTS；sys_job / sys_menu 按唯一标识 NOT EXISTS 插入

-- 1) 成员操作日统计表
CREATE TABLE IF NOT EXISTS sys_member_operation_statistic (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  stat_date date NOT NULL COMMENT '统计日期',
  project_id bigint NOT NULL COMMENT '项目id',
  user_id bigint NOT NULL COMMENT '成员id',
  create_count int NOT NULL DEFAULT 0 COMMENT '新增缺陷数',
  repair_count int NOT NULL DEFAULT 0 COMMENT '修复数',
  verify_count int NOT NULL DEFAULT 0 COMMENT '验证数',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  CONSTRAINT uk_stat_date_project_user UNIQUE (stat_date, project_id, user_id)
);

-- 2) 定时任务：每日 00:10 统计前一日成员操作
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT '成员操作统计日结', 'DEFAULT', 'memberOperationStatisticTask.generateDaily()', '0 10 0 * * ?', '3', '1', '0', 'admin', CURRENT_TIMESTAMP, '每日00:10统计前一日成员操作'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'memberOperationStatisticTask.generateDaily()');

-- 3) 顶级菜单：成员操作统计（挂在「团队」菜单 2043 下）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2170, '成员操作统计', 2043, 1, 'member-operation', 'system/memberOperation/statistic/index', NULL, 1, 0, 'C', '0', '0', 'system:memberOperation:statistic:query', 'chart', 'admin', CURRENT_TIMESTAMP, '', NULL, '成员操作统计菜单', 'member-operation.statistic'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:memberOperation:statistic:query');

-- 4) 角色授权：团队创建人(4)/项目创建人(6)/团队管理员(11)/项目管理员(12)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM (SELECT 4 AS role_id UNION ALL SELECT 6 UNION ALL SELECT 11 UNION ALL SELECT 12) r
CROSS JOIN `sys_menu` m
WHERE m.perms = 'system:memberOperation:statistic:query'
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);
