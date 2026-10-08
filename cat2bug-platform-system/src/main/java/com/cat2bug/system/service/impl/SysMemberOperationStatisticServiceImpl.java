package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysMemberOperationStatistics;
import com.cat2bug.system.mapper.SysMemberOperationStatisticMapper;
import com.cat2bug.system.service.ISysMemberOperationStatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 成员操作统计服务实现
 */
@Service
public class SysMemberOperationStatisticServiceImpl implements ISysMemberOperationStatisticService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SysMemberOperationStatisticMapper sysMemberOperationStatisticMapper;

    @Override
    public List<SysMemberOperationStatistics> statistic(Long currentUserId, String timeType, String date) {
        LocalDate reference = parseReferenceDate(date);
        PeriodRange range = toPeriodRange(timeType, reference);
        List<SysMemberOperationStatistics> list =
                sysMemberOperationStatisticMapper.selectMemberOperationStatistics(currentUserId, range.getStartDate(), range.getEndDate());
        String period = toPeriodLabel(timeType, reference, range);
        list.forEach(item -> item.setPeriod(period));
        return list;
    }

    @Override
    @Transactional
    public int generateDaily() {
        return generateByDate(LocalDate.now().minusDays(1).format(DATE_FORMAT));
    }

    @Override
    @Transactional
    public int generateByDate(String date) {
        LocalDate statDate = parseReferenceDate(date);
        String startTime = statDate.atStartOfDay().format(DATETIME_FORMAT);
        String endTime = statDate.plusDays(1).atStartOfDay().format(DATETIME_FORMAT);
        sysMemberOperationStatisticMapper.deleteDailyFromLogs(startTime, endTime);
        return sysMemberOperationStatisticMapper.insertDailyFromLogs(startTime, endTime);
    }

    /**
     * 计算日/周/月的起止日期
     */
    static PeriodRange toPeriodRange(String timeType, LocalDate reference) {
        if ("week".equalsIgnoreCase(timeType)) {
            LocalDate monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new PeriodRange(monday, monday.plusDays(6));
        }
        if ("month".equalsIgnoreCase(timeType)) {
            LocalDate first = reference.withDayOfMonth(1);
            return new PeriodRange(first, first.withDayOfMonth(first.lengthOfMonth()));
        }
        return new PeriodRange(reference, reference);
    }

    /**
     * 期间展示标签：日=yyyy-MM-dd / 周=yyyy-MM-dd ~ yyyy-MM-dd / 月=yyyy-MM
     */
    static String toPeriodLabel(String timeType, LocalDate reference, PeriodRange range) {
        if ("month".equalsIgnoreCase(timeType)) {
            return reference.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        if ("week".equalsIgnoreCase(timeType)) {
            return range.getStartDate() + " ~ " + range.getEndDate();
        }
        return range.getStartDate();
    }

    static LocalDate parseReferenceDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    static final class PeriodRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        PeriodRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        String getStartDate() {
            return startDate.format(DATE_FORMAT);
        }

        String getEndDate() {
            return endDate.format(DATE_FORMAT);
        }
    }
}
