package com.fund.tools.api.controller;

import com.fund.tools.api.common.Result;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.service.HolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 节假日控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/holiday")
public class HolidayController {

    @Resource
    private HolidayService holidayService;

    /**
     * 判断指定日期是否为交易日
     *
     * @param date 日期（yyyy-MM-dd格式），不传则默认为今天
     * @return 是否为交易日
     */
    @GetMapping("/is-trading-day")
    public Result<Boolean> isTradingDay(@RequestParam(required = false) String date) {
        try {
            if (date == null || date.trim().isEmpty()) {
                date = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            }
            boolean isTradingDay = holidayService.isTradingDay(date);
            log.info("查询日期{}是否为交易日: {}", date, isTradingDay);
            return Result.success(isTradingDay);
        } catch (Exception e) {
            log.error("查询交易日失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期最近的交易日日期
     *
     * @param date 日期（yyyy-MM-dd格式），不传则默认为今天
     * @return 最近的交易日日期
     */
    @GetMapping("/nearest-trading-day")
    public Result<String> getNearestTradingDay(@RequestParam(required = false) String date) {
        try {
            if (date == null || date.trim().isEmpty()) {
                date = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            }
            
            // 先检查该日期是否是交易日（会自动获取缺失数据）
            if (holidayService.isTradingDay(date)) {
                log.info("日期{}是交易日", date);
                return Result.success(date);
            }
            
            // 如果不是交易日，查找最近的交易日
            String nearestTradingDay = holidayService.getNearestTradingDay(date);
            
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
}
