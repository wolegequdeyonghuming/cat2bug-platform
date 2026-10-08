package com.cat2bug.system.domain;

import lombok.Data;

/**
 * 成员操作统计表行（所有项目维度，按期间聚合）
 */
@Data
public class SysMemberOperationStatistics {
    /** 成员id */
    private Long userId;
    /** 成员昵称 */
    private String nickName;
    /** 成员账号 */
    private String userName;
    /** 成员头像 */
    private String avatar;
    /** 期间展示（日=yyyy-MM-dd / 周=yyyy-MM-dd ~ yyyy-MM-dd / 月=yyyy-MM） */
    private String period;
    /** 新增缺陷数 */
    private int createCount;
    /** 修复数 */
    private int repairCount;
    /** 验证数 */
    private int verifyCount;
    /** 合计 */
    private int total;
}
