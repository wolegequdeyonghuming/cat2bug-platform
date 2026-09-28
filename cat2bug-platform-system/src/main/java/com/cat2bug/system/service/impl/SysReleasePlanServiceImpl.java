package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysReleasePlan;
import com.cat2bug.system.mapper.SysReleasePlanMapper;
import com.cat2bug.system.service.ISysReleasePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 发版计划Service业务层处理
 *
 * @author yuzhantao
 */
@Service
public class SysReleasePlanServiceImpl implements ISysReleasePlanService
{
    @Autowired
    private SysReleasePlanMapper sysReleasePlanMapper;

    /**
     * 查询发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 发版计划集合
     */
    @Override
    public List<SysReleasePlan> selectSysReleasePlanList(SysReleasePlan sysReleasePlan)
    {
        return sysReleasePlanMapper.selectSysReleasePlanList(sysReleasePlan);
    }

    /**
     * 查询发版计划
     *
     * @param releasePlanId 发版计划主键
     * @return 发版计划
     */
    @Override
    public SysReleasePlan selectSysReleasePlanByReleasePlanId(Long releasePlanId)
    {
        return sysReleasePlanMapper.selectSysReleasePlanByReleasePlanId(releasePlanId);
    }

    /**
     * 新增发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 结果
     */
    @Override
    public int insertSysReleasePlan(SysReleasePlan sysReleasePlan)
    {
        sysReleasePlan.setCreateBy(SecurityUtils.getUsername());
        sysReleasePlan.setCreateById(SecurityUtils.getUserId());
        sysReleasePlan.setCreateTime(DateUtils.getNowDate());
        sysReleasePlan.setUpdateBy(SecurityUtils.getUsername());
        sysReleasePlan.setUpdateById(SecurityUtils.getUserId());
        sysReleasePlan.setUpdateTime(DateUtils.getNowDate());
        return sysReleasePlanMapper.insertSysReleasePlan(sysReleasePlan);
    }

    /**
     * 修改发版计划
     *
     * @param sysReleasePlan 发版计划
     * @return 结果
     */
    @Override
    public int updateSysReleasePlan(SysReleasePlan sysReleasePlan)
    {
        sysReleasePlan.setUpdateBy(SecurityUtils.getUsername());
        sysReleasePlan.setUpdateById(SecurityUtils.getUserId());
        sysReleasePlan.setUpdateTime(DateUtils.getNowDate());
        return sysReleasePlanMapper.updateSysReleasePlan(sysReleasePlan);
    }

    /**
     * 批量删除发版计划
     *
     * @param releasePlanIds 需要删除的发版计划主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysReleasePlanByReleasePlanIds(Long[] releasePlanIds)
    {
        sysReleasePlanMapper.clearDefectsOfReleasePlans(releasePlanIds);
        return sysReleasePlanMapper.deleteSysReleasePlanByReleasePlanIds(releasePlanIds);
    }

    /**
     * 查询发版计划下的缺陷列表
     *
     * @param releasePlanId 发版计划主键
     * @param sysDefect 缺陷
     * @return 缺陷集合
     */
    @Override
    public List<SysDefect> selectSysDefectList(Long releasePlanId, SysDefect sysDefect)
    {
        return sysReleasePlanMapper.selectSysDefectList(releasePlanId, sysDefect, SecurityUtils.getUserId(), DateUtils.getNowDate());
    }

    /**
     * 批量关联缺陷到发版计划
     *
     * @param releasePlanId 发版计划主键
     * @param defectIds 缺陷主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int associateDefects(Long releasePlanId, List<Long> defectIds)
    {
        if (defectIds == null || defectIds.isEmpty()) {
            return sysReleasePlanMapper.clearReleasePlanOfDefects(releasePlanId, java.util.Collections.emptyList());
        }
        sysReleasePlanMapper.clearReleasePlanOfDefects(releasePlanId, defectIds);
        return sysReleasePlanMapper.associateDefects(releasePlanId, defectIds);
    }
}
