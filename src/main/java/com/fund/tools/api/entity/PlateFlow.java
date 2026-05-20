package com.fund.tools.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 板块资金流向实体类
 */
@Data
@TableName("t_plate_flow")
public class PlateFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 板块名称
     */
    private String plateName;

    /**
     * 主力净流入（元）
     * 主力资金 = 超大单 + 大单
     * 正数 = 流入，负数 = 流出
     */
    private BigDecimal mainFlow;

    /**
     * 主力成交额/总成交（元）
     * 板块当天所有成交金额
     */
    private BigDecimal mainDealAmount;

    /**
     * 主力流入金额（元）
     * 主力主动买入的钱
     */
    private BigDecimal mainFlowAmount;

    /**
     * 主力净流入占比（%）
     * 主力净流入 / 板块总成交额
     * 越高代表资金越集中
     */
    private BigDecimal mainFlowRate;

    /**
     * 数据时间
     */
    private LocalDateTime dataTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
