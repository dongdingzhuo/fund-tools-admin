package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 板块资金流向响应DTO
 */
@Data
public class PlateFlowResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 板块名称
     */
    private String plateName;

    /**
     * 主力净流入（元）
     */
    private BigDecimal mainFlow;

    /**
     * 主力成交额/总成交（元）
     */
    private BigDecimal mainDealAmount;

    /**
     * 主力流入金额（元）
     */
    private BigDecimal mainFlowAmount;

    /**
     * 主力净流入占比（%）
     */
    private BigDecimal mainFlowRate;

    /**
     * 数据时间
     */
    private LocalDateTime dataTime;
}
