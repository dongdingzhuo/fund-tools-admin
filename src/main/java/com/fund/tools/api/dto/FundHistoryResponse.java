package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 基金历史净值响应
 */
@Data
public class FundHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 日期（yyyy-MM-dd格式）
     */
    private String date;

    /**
     * 净值
     */
    private BigDecimal historyPrice;

    /**
     * 日增长率（%）
     */
    private BigDecimal profitPercent;
}
