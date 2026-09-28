package com.cat2bug.system.service;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysReleasePlan;

import java.util.List;

/**
 * 发版计划Service接口
 *
 * @author yuzhantao
 */
public interface ISysReleasePlanService
{
    /**
     * 查询发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 发版计划集合
     */
    public List<SysReleasePlan> selectSysReleasePlanList(SysReleasePlan sysReleasePlan);

    /**
     * 查询发版计划
     *
     * @param releasePlanId 发版计划主键
     * @return 发版计划
     */
    public SysReleasePlan selectSysReleasePlanByReleasePlanId(Long releasePlanId);

    /**
     * 新增发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 结果
     */
    public int insertSysReleasePlan(SysReleasePlan sysReleasePlan);

    /**
     * 修改发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 结果
     */
    public int updateSysReleasePlan(SysReleasePlan sysReleasePlan);

    /**
     * 批量删除发版计划
     *
     * @param releasePlanIds 需要删除的发版计划主键集合
     * @return 结果
     */
    public int deleteSysReleasePlanByReleasePlanIds(Long[] releasePlanIds);

    /**
     * 查询发版计划下的缺陷列表
     *
     * @param releasePlanId 发版计划主键
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    public List<SysDefect> selectSysDefectList(Long releasePlanId, SysDefect sysDefect);

    /**
     * 批量关联缺陷到发版计划
     *
     * @param releasePlanId 发版计划主键
     * @param defectIds 缺陷主键集合
     * @return 结果
     */
    public int associateDefects(Long releasePlanId, List<Long> defectIds);
}
