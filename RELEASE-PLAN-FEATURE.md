# 缺陷新增字段 + 发版计划管理 —— 实施方案

## 需求
1. 缺陷表增加两个字段（均非必填）：
   - 计划完成时间
   - 发版计划（下拉框）
2. 增加管理页面「发版计划」：增删改查 + 手动关联缺陷弹窗
3. 缺陷管理页面增加发版计划筛选

## 已确认设计
- 发版计划按项目隔离（表加 `project_id`），管理页与缺陷下拉框按当前项目过滤
- 计划完成时间：日期精度 `yyyy-MM-dd`
- 发版计划字段：名称 + 发版日期 + 备注
- 关联缺陷：复选框多选弹窗（已关联默认勾选，保存时批量写入）

---

## 一、数据库迁移

### 新增 `cat2bug-platform-admin/src/main/resources/db/migration/mysql/V1_0_0_2__release_plan.sql`
```sql
-- 发版计划表
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

-- sys_defect 增加列（沿用 V0_6_2_7 幂等 PREPARE 模式）
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='sys_defect' AND COLUMN_NAME='plan_complete_time');
SET @q = IF(@exists=0, 'ALTER TABLE `sys_defect` ADD COLUMN `plan_complete_time` datetime DEFAULT NULL COMMENT ''计划完成时间'' AFTER `plan_end_time`', 'SELECT 1');
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;
-- release_plan_id 同上 AFTER plan_complete_time
CREATE INDEX idx_sys_defect_release_plan ON `sys_defect` (`release_plan_id`);

-- 菜单 2160-2164（父菜单 2013）+ 角色 4/6/11/12 授权（仿 V0_6_2_14 幂等 INSERT）
-- 2160 C 类: path='releasePlan' component='system/releasePlan/index' perms='system:releasePlan:list' menu_name_i18n_key='release-plan.manage'
-- 2161-2164 F 类: system:releasePlan:query / add / edit / remove
```

### 新增 `cat2bug-platform-admin/src/main/resources/db/migration/h2/V1_0_0_2__release_plan.sql`
H2 语法：`CREATE TABLE IF NOT EXISTS`、`ALTER TABLE sys_defect ADD COLUMN IF NOT EXISTS`、`CREATE INDEX IF NOT EXISTS`，菜单/角色授权与 mysql 一致。

### 同步全量 dump
- `h2-schema.sql`：`sys_defect` 建表加 `plan_complete_time`/`release_plan_id`；新增 `sys_release_plan` 建表；`sys_menu`/`sys_role_menu` 段追加 2160-2164。
- `sql/cat2bug_platform.sql`：同样三处同步。

---

## 二、后端 SysDefect 字段

### `cat2bug-platform-common/src/main/java/com/cat2bug/common/core/domain/entity/SysDefect.java`
在 `planEndTime` 后追加：
```java
/** 计划完成时间 */
@JsonFormat(pattern = "yyyy-MM-dd")
@Excel(name = "计划完成时间", i18nNameKey = "plan-complete-time", width = 30, dateFormat = "yyyy-MM-dd", type = Excel.Type.EXPORT)
private Date planCompleteTime;

/** 发版计划id */
private Long releasePlanId;

/** 发版计划名称 */
private String releasePlanName;
```

### `cat2bug-platform-system/src/main/resources/mapper/system/SysDefectMapper.xml`
1. resultMap `SysDefectResult` 追加 3 个 result 映射。
2. 3 处 select 片段（`selectSysDefectVo`、`selectSysDefectListVo`、`selectSysDefectListVo databaseId="h2"`）：
   - SELECT 加 `d.plan_complete_time, d.release_plan_id, rp.release_plan_name,`
   - JOIN 加 `LEFT JOIN sys_release_plan rp ON rp.release_plan_id = d.release_plan_id`
3. `insertSysDefect`：列与 values 各加 `plan_complete_time`、`release_plan_id` 的 `<if>`。
4. `batchInsertSysDefect`：列串加 `plan_complete_time,release_plan_id`，values 加 `#{defect.planCompleteTime},#{defect.releasePlanId}`。
5. `updateSysDefect`：加 `plan_complete_time`、`release_plan_id` 及对应 `params.clearXxx` 清空模式。
6. `selectSysDefectList` WHERE（`testPlanId` 之后）：
```xml
<if test="defect.releasePlanId != null"> and d.release_plan_id = #{defect.releasePlanId}</if>
```

