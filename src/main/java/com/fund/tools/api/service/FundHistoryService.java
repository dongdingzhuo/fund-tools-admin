package com.fund.tools.api.service;

import com.fund.tools.api.dto.FundHistoryResponse;
import com.fund.tools.api.entity.FundHistory;

import java.util.List;

/**
 * 基金历史净值服务接口
 */
public interface FundHistoryService {

    /**
     * 根据基金代码和日期范围查询历史净值
     *
     * @param fundCode  基金代码
     * @param startDate 开始日期（yyyy-MM-dd格式）
     * @param endDate   结束日期（yyyy-MM-dd格式）
     * @return 历史净值列表
     */
    List<FundHistoryResponse> getHistoryByFundCodeAndDateRange(String fundCode, String startDate, String endDate);

    /**
     * 根据基金代码和日期范围查询历史净值（实体）
     *
     * @param fundCode  基金代码
     * @param startDate 开始日期（yyyy-MM-dd格式）
     * @param endDate   结束日期（yyyy-MM-dd格式）
     * @return 历史净值列表
     */
    List<FundHistory> getHistoryEntitiesByFundCodeAndDateRange(String fundCode, String startDate, String endDate);

    /**
     * 判断指定日期的基金历史数据是否存在
     *
     * @param fundCode 基金代码
     * @param date     日期（yyyy-MM-dd格式）
     * @return 是否存在
     */
    boolean existsByFundCodeAndDate(String fundCode, String date);

    /**
     * 保存或更新历史净值数据
     *
     * @param history 历史净值数据
     * @return 是否成功
     */
    boolean saveOrUpdateHistory(FundHistory history);
}
