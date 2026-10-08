package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysMemberOperationStatistic;
import com.cat2bug.system.domain.SysMemberOperationStatistics;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成员操作日统计接口
 */
public interface SysMemberOperationStatisticMapper {
    /**
     * 从缺陷日志聚合指定时间区间（含边界）写入日统计表（幂等 upsert）
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间（不含）yyyy-MM-dd HH:mm:ss
     * @return 写入行数
     */
    int insertDailyFromLogs(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 删除时间区间内的日统计（MySQL 为幂等无操作，H2 为先删后插，保证可重跑）
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间（不含）yyyy-MM-dd HH:mm:ss
     * @return 删除行数
     */
    int deleteDailyFromLogs(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 按日期区间统计当前用户可见项目的成员操作（日=单日，周/月=区间求和）
     * @param currentUserId 当前用户id
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @return 成员统计行
     */
    List<SysMemberOperationStatistics> selectMemberOperationStatistics(@Param("currentUserId") Long currentUserId,
                                                                        @Param("startDate") String startDate,
                                                                        @Param("endDate") String endDate);
}
