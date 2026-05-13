package com.fund.tools.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建/更新自选基金请求
 */
@Data
public class FundSelfRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户账号
     */
    @NotBlank(message = "用户账号不能为空")
    private String userName;

    /**
     * 基金代码
     */
    @NotBlank(message = "基金代码不能为空")
    private String fundCode;

    /**
     * 成本价
     */
    @NotNull(message = "成本价不能为空")
    private BigDecimal baseAmount;

    /**
     * 持有份额
     */
    @NotNull(message = "持有份额不能为空")
    private BigDecimal shareQuantity;
}
