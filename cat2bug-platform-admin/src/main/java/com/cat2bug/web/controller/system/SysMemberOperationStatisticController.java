package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysMemberOperationStatistics;
import com.cat2bug.system.service.ISysMemberOperationStatisticService;
import com.cat2bug.web.excel.ExcelHttpSupport;
import com.cat2bug.web.service.excel.SystemExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 成员操作统计（所有项目维度，读取日统计表，支持 日/周/月）
 */
@RestController
@RequestMapping("/system/member-operation/statistic")
public class SysMemberOperationStatisticController extends BaseController {
    @Autowired
    private ISysMemberOperationStatisticService sysMemberOperationStatisticService;
    @Autowired
    private SystemExcelExportService systemExcelExportService;

    /**
     * 成员操作统计列表（不分页）
     */
    @PreAuthorize("@ss.hasPermi('system:memberOperation:statistic:query')")
    @GetMapping(value = "/list")
    public AjaxResult list(@RequestParam("timeType") String timeType,
                           @RequestParam(value = "date", required = false) String date)
    {
        List<SysMemberOperationStatistics> list = sysMemberOperationStatisticService.statistic(getUserId(), timeType, date);
        return success(list);
    }

    /**
     * 手动触发指定日期日结（幂等）
     */
    @PreAuthorize("@ss.hasPermi('system:memberOperation:statistic:query')")
    @PostMapping(value = "/generate")
    public AjaxResult generate(@RequestParam(value = "date", required = false) String date)
    {
        return success(sysMemberOperationStatisticService.generateByDate(date));
    }

    /**
     * 成员操作统计导出
     */
    @PreAuthorize("@ss.hasPermi('system:memberOperation:statistic:query')")
    @Log(title = "成员操作统计", businessType = BusinessType.EXPORT)
    @PostMapping(value = "/export")
    public void export(HttpServletResponse response,
                       @RequestParam("timeType") String timeType,
                       @RequestParam(value = "date", required = false) String date) throws IOException
    {
        List<SysMemberOperationStatistics> list = sysMemberOperationStatisticService.statistic(getUserId(), timeType, date);
        byte[] workbook = systemExcelExportService.exportMemberOperationStatistics(list);
        ExcelHttpSupport.write(response, workbook, "成员操作统计.xlsx");
    }
}
