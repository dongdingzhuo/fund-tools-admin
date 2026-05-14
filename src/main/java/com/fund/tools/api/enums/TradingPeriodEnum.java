package com.fund.tools.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

/**
 * 交易时段枚举
 * 定义基金交易的相关时间段
 */
@Getter
@AllArgsConstructor
public enum TradingPeriodEnum {
    
    /**
     * 早盘交易时段 09:30-15:10
     */
    MORNING_TRADING(
            LocalTime.of(9, 30),
            LocalTime.of(15, 10),
            "早盘交易时段"
    ),
    
    /**
     * 晚间数据更新时段 18:00-23:59:59
     */
    EVENING_UPDATE(
            LocalTime.of(18, 0),
            LocalTime.of(23, 59, 59),
            "晚间数据更新时段"
    ),
    
    /**
     * 实时数据更新时段 09:30-15:30
     */
    REALTIME_UPDATE(
            LocalTime.of(9, 30),
            LocalTime.of(15, 30),
            "实时数据更新时段"
    );
    
    /**
     * 开始时间
     */
    private final LocalTime startTime;
    
    /**
     * 结束时间
     */
    private final LocalTime endTime;
    
    /**
     * 描述
     */
    private final String description;
    
    /**
     * 判断当前时间是否在该时段内
     *
     * @param currentTime 当前时间
     * @return 是否在时段内
     */
    public boolean isInRange(LocalTime currentTime) {
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }
}
