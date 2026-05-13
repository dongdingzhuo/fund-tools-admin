package com.fund.tools.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 节假日日期请求
 */
@Data
public class HolidayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期（yyyy-MM-dd格式）
     */
    @NotBlank(message = "日期不能为空")
    private String date;

    /**
     * 工作日标识（Y/N）
     */
    @NotBlank(message = "工作日标识不能为空")
    private String workdayFlag;

    /**
     * 节假日标识（Y/N）
     */
    @NotBlank(message = "节假日标识不能为空")
    private String holidayFlag;

    /**
     * 周末标识（Y/N）
     */
    @NotBlank(message = "周末标识不能为空")
    private String weekendFlag;

    /**
     * 交易日标识（Y/N）
     */
    @NotBlank(message = "交易日标识不能为空")
    private String tradingDayFlag;

    /**
     * 星期（0-周日，1-周一，2-周二，3-周三，4-周四，5-周五，6-周六）
     */
    @NotNull(message = "星期不能为空")
    private Integer week;
}
