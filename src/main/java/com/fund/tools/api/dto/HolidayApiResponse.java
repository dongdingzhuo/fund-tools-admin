package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 节假日API响应DTO
 */
@Data
public class HolidayApiResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 类型信息
     */
    private TypeInfo type;

    /**
     * 节假日信息
     */
    private HolidayInfo holiday;

    /**
     * 类型信息内部类
     */
    @Data
    public static class TypeInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 类型（0-普通工作日，1-周末，2-法定节假日，3-调休补班）
         */
        private Integer type;
        
        /**
         * 名称
         */
        private String name;
        
        /**
         * 星期（0-周日，1-周一...6-周六）
         */
        private Integer week;
    }

    /**
     * 节假日信息内部类
     */
    @Data
    public static class HolidayInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 是否为节假日
         */
        private Boolean holiday;
        
        /**
         * 节假日名称
         */
        private String name;
        
        /**
         * 工资倍数
         */
        private Integer wage;
        
        /**
         * 是否为补班
         */
        private Boolean after;
        
        /**
         * 目标节假日
         */
        private String target;
        
        /**
         * 日期
         */
        private String date;
        
        /**
         * 休息天数
         */
        private Integer rest;
    }
}
