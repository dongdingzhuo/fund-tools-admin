package com.fund.tools.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 应用启动时数据库连接检查
 * 确保系统启动时就验证数据库连接是否正常
 */
@Slf4j
@Component
public class DatabaseConnectionChecker implements ApplicationRunner {

    @Resource
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========== 开始检查数据库连接 ==========");
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                log.info("✅ 数据库连接成功！");
                log.info("   - 数据库URL: {}", connection.getMetaData().getURL());
                log.info("   - 数据库产品: {}", connection.getMetaData().getDatabaseProductName());
                log.info("   - 数据库版本: {}", connection.getMetaData().getDatabaseProductVersion());
                log.info("   - 驱动名称: {}", connection.getMetaData().getDriverName());
            } else {
                log.error("❌ 数据库连接失败：连接为空或已关闭");
                throw new RuntimeException("数据库连接失败");
            }
        } catch (Exception e) {
            log.error("❌ 数据库连接检查失败", e);
            throw new RuntimeException("数据库连接检查失败: " + e.getMessage(), e);
        }
        
        log.info("========== 数据库连接检查完成 ==========");
    }
}
