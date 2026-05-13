package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自选基金响应
 */
@Data
public class FundSelfResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户账号
     */
    private String userName;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 成本价
     */
    private BigDecimal baseAmount;

    /**
     * 持有份额
     */
    private BigDecimal shareQuantity;

    /**
     * 最新净值
     */
    private BigDecimal currentPrice;

    /**
     * 上一个交易日净值
     */
    private BigDecimal prevPrice;

    /**
     * 最新收益率（%）
     */
    private BigDecimal profitPercent;

    /**
     * 持有收益
     */
    private BigDecimal holdingProfit;

    /**
     * 持有收益率（%）
     */
    private BigDecimal holdingProfitPercent;

    /**
     * 实时数据更新时间
     */
    private LocalDateTime fundUpdateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
