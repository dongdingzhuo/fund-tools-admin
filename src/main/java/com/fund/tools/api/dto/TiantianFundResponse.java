package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 天天基金实时数据响应DTO
 */
@Data
public class TiantianFundResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 基金代码
     */
    private String fundcode;

    /**
     * 基金名称
     */
    private String name;

    /**
     * 净值日期
     */
    private String jzrq;

    /**
     * 单位净值
     */
    private String dwjz;

    /**
     * 估算值
     */
    private String gsz;

    /**
     * 估算涨跌幅
     */
    private String gszzl;

    /**
     * 估算时间
     */
    private String gztime;
}
