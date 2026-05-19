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
     * 2. 删除旧数据（3个月前的历史数据 + 非自选基金数据）
     * 3. 初始化基金数据（根据时间段自动选择实时或历史接口）
     *    - 交易日 09:30-15:30：调用实时接口获取最新价格和名称
     *    - 交易日 18:00-24:00：调用F10历史接口获取历史净值数据
     *    - 其他时间：跳过基金数据更新
     * 
     * 建议每10分钟调用一次，系统会自动判断是否需要更新
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
