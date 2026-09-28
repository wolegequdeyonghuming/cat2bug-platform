package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysReleasePlan;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysReleasePlanService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.util.DefectListKeywordSupport;
import com.cat2bug.system.util.DefectListQuerySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 发版计划Controller
 *
 * @author yuzhantao
 */
@RestController
@RequestMapping("/system/releasePlan")
public class SysReleasePlanController extends BaseController
{
    @Autowired
    private ISysReleasePlanService sysReleasePlanService;
    @Autowired
    private ISysModuleService sysModuleService;
    @Autowired
    private ISysUserProjectService sysUserProjectService;

    /**
     * 查询发版计划列表
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysReleasePlan sysReleasePlan)
    {
        startPage();
        List<SysReleasePlan> list = sysReleasePlanService.selectSysReleasePlanList(sysReleasePlan);
        return getDataTable(list);
    }

    /**
     * 获取发版计划详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:query')")
    @GetMapping(value = "/{releasePlanId}")
    public AjaxResult getInfo(@PathVariable("releasePlanId") Long releasePlanId)
    {
        return success(sysReleasePlanService.selectSysReleasePlanByReleasePlanId(releasePlanId));
    }

    /**
     * 获取发版计划下的缺陷列表
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:query')")
    @GetMapping(value = "/{releasePlanId}/defect/list")
    public TableDataInfo listOfReleasePlan(@PathVariable("releasePlanId") Long releasePlanId, SysDefect sysDefect)
    {
        if(sysDefect.getParams()==null) {
            sysDefect.setParams(new HashMap<>());
        }
        if (sysDefect.getModuleId() != null && sysDefect.getModuleId() > 0) {
            Set<Long> moduleIds = sysModuleService.getAllChildIds(sysDefect.getProjectId(), sysDefect.getModuleId());
            if(moduleIds.size()>0) {
                sysDefect.getParams().put("moduleIdsOfProject", moduleIds);
            } else {
                sysDefect.getParams().put("moduleIdsOfProject", Arrays.asList(0));
            }
        }
        DefectListKeywordSupport.fillNameVersionKeywordHandleBy(sysDefect, sysUserProjectService);
        DefectListQuerySupport.startDefectListPage();
        List<SysDefect> list = sysReleasePlanService.selectSysDefectList(releasePlanId, sysDefect);
        TableDataInfo tableDataInfo = getDataTable(list);
        List<SysDefect> newList = new ArrayList<>(list);
        newList.forEach(l->{
            if(l.getHandleByList()!=null){
                Map<Long, SysUser> userMap = l.getHandleByList().stream().filter(h->h.getUserId()>0).collect(Collectors.toMap(SysUser::getUserId, i->i));
                List<SysUser> handleList = new ArrayList<>();
                if (l.getHandleBy() != null) {
                    for(int i=0;i<l.getHandleBy().size();i++) {
                        Long userId = l.getHandleBy().get(i);
                        if(userId!=null && userMap.containsKey(userId)) {
                            handleList.add(userMap.get(userId));
                        }
                    }
                }
                l.setHandleByList(handleList);
            }
        });
        tableDataInfo.setRows(newList);
        return tableDataInfo;
    }

    /**
     * 新增发版计划
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:add')")
    @Log(title = "发版计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysReleasePlan sysReleasePlan)
    {
        return toAjax(sysReleasePlanService.insertSysReleasePlan(sysReleasePlan));
    }

    /**
     * 修改发版计划
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:edit')")
    @Log(title = "发版计划", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysReleasePlan sysReleasePlan)
    {
        return toAjax(sysReleasePlanService.updateSysReleasePlan(sysReleasePlan));
    }

    /**
     * 批量关联缺陷到发版计划
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:edit')")
    @Log(title = "发版计划", businessType = BusinessType.UPDATE)
    @PutMapping("/{releasePlanId}/defect")
    public AjaxResult associateDefects(@PathVariable("releasePlanId") Long releasePlanId, @RequestBody Map<String, List<Long>> body)
    {
        List<Long> defectIds = body == null ? null : body.get("defectIds");
        if (defectIds == null) {
            defectIds = new ArrayList<>();
        }
        return toAjax(sysReleasePlanService.associateDefects(releasePlanId, defectIds));
    }

    /**
     * 删除发版计划
     */
    @PreAuthorize("@ss.hasPermi('system:releasePlan:remove')")
    @Log(title = "发版计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{releasePlanIds}")
    public AjaxResult remove(@PathVariable Long[] releasePlanIds)
    {
        return toAjax(sysReleasePlanService.deleteSysReleasePlanByReleasePlanIds(releasePlanIds));
    }
}
