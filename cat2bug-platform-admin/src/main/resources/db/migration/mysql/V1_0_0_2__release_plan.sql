-- 发版计划：新增表 sys_release_plan，sys_defect 增加 计划完成时间 / 发版计划 字段，并补菜单与权限
-- 幂等写法：建表 CREATE TABLE IF NOT EXISTS；加列沿用 V0_6_2_7 的 information_schema + PREPARE 模式；菜单按 perms 幂等插入

-- 1) 发版计划表
CREATE TABLE IF NOT EXISTS `sys_release_plan` (
  `release_plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '发版计划ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `release_plan_name` varchar(255) NOT NULL COMMENT '发版计划名称',
  `release_date` date DEFAULT NULL COMMENT '发版日期',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_by_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_by_id` bigint DEFAULT NULL COMMENT '更新者ID',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`release_plan_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='发版计划';

CREATE INDEX idx_sys_release_plan_project ON `sys_release_plan` (`project_id`);

-- 2) sys_defect 增加 计划完成时间（yyyy-MM-dd）
SET @exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_defect' AND COLUMN_NAME = 'plan_complete_time'
);
SET @q = IF(@exists = 0,
  'ALTER TABLE `sys_defect` ADD COLUMN `plan_complete_time` datetime DEFAULT NULL COMMENT ''计划完成时间'' AFTER `plan_end_time`',
  'SELECT 1'
);
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;

-- 3) sys_defect 增加 发版计划id
SET @exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_defect' AND COLUMN_NAME = 'release_plan_id'
);
SET @q = IF(@exists = 0,
  'ALTER TABLE `sys_defect` ADD COLUMN `release_plan_id` bigint DEFAULT NULL COMMENT ''发版计划ID'' AFTER `plan_complete_time`',
  'SELECT 1'
);
PREPARE s FROM @q;
EXECUTE s;
DEALLOCATE PREPARE s;

CREATE INDEX idx_sys_defect_release_plan ON `sys_defect` (`release_plan_id`);

-- 4) 菜单：发版计划（父菜单 2013 项目功能）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2160, '发版计划', 2013, 4, 'releasePlan', 'system/releasePlan/index', NULL, 1, 0, 'C', '0', '0', 'system:releasePlan:list', 'date', 'admin', NOW(), '', NULL, '', 'release-plan.manage'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:list');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2161, '发版计划查询', 2160, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:query', '#', 'admin', NOW(), '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:query');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2162, '发版计划新增', 2160, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:add', '#', 'admin', NOW(), '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:add');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2163, '发版计划修改', 2160, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:edit', '#', 'admin', NOW(), '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:releasePlan:edit');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2164, '发版计划删除', 2160, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:releasePlan:remove', '#', 'admin', NOW(), '', NULL, '', NULL
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
