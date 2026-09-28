-- 发版计划：新增表 sys_release_plan，sys_defect 增加 计划完成时间 / 发版计划 字段，并补菜单与权限
-- H2 幂等写法：CREATE TABLE / ADD COLUMN / CREATE INDEX 均支持 IF NOT EXISTS

-- 1) 发版计划表
CREATE TABLE IF NOT EXISTS sys_release_plan (
  release_plan_id bigint NOT NULL AUTO_INCREMENT COMMENT '发版计划ID',
  project_id bigint NOT NULL COMMENT '项目ID',
  release_plan_name varchar(255) NOT NULL COMMENT '发版计划名称',
  release_date date DEFAULT NULL COMMENT '发版日期',
  remark varchar(1000) DEFAULT NULL COMMENT '备注',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_by_id bigint DEFAULT NULL COMMENT '创建者ID',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_by_id bigint DEFAULT NULL COMMENT '更新者ID',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (release_plan_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_release_plan_project ON sys_release_plan (project_id);

-- 2) sys_defect 增加 计划完成时间（yyyy-MM-dd）
ALTER TABLE sys_defect ADD COLUMN IF NOT EXISTS plan_complete_time datetime DEFAULT NULL COMMENT '计划完成时间';

-- 3) sys_defect 增加 发版计划id
ALTER TABLE sys_defect ADD COLUMN IF NOT EXISTS release_plan_id bigint DEFAULT NULL COMMENT '发版计划ID';

CREATE INDEX IF NOT EXISTS idx_sys_defect_release_plan ON sys_defect (release_plan_id);

-- 4) 菜单：发版计划（父菜单 2013 项目功能）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2160, '发版计划', 2013, 4, 'releasePlan', 'system/releasePlan/index', NULL, 1, 0, 'C', '0', '0', 'system:releasePlan:list', 'date', 'admin', CURRENT_TIMESTAMP, '', NULL, '', 'release-plan.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:list');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2161, '发版计划查询', 2160, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:query', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:query');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2162, '发版计划新增', 2160, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:add', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:add');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2163, '发版计划修改', 2160, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:edit', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:edit');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2164, '发版计划删除', 2160, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:remove', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:remove');

-- 5) 角色授权：团队创建人(4)/项目创建人(6)/团队管理员(11)/项目管理员(12)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM (SELECT 4 AS role_id UNION ALL SELECT 6 UNION ALL SELECT 11 UNION ALL SELECT 12) r
CROSS JOIN `sys_menu` m
WHERE m.perms IN (
    'system:releasePlan:list',
    'system:releasePlan:query',
    'system:releasePlan:add',
    'system:releasePlan:edit',
    'system:releasePlan:remove'
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);
