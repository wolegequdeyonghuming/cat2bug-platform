package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysMemberOperationStatistics;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 成员操作日统计：H2 造数验证 聚合写入 / 单日 / 周月求和 / 项目隔离。
 */
public class SysMemberOperationStatisticMapperTest {

    private Connection h2Connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:member_operation_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        h2Connection = dataSource.getConnection();
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS sys_member_operation_statistic");
            stmt.execute("DROP TABLE IF EXISTS sys_defect_log");
            stmt.execute("DROP TABLE IF EXISTS sys_defect");
            stmt.execute("DROP TABLE IF EXISTS sys_user_project");
            stmt.execute("DROP TABLE IF EXISTS sys_user");
            stmt.execute("CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY, nick_name VARCHAR(32), user_name VARCHAR(32), avatar VARCHAR(255), del_flag CHAR(1))");
            stmt.execute("CREATE TABLE sys_user_project (user_project_id BIGINT PRIMARY KEY, user_id BIGINT, project_id BIGINT)");
            stmt.execute("CREATE TABLE sys_defect (defect_id BIGINT PRIMARY KEY, project_id BIGINT, del_flag CHAR(1))");
            stmt.execute("CREATE TABLE sys_defect_log (defect_log_id BIGINT PRIMARY KEY, defect_id BIGINT, create_by BIGINT, create_time TIMESTAMP, defect_log_type INT)");
            stmt.execute("CREATE TABLE sys_member_operation_statistic (id BIGINT AUTO_INCREMENT PRIMARY KEY, stat_date DATE, project_id BIGINT, user_id BIGINT, create_count INT, repair_count INT, verify_count INT, create_time TIMESTAMP, CONSTRAINT uk_stat_date_project_user UNIQUE (stat_date, project_id, user_id))");

