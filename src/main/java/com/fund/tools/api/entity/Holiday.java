package com.fund.tools.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 节假日日期实体类
 */
@Data
@TableName("t_holiday")
public class Holiday implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期（yyyy-MM-dd格式）
     */
    private String date;

    /**
     * 工作日标识（Y/N）
     */
    private String workdayFlag;

    /**
     * 节假日标识（Y/N）
     */
    private String holidayFlag;

    /**
     * 周末标识（Y/N）
     */
    private String weekendFlag;

    /**
     * 交易日标识（Y/N）
     */
    private String tradingDayFlag;

    /**
     * 星期（0-周日，1-周一，2-周二，3-周三，4-周四，5-周五，6-周六）
     */
    private Integer week;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
