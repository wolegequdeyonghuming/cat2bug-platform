package com.cat2bug.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 成员操作日统计（定时任务按 日期×项目×成员 汇总写入）
 */
@Data
public class SysMemberOperationStatistic {
    /** 主键 */
    private Long id;
    /** 统计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date statDate;
    /** 项目id */
    private Long projectId;
    /** 成员id */
    private Long userId;
    /** 新增缺陷数 */
    private int createCount;
    /** 修复数 */
    private int repairCount;
    /** 验证数 */
    private int verifyCount;
    /** 创建时间 */
    private Date createTime;
}