            stmt.execute("INSERT INTO sys_user(user_id, nick_name, user_name, avatar, del_flag) VALUES (1, 'u1', 'zhangsan', 'a1', '0')");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, user_name, avatar, del_flag) VALUES (2, 'u2', 'lisi', 'a2', '0')");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, user_name, avatar, del_flag) VALUES (3, 'u3', 'wangwu', 'a3', '0')");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, user_name, avatar, del_flag) VALUES (4, 'u4', 'zhaoliu', 'a4', '0')");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (11, 1, 100)");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (12, 2, 100)");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (13, 3, 100)");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (14, 1, 200)");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (15, 4, 200)");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag) VALUES (101, 100, '0')");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag) VALUES (102, 100, '1')");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag) VALUES (201, 200, '0')");
            // 2026-10-05：u1 在 p100 新增1+修复1，u2 在 p100 验证1，u4 在 p200 新增1+修复1
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1001, 101, 1, PARSEDATETIME('2026-10-05 10:00:00', 'yyyy-MM-dd HH:mm:ss'), 0)");
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1002, 101, 1, PARSEDATETIME('2026-10-05 11:00:00', 'yyyy-MM-dd HH:mm:ss'), 5)");
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1003, 101, 2, PARSEDATETIME('2026-10-05 12:00:00', 'yyyy-MM-dd HH:mm:ss'), 3)");
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1004, 201, 4, PARSEDATETIME('2026-10-05 10:30:00', 'yyyy-MM-dd HH:mm:ss'), 0)");
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1005, 201, 4, PARSEDATETIME('2026-10-05 10:31:00', 'yyyy-MM-dd HH:mm:ss'), 5)");
            // 2026-10-06：u1 在 p100 关闭（验证）
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1006, 101, 1, PARSEDATETIME('2026-10-06 09:00:00', 'yyyy-MM-dd HH:mm:ss'), 6)");
            // 已删除缺陷 102 的日志：不应计入
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1007, 102, 1, PARSEDATETIME('2026-10-05 13:00:00', 'yyyy-MM-dd HH:mm:ss'), 0)");
            // 区间外 2026-10-04：不应计入
            stmt.execute("INSERT INTO sys_defect_log(defect_log_id, defect_id, create_by, create_time, defect_log_type) VALUES (1008, 101, 1, PARSEDATETIME('2026-10-04 15:00:00', 'yyyy-MM-dd HH:mm:ss'), 5)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.addMapper(SysMemberOperationStatisticMapper.class);

        DatabaseIdProvider databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("H2", "h2");
        ((org.apache.ibatis.mapping.VendorDatabaseIdProvider) databaseIdProvider).setProperties(properties);
        configuration.setDatabaseId(databaseIdProvider.getDatabaseId(dataSource));

        String mapperResource = "mapper/system/SysMemberOperationStatisticMapper.xml";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(mapperResource)) {
            assertNotNull(inputStream);
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    mapperResource,
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @After
    public void tearDown() throws Exception {
        if (h2Connection != null) {
            h2Connection.close();
        }
    }

    @Test
    public void insertDailyFromLogs_writesAggregatedRowsForDay() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            SysMemberOperationStatisticMapper mapper = sqlSession.getMapper(SysMemberOperationStatisticMapper.class);
            int rows = mapper.insertDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            assertEquals(3, rows);

            List<SysMemberOperationStatistics> list = mapper.selectMemberOperationStatistics(1L, "2026-10-05", "2026-10-05");
            assertEquals(4, list.size());

            SysMemberOperationStatistics u1 = find(list, 1L);
            assertEquals(1, u1.getCreateCount());
            assertEquals(1, u1.getRepairCount());
            assertEquals(0, u1.getVerifyCount());

            SysMemberOperationStatistics u2 = find(list, 2L);
            assertEquals(0, u2.getCreateCount());
            assertEquals(0, u2.getRepairCount());
            assertEquals(1, u2.getVerifyCount());

            SysMemberOperationStatistics u3 = find(list, 3L);
            assertEquals(0, u3.getCreateCount());
            assertEquals(0, u3.getRepairCount());
            assertEquals(0, u3.getVerifyCount());

            SysMemberOperationStatistics u4 = find(list, 4L);
            assertEquals(1, u4.getCreateCount());
            assertEquals(1, u4.getRepairCount());
            assertEquals(0, u4.getVerifyCount());
        }
    }

    @Test
    public void insertDailyFromLogs_isIdempotentOnReRun() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            SysMemberOperationStatisticMapper mapper = sqlSession.getMapper(SysMemberOperationStatisticMapper.class);
            mapper.deleteDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            mapper.insertDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            mapper.deleteDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            mapper.insertDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");

            List<SysMemberOperationStatistics> list = mapper.selectMemberOperationStatistics(1L, "2026-10-05", "2026-10-05");
            assertEquals(4, list.size());
            assertEquals(1, find(list, 1L).getCreateCount());
            assertEquals(1, find(list, 4L).getRepairCount());
        }
    }

    @Test
    public void selectMemberOperationStatistics_weekSumsAcrossDays() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            SysMemberOperationStatisticMapper mapper = sqlSession.getMapper(SysMemberOperationStatisticMapper.class);
            mapper.insertDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            mapper.insertDailyFromLogs("2026-10-06 00:00:00", "2026-10-07 00:00:00");

            List<SysMemberOperationStatistics> list = mapper.selectMemberOperationStatistics(1L, "2026-10-05", "2026-10-11");
            assertEquals(4, list.size());
            // u1：10-05 新增1 + 10-06 验证1
            assertEquals(1, find(list, 1L).getCreateCount());
            assertEquals(1, find(list, 1L).getVerifyCount());
            // u2：10-05 验证1
            assertEquals(1, find(list, 2L).getVerifyCount());
        }
    }

    @Test
    public void selectMemberOperationStatistics_isolatesToVisibleProjects() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            SysMemberOperationStatisticMapper mapper = sqlSession.getMapper(SysMemberOperationStatisticMapper.class);
            mapper.insertDailyFromLogs("2026-10-05 00:00:00", "2026-10-06 00:00:00");
            mapper.insertDailyFromLogs("2026-10-06 00:00:00", "2026-10-07 00:00:00");

            // 当前用户 2 仅属于项目 100：u4 不在其中，u1 只统计项目 100 的操作
            List<SysMemberOperationStatistics> list = mapper.selectMemberOperationStatistics(2L, "2026-10-05", "2026-10-11");
            assertEquals(3, list.size());
            assertTrue(list.stream().noneMatch(s -> Long.valueOf(4L).equals(s.getUserId())));
            assertEquals(1, find(list, 1L).getCreateCount());
            assertEquals(1, find(list, 1L).getVerifyCount());
        }
    }

    @Test
    public void selectMemberOperationStatistics_dayWithoutData_returnsZeros() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            SysMemberOperationStatisticMapper mapper = sqlSession.getMapper(SysMemberOperationStatisticMapper.class);
            List<SysMemberOperationStatistics> list = mapper.selectMemberOperationStatistics(1L, "2026-10-07", "2026-10-07");
            assertEquals(4, list.size());
            assertTrue(list.stream().allMatch(s -> s.getCreateCount() == 0 && s.getRepairCount() == 0 && s.getVerifyCount() == 0));
        }
    }

    private SysMemberOperationStatistics find(List<SysMemberOperationStatistics> list, long userId) {
        return list.stream()
                .filter(s -> Long.valueOf(userId).equals(s.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing user " + userId));
    }
}
