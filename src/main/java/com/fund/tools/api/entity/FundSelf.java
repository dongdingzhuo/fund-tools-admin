package com.fund.tools.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自选基金实体类
 */
@Data
@TableName("t_fund_self")
public class FundSelf implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 成本价
     */
    private BigDecimal baseAmount;

    /**
     * 持有份额
     */
    private BigDecimal shareQuantity;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
