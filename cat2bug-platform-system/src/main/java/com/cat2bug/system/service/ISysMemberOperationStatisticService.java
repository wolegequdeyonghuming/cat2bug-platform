package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysMemberOperationStatistics;

import java.util.List;

/**
 * 成员操作统计服务（读取日统计表，日/周/月聚合）
 */
public interface ISysMemberOperationStatisticService {
    /**
     * 统计当前用户可见项目的成员操作
     * @param currentUserId 当前用户id
     * @param timeType      时间维度：day/week/month
     * @param date          参考日期 yyyy-MM-dd（为空取今天）
     * @return 成员统计行
     */
    List<SysMemberOperationStatistics> statistic(Long currentUserId, String timeType, String date);

    /**
     * 定时任务入口：统计前一日并写入日统计表（幂等）
     * @return 写入行数
     */
    int generateDaily();

    /**
     * 手动触发指定日期日结（幂等）
     * @param date 统计日期 yyyy-MM-dd
     * @return 写入行数
     */
    int generateByDate(String date);
}
