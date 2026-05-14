package com.fund.tools.api.config;

import java.time.format.DateTimeFormatter;

/**
 * 日期格式常量类
 * 集中管理所有日期时间格式化器
 */
public class DateFormatConstants {
    
    /**
     * 私有构造函数，防止实例化
     */
    private DateFormatConstants() {
        throw new IllegalStateException("Constant class");
    }
    
    /**
     * 标准日期格式：yyyy-MM-dd
     * 例如：2026-05-14
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 估算时间格式：yyyy-MM-dd HH:mm
     * 例如：2026-05-14 15:00
     */
    public static final DateTimeFormatter DATETIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * 完整日期时间格式：yyyy-MM-dd HH:mm:ss
     * 例如：2026-05-14 15:30:00
     */
    public static final DateTimeFormatter DATETIME_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