### `cat2bug-platform-system/src/main/java/com/cat2bug/system/util/DefectChangeUtil.java`
`diff()` 中 `planEndTime` 之后追加：
```java
addTimeChange(changes, "planCompleteTime", oldDefect.getPlanCompleteTime(), newDefect.getPlanCompleteTime());
addNumberChange(changes, "releasePlanId", oldDefect.getReleasePlanId(), newDefect.getReleasePlanId());
```

### `cat2bug-platform-admin/src/main/java/com/cat2bug/web/controller/system/SysDefectController.java`
- 注入 `ISysReleasePlanService`
- `config()` 在 `ret.put("tabs",...)` 后加：
```java
SysReleasePlan releasePlan = new SysReleasePlan();
releasePlan.setProjectId(userConfig.getCurrentProjectId());
ret.put("releasePlans", sysReleasePlanService.selectSysReleasePlanList(releasePlan));
```

---

## 三、后端 SysReleasePlan 模块（新）

### `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysReleasePlan.java`
`@Data @EqualsAndHashCode(callSuper=false)`，extends `BaseEntity`：
- `Long releasePlanId; Long projectId; String releasePlanName;`
- `@JsonFormat(pattern="yyyy-MM-dd") Date releaseDate;`
- `Long createById; Long updateById;`
- （`createBy/createTime/updateBy/updateTime/remark` 来自 `BaseEntity`）

### `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/SysReleasePlanMapper.java` + `resources/mapper/system/SysReleasePlanMapper.xml`
方法：
- `selectSysReleasePlanList(SysReleasePlan)`：`WHERE project_id=#{projectId}` + `release_plan_name like`，`ORDER BY release_date DESC, create_time DESC`
- `selectSysReleasePlanByReleasePlanId(Long)`
- `insertSysReleasePlan` / `updateSysReleasePlan`（`<trim>` 动态列）
- `deleteSysReleasePlanByReleasePlanIds(String[]/Long[])`：硬删 `DELETE WHERE release_plan_id IN (...)`
- `selectSysDefectList(@Param("releasePlanId") Long, @Param("defect") SysDefect)`：`<include refid="com.cat2bug.system.mapper.SysDefectMapper.selectSysDefectVo"/> WHERE d.release_plan_id=#{releasePlanId} AND d.del_flag='0'` + 复用缺陷列表过滤
- `associateDefects(@Param("releasePlanId") Long, @Param("defectIds") List<Long>)`：`UPDATE sys_defect SET release_plan_id=#{releasePlanId} WHERE del_flag='0' AND defect_id IN (...)`
- `clearReleasePlanOfDefects(@Param("releasePlanId") Long, @Param("keepIds") List<Long>)`：`UPDATE sys_defect SET release_plan_id=NULL WHERE del_flag='0' AND release_plan_id=#{releasePlanId} AND defect_id NOT IN (...)`

### `cat2bug-platform-system/src/main/java/com/cat2bug/system/service/ISysReleasePlanService.java` + `impl/SysReleasePlanServiceImpl.java`
- `selectSysReleasePlanList` / `selectSysReleasePlanByReleasePlanId`
- `insertSysReleasePlan`：填 createById/createTime/updateById/updateTime
- `updateSysReleasePlan`：填 updateById/updateTime
- `deleteSysReleasePlanByReleasePlanIds`：先 `UPDATE sys_defect SET release_plan_id=NULL WHERE release_plan_id IN (...)` 再 DELETE
- `selectSysDefectList(Long, SysDefect)`
- `@Transactional associateDefects(Long id, List<Long> defectIds)`：先 `clearReleasePlanOfDefects(id, defectIds)` 再 `associateDefects(id, defectIds)`，实现增删同步

### `cat2bug-platform-admin/src/main/java/com/cat2bug/web/controller/system/SysReleasePlanController.java`
`@RequestMapping("/system/releasePlan")`：
- `GET /list`（`system:releasePlan:list`）
- `GET /{releasePlanId}`（`system:releasePlan:query`）
- `GET /{releasePlanId}/defect/list`（`system:releasePlan:query`，仿 SysPlanController.listOfPlan 处理 moduleIdsOfProject）
- `POST`（`system:releasePlan:add`，`@Log` INSERT）
- `PUT`（`system:releasePlan:edit`，`@Log` UPDATE）
- `PUT /{releasePlanId}/defect`（`system:releasePlan:edit`，`@Log` UPDATE，body `{defectIds:[]}`）
- `DELETE /{releasePlanIds}`（`system:releasePlan:remove`，`@Log` DELETE）

---

## 四、前端

