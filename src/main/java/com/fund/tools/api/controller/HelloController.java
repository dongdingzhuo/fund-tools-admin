package com.fund.tools.api.controller;

import com.fund.tools.api.common.Result;
import com.fund.tools.api.service.AsyncInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Hello 控制器
 * 提供初始化接口，触发异步任务
 */
@Slf4j
@RestController
public class HelloController {

    @Resource
    private AsyncInitService asyncInitService;

    /**
     * 初始化接口
     * 触发三个异步任务：
     * 1. 同步节假日数据
     * 2. 删除旧数据
     * 3. 初始化历史基金数据
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        try {
            log.info("开始执行初始化任务（异步）");

            // 调用异步初始化服务
            asyncInitService.asyncSyncHolidayData();
            asyncInitService.asyncDeleteOldData();
            asyncInitService.asyncInitFundHistoryData();

            log.info("初始化任务已提交到异步线程池执行");
            return Result.success("初始化任务已启动，正在后台执行，请查看日志了解进度");
        } catch (Exception e) {
            log.error("初始化任务启动失败", e);
            return Result.error("初始化任务启动失败: " + e.getMessage());
        }
    }
}
