package com.fund.tools.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时基金数据实体类
 */
@Data
@TableName("t_fund_last")
public class FundLast implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 最新收益率
     */
    private BigDecimal profitPercent;

    /**
     * 净值
     */
    private BigDecimal currentPrice;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
