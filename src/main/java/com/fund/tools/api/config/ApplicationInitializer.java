package com.fund.tools.api.config;

import com.fund.tools.api.task.HolidayTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 应用启动初始化器
 */
@Slf4j
@Component
public class ApplicationInitializer implements CommandLineRunner {

    @Resource
    private HolidayTask holidayTask;

    @Override
    public void run(String... args) throws Exception {
        log.info("应用启动，开始初始化节假日数据");
        try {
            holidayTask.initHolidayData();
            log.info("节假日数据初始化完成");
        } catch (Exception e) {
            log.error("节假日数据初始化失败", e);
        }
    }
}
