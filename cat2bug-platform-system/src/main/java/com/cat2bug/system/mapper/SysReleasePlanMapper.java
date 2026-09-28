package com.cat2bug.system.mapper;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysReleasePlan;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 发版计划Mapper接口
 *
 * @author yuzhantao
 */
public interface SysReleasePlanMapper
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
     * 删除发版计划
     *
     * @param releasePlanIds 需要删除的数据主键集合
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
    public List<SysDefect> selectSysDefectList(@Param("releasePlanId") Long releasePlanId, @Param("defect") SysDefect sysDefect, @Param("currentUserId") Long currentUserId, @Param("currentTime") Date currentTime);

    /**
     * 批量将缺陷关联到发版计划
     *
     * @param releasePlanId 发版计划主键
     * @param defectIds 缺陷主键集合
     * @return 结果
     */
    public int associateDefects(@Param("releasePlanId") Long releasePlanId, @Param("defectIds") List<Long> defectIds);

    /**
     * 清空发版计划下未被选中的缺陷关联
     *
     * @param releasePlanId 发版计划主键
     * @param keepIds 保留的缺陷主键集合
     * @return 结果
     */
    public int clearReleasePlanOfDefects(@Param("releasePlanId") Long releasePlanId, @Param("keepIds") List<Long> keepIds);

    /**
     * 删除发版计划时清空缺陷上的发版计划引用
     *
     * @param releasePlanIds 发版计划主键集合
     * @return 结果
     */
    public int clearDefectsOfReleasePlans(Long[] releasePlanIds);
}
