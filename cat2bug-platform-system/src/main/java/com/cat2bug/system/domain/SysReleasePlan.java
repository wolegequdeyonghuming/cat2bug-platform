package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 发版计划对象 sys_release_plan
 *
 * @author yuzhantao
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysReleasePlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 发版计划id */
    private Long releasePlanId;

    /** 项目id */
    private Long projectId;

    /** 发版计划名称 */
    private String releasePlanName;

    /** 发版日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date releaseDate;
}
