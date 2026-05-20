package com.fund.tools.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 东方财富板块资金流向API响应DTO
 */
@Data
public class EastmoneyPlateFlowResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 返回码
     */
    private Integer rc;

    /**
     * 数据对象
     */
    private PlateFlowData data;

    @Data
    public static class PlateFlowData implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 总数
         */
        private Integer total;

        /**
         * 板块列表
         */
        private List<PlateFlowItem> diff;
    }

    @Data
    public static class PlateFlowItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 板块名称 (f14)
         */
        @JsonProperty("f14")
        private String plateName;

        /**
         * 主力净流入（元）(f62)
         */
        @JsonProperty("f62")
        private Double mainFlow;

        /**
         * 主力成交额/总成交（元）(f63)
         */
        @JsonProperty("f63")
        private Double mainDealAmount;

        /**
         * 主力流入金额（元）(f64)
         */
        @JsonProperty("f64")
        private Double mainFlowAmount;
    }
}
