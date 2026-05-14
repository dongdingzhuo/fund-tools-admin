package com.fund.tools.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fund.tools.api.mapper")
@EnableScheduling
@EnableAsync
public class FundToolsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundToolsAdminApplication.class, args);
    }
}
