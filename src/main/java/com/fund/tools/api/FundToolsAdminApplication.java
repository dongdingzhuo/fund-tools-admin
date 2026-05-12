package com.fund.tools.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fund.tools.api.mapper")
public class FundToolsAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundToolsAdminApplication.class, args);
    }
}
