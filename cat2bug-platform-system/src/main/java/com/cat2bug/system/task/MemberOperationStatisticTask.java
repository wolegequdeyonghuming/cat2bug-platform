package com.cat2bug.system.task;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.service.ISysMemberOperationStatisticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 成员操作统计日结任务（sys_job 配置 invoke_target = memberOperationStatisticTask.generateDaily()）
 */
@Component("memberOperationStatisticTask")
public class MemberOperationStatisticTask {
    private static final Logger log = LoggerFactory.getLogger(MemberOperationStatisticTask.class);

    @Autowired
    private ISysMemberOperationStatisticService sysMemberOperationStatisticService;

    /**
     * 统计前一日成员操作并写入日统计表
     */
    public void generateDaily() {
        int rows = sysMemberOperationStatisticService.generateDaily();
        log.info(StringUtils.format("成员操作统计日结完成，写入 {} 行", rows));
    }

    /**
     * 手动触发指定日期日结
     * @param date 统计日期 yyyy-MM-dd
     */
    public void generateByDate(String date) {
        int rows = sysMemberOperationStatisticService.generateByDate(date);
        log.info(StringUtils.format("成员操作统计日结完成，日期：{}，写入 {} 行", date, rows));
    }
}
