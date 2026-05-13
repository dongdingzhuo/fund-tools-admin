package com.fund.tools.api.controller;

import com.fund.tools.api.common.Result;
import com.fund.tools.api.dto.FundHistoryResponse;
import com.fund.tools.api.service.FundHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 基金历史净值控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/fund-history")
public class FundHistoryController {

    @Resource
    private FundHistoryService fundHistoryService;

    /**
     * 根据基金代码和起始日期查询历史净值
     *
     * @param fundCode  基金代码（必填）
     * @param startDate 开始日期（yyyy-MM-dd格式，可选）
     * @param endDate   结束日期（yyyy-MM-dd格式，可选）
     * @return 历史净值列表
     */
    @GetMapping("/list")
    public Result<List<FundHistoryResponse>> getHistoryList(
            @RequestParam String fundCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<FundHistoryResponse> list = fundHistoryService.getHistoryByFundCodeAndDateRange(fundCode, startDate, endDate);
            log.info("查询基金{}的历史净值，起始日期: {}, 结束日期: {}, 共{}条", fundCode, startDate, endDate, list.size());
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询基金历史净值失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
