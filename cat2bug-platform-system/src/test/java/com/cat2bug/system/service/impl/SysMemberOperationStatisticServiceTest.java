package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysMemberOperationStatistics;
import com.cat2bug.system.mapper.SysMemberOperationStatisticMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成员操作统计：日/周/月期间计算、mapper 委托与日结。
 */
public class SysMemberOperationStatisticServiceTest {

    @Mock
    private SysMemberOperationStatisticMapper sysMemberOperationStatisticMapper;

    @InjectMocks
    private SysMemberOperationStatisticServiceImpl sysMemberOperationStatisticService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void toPeriodRange_day_returnsSingleDay() {
        SysMemberOperationStatisticServiceImpl.PeriodRange range =
                SysMemberOperationStatisticServiceImpl.toPeriodRange("day", LocalDate.of(2026, 10, 8));
        assertEquals("2026-10-08", range.getStartDate());
        assertEquals("2026-10-08", range.getEndDate());
    }

    @Test
    public void toPeriodRange_week_returnsMondayToSunday() {
        SysMemberOperationStatisticServiceImpl.PeriodRange range =
                SysMemberOperationStatisticServiceImpl.toPeriodRange("week", LocalDate.of(2026, 10, 8));
        assertEquals("2026-10-05", range.getStartDate());
        assertEquals("2026-10-11", range.getEndDate());
    }

    @Test
    public void toPeriodRange_week_mondayBoundaryKeepsSameWeek() {
        SysMemberOperationStatisticServiceImpl.PeriodRange range =
                SysMemberOperationStatisticServiceImpl.toPeriodRange("week", LocalDate.of(2026, 10, 5));
        assertEquals("2026-10-05", range.getStartDate());
        assertEquals("2026-10-11", range.getEndDate());
    }

    @Test
    public void toPeriodRange_week_previousMonthBoundary() {
        SysMemberOperationStatisticServiceImpl.PeriodRange range =
                SysMemberOperationStatisticServiceImpl.toPeriodRange("week", LocalDate.of(2026, 10, 1));
        assertEquals("2026-09-28", range.getStartDate());
        assertEquals("2026-10-04", range.getEndDate());
    }

    @Test
    public void toPeriodRange_month_returnsFirstToLastDay() {
        SysMemberOperationStatisticServiceImpl.PeriodRange range =
                SysMemberOperationStatisticServiceImpl.toPeriodRange("month", LocalDate.of(2026, 10, 8));
        assertEquals("2026-10-01", range.getStartDate());
        assertEquals("2026-10-31", range.getEndDate());
    }

    @Test
    public void toPeriodLabel_formatsByTimeType() {
        LocalDate ref = LocalDate.of(2026, 10, 8);
        assertEquals("2026-10-08",
                SysMemberOperationStatisticServiceImpl.toPeriodLabel("day", ref,
                        SysMemberOperationStatisticServiceImpl.toPeriodRange("day", ref)));
        assertEquals("2026-10-05 ~ 2026-10-11",
                SysMemberOperationStatisticServiceImpl.toPeriodLabel("week", ref,
                        SysMemberOperationStatisticServiceImpl.toPeriodRange("week", ref)));
        assertEquals("2026-10",
                SysMemberOperationStatisticServiceImpl.toPeriodLabel("month", ref,
                        SysMemberOperationStatisticServiceImpl.toPeriodRange("month", ref)));
    }

    @Test
    public void statistic_delegatesToMapperAndSetsPeriod() {
        SysMemberOperationStatistics row = new SysMemberOperationStatistics();
        row.setUserId(1L);
        row.setCreateCount(2);
        when(sysMemberOperationStatisticMapper.selectMemberOperationStatistics(eq(99L), eq("2026-10-05"), eq("2026-10-11")))
                .thenReturn(Collections.singletonList(row));

        List<SysMemberOperationStatistics> list = sysMemberOperationStatisticService.statistic(99L, "week", "2026-10-08");

        assertEquals(1, list.size());
        assertEquals("2026-10-05 ~ 2026-10-11", list.get(0).getPeriod());
        verify(sysMemberOperationStatisticMapper).selectMemberOperationStatistics(eq(99L), eq("2026-10-05"), eq("2026-10-11"));
    }

    @Test
    public void statistic_month_passesMonthBounds() {
        when(sysMemberOperationStatisticMapper.selectMemberOperationStatistics(eq(1L), eq("2026-10-01"), eq("2026-10-31")))
                .thenReturn(Collections.emptyList());

        List<SysMemberOperationStatistics> list = sysMemberOperationStatisticService.statistic(1L, "month", "2026-10-08");

        assertEquals(0, list.size());
        verify(sysMemberOperationStatisticMapper).selectMemberOperationStatistics(eq(1L), eq("2026-10-01"), eq("2026-10-31"));
    }

    @Test
    public void generateByDate_passesDayBounds() {
        when(sysMemberOperationStatisticMapper.insertDailyFromLogs("2026-10-08 00:00:00", "2026-10-09 00:00:00"))
                .thenReturn(3);
        assertEquals(3, sysMemberOperationStatisticService.generateByDate("2026-10-08"));
        verify(sysMemberOperationStatisticMapper).insertDailyFromLogs("2026-10-08 00:00:00", "2026-10-09 00:00:00");
    }

    @Test
    public void parseReferenceDate_fallsBackToToday() {
        assertEquals(LocalDate.now(), SysMemberOperationStatisticServiceImpl.parseReferenceDate(null));
        assertEquals(LocalDate.now(), SysMemberOperationStatisticServiceImpl.parseReferenceDate(""));
        assertEquals(LocalDate.now(), SysMemberOperationStatisticServiceImpl.parseReferenceDate("not-a-date"));
        assertEquals(LocalDate.of(2026, 10, 8), SysMemberOperationStatisticServiceImpl.parseReferenceDate("2026-10-08"));
    }
}