### API：新建 `src/api/system/releasePlan.js`
`listReleasePlan / getReleasePlan / addReleasePlan / updateReleasePlan / delReleasePlan / listDefectOfReleasePlan(planId, query) / associateDefects(planId, {defectIds})`

### 缺陷表单
- `src/components/Defect/DefectFormOrderedFields.vue`：`planTime` 分支后追加两个分支
  - `planCompleteTime`：`el-date-picker type="date" value-format="yyyy-MM-dd"`
  - `releasePlanId`：`el-select`，options 取 `config.releasePlans`，label=`releasePlanName` value=`releasePlanId`
- `src/components/Defect/AddDefect.vue`：`data().form` 与 `reset()` 加 `planCompleteTime:null, releasePlanId:null`；`readAddFormCache/saveAddFormCache` 带 `releasePlanId`
- `src/components/Defect/EditDefectDialog.vue`：`reset()` 加两字段（`getDefectInfo` 直接回填）

### 字段注册（接入"显示字段/字段管理"体系）
- `src/utils/defect-form-field-order.js`：`DEFAULT_FORM_BUILTIN_ORDER` 在 `'planStartTime'` 后加 `'planCompleteTime','releasePlanId'`
- `src/utils/defect-field-layout.js`：
  - `TABLE_KEY_TO_BUILTIN_FIELD_KEY` 加 `'plan-complete-time':'planCompleteTime'`、`'release-plan':'releasePlanId'`
  - `EXCEL_COL_KEY_TO_BUILTIN_FIELD_KEY` 加 `planCompleteTime:'planCompleteTime'`、`releasePlanId:'releasePlanId'`

### 缺陷列表
- `src/views/system/defect/list/table-options.js`：加两列 `plan-complete-time`(prop `planCompleteTime`)、`release-plan`(prop `releasePlanName`)
- `src/views/system/defect/list/table.vue`：`query` prop 默认加 `releasePlanId:null`；列渲染加 `planCompleteTime` 日期格式化分支
- `src/views/system/defect/index.vue`：`queryParams` 加 `releasePlanId:null`；搜索表单加发版计划 `el-select`（`config.releasePlans`，`@change="handleQuery()"`）
- `src/views/system/defect/list/excel.vue`：`COLS` 加两列（`editable:true`）；行→payload 映射补 `planCompleteTime`/`releasePlanId` 与 `params.clearXxx`

### 发版计划管理页：新建 `src/views/system/releasePlan/index.vue`（仿 plan/index.vue 精简版）
- `projectId` computed：`parseInt(this.$store.state.user.config.currentProjectId)`
- `getList()`：`listReleasePlan({...queryParams, projectId})`
- 列：`releasePlanName`、`releaseDate`、`remark`、`updateTime` + 操作（修改/删除/关联缺陷，`v-hasPermi`）
- 新增/编辑弹窗：名称、发版日期(`type="date"`)、备注
- 关联缺陷弹窗：并行 `listDefectOfReleasePlan(id,{projectId})`（预勾选）+ `listDefect({projectId,pageSize:9999})`（全项目缺陷），`el-table type="selection"`，保存 `associateDefects(id,{defectIds})`

### 路由
由数据库菜单驱动（2160 → `system/releasePlan/index`），无需改 `router/index.js`。

---

## 五、i18n

前端 7 个 json（`src/utils/i18n/i18n-{zh-CN,zh-TW,en-US,ja-JP,ko-KR,ru,ar}.json`）各加：
```json
"release-plan.manage": "发版计划",
"release-plan": "发版计划",
"release-plan.name": "发版计划名称",
"release-date": "发版日期",
"plan-complete-time": "计划完成时间",
"release-plan.create": "新建发版计划",
"release-plan.edit": "修改发版计划",
"release-plan.associate-defect": "关联缺陷",
"release-plan.please-select": "请选择发版计划",
"release-plan.enter-name": "请输入发版计划名称",
"release-plan.select-defect": "选择缺陷"
```
（各语言提供对应翻译）

后端 8 个 `cat2bug-platform-admin/src/main/resources/i18n/messages*.properties` 可选加：
```properties
releasePlan.create-fail=发版计划创建失败!
releasePlan.not-find=没有找到指定发版计划!
releasePlan.delete-fail=删除发版计划失败!
```

---

## 六、验证
- 后端编译：`mvn -q -pl cat2bug-platform-admin -am compile`
- 前端：`npm run lint` + `npm run build:prod`
- 手测：缺陷新建/编辑表单两字段；缺陷列表发版计划筛选；发版计划页增删改查 + 关联缺陷（勾选/取消→保存后同步）
