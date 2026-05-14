package com.fund.tools.api.config;

/**
 * API配置常量类
 * 集中管理所有外部API的URL和相关配置
 */
public class ApiConstants {
    
    /**
     * 私有构造函数，防止实例化
     */
    private ApiConstants() {
        throw new IllegalStateException("Constant class");
    }
    
    // ==================== 基金数据API ====================
    
    /**
     * 天天基金实时数据API基础URL
     * 用于获取基金实时估算数据
     */
    public static final String TIAN_TIAN_FUND_API = "https://fundgz.1234567.com.cn/js/";
    
    /**
     * 东方财富F10历史数据API
     * 用于获取基金历史净值数据
     */
    public static final String EASTMONEY_F10_API = "https://fundf10.eastmoney.com/F10DataApi.aspx";
    
    // ==================== 节假日API ====================
    
    /**
     * 节假日查询API基础URL
     * 用于查询交易日、节假日等信息
     */
    public static final String HOLIDAY_API = "https://timor.tech/api/holiday/info/";
    
    // ==================== API调用配置 ====================
    
    /**
     * API请求超时时间（毫秒）
     */
    public static final int API_TIMEOUT_MS = 60000;
    
    /**
     * 快速API请求超时时间（毫秒）
     */
    public static final int QUICK_API_TIMEOUT_MS = 5000;
    
    /**
     * API请求间隔时间（毫秒），避免频繁调用被限流
     */
    public static final int API_REQUEST_INTERVAL_MS = 1000;
    
    /**
     * 节假日API请求间隔时间（毫秒）
     */
    public static final int HOLIDAY_API_INTERVAL_MS = 3000;
}
