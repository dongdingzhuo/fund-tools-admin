package com.fund.tools.api.controller;

import com.fund.tools.api.common.Result;
import com.fund.tools.api.service.HolidayService;
import com.fund.tools.api.task.HolidayTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 节假日控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/holiday")
public class HolidayController {

    @Resource
    private HolidayService holidayService;

    @Resource
    private HolidayTask holidayTask;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 判断当前日期是否为交易日
     *
     * @return 是否为交易日
     */
    @GetMapping("/is-trading-day")
    public Result<Boolean> isTradingDay() {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            boolean isTradingDay = holidayService.isTradingDay(today);
            log.info("查询日期{}是否为交易日: {}", today, isTradingDay);
            return Result.success(isTradingDay);
        } catch (Exception e) {
            log.error("查询交易日失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前最近的交易日日期
     *
     * @return 最近的交易日日期
     */
    @GetMapping("/nearest-trading-day")
    public Result<String> getNearestTradingDay() {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            
            // 先检查今天是否是交易日
            if (holidayService.isTradingDay(today)) {
                log.info("今天{}是交易日", today);
                return Result.success(today);
            }
            
            // 如果不是交易日，查找最近的交易日
            String nearestTradingDay = holidayService.getNearestTradingDay(today);
            
            if (nearestTradingDay == null) {
                return Result.error("未找到最近的交易日");
            }
            
            log.info("最近交易日为: {}", nearestTradingDay);
            return Result.success(nearestTradingDay);
        } catch (Exception e) {
            log.error("查询最近交易日失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发初始化节假日数据（用于测试）
     *
     * @return 初始化结果
     */
    @PostMapping("/init")
    public Result<String> initHolidayData() {
        try {
            log.info("手动触发初始化节假日数据");
            holidayTask.initHolidayData();
            return Result.success("初始化完成，请查看日志获取详细信息");
        } catch (Exception e) {
            log.error("初始化节假日数据失败", e);
            return Result.error("初始化失败: " + e.getMessage());
        }
    }
}
