package com.fund.tools.api.config;

/**
 * 批量处理配置常量类
 * 集中管理批量操作的相关配置
 */
public class BatchConstants {
    
    /**
     * 私有构造函数，防止实例化
     */
    private BatchConstants() {
        throw new IllegalStateException("Constant class");
    }
    
    // ==================== 批量更新配置 ====================
    
    /**
     * 每次最多更新的基金数量，防止内存溢出
     */
    public static final int MAX_UPDATE_COUNT = 5;
    
    /**
     * 节假日数据批量保存的大小
     */
    public static final int HOLIDAY_BATCH_SIZE = 7;
    
    // ==================== 数据刷新配置 ====================
    
    /**
     * 实时数据刷新阈值（分钟）
     * 数据时间超过此值才需要刷新
     */
    public static final int REALTIME_REFRESH_THRESHOLD_MINUTES = 30;
    
    /**
     * 历史数据刷新时间点（小时）
     * 数据时间在此时间之前才需要刷新
     */
    public static final int HISTORY_REFRESH_HOUR = 18;
}
