package com.fund.tools.api.task;

import cn.hutool.http.HttpUtil;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.BatchConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.service.FundHistoryService;
import com.fund.tools.api.service.FundLastService;
import com.fund.tools.api.service.HolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基金历史净值定时任务
 */
@Slf4j
@Component
public class FundHistoryTask {

    @Resource
    private FundLastService fundLastService;

    @Resource
    private FundHistoryService fundHistoryService;

    @Resource
    private HolidayService holidayService;

    private static final String API_URL = ApiConstants.EASTMONEY_F10_API;
    private static final int MAX_UPDATE_COUNT = BatchConstants.MAX_UPDATE_COUNT; // 每次最多更新5条数据

    /**
     * 周一到周五 18:00-24:00 每10分钟执行一次
     * Cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/10 18-23 * * MON-FRI")
    public void updateFundHistoryData() {
        executeUpdate();
    }

    /**
     * 测试用：每分钟执行一次（用于调试）
     * 如需启用，请取消注释下面的 @Scheduled 注解
     */
     @Scheduled(cron = "0 17 * * * ?")
    public void testUpdateFundHistoryData() {
        log.info("【测试模式】开始执行基金历史净值更新任务");
        executeUpdate();
    }

    /**
     * 执行更新逻辑
     */
    private void executeUpdate() {
        log.info("开始执行基金历史净值更新任务");

        try {
            // 判断今天是否为交易日
            String today = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            if (!holidayService.isTradingDay(today)) {
                log.info("今天({})不是交易日，跳过执行", today);
                return;
            }

            log.info("当前是交易日，开始更新基金历史净值数据");

            // 获取所有实时基金数据
            List<FundLast> fundLastList = fundLastService.getAllFundLast();

            if (fundLastList == null || fundLastList.isEmpty()) {
                log.info("t_fund_last表中没有基金数据，跳过更新");
                return;
            }

            // 限制每次最多更新5条数据，防止内存溢出
            int totalSize = fundLastList.size();
            int updateSize = Math.min(totalSize, MAX_UPDATE_COUNT);
            List<FundLast> toUpdateList = fundLastList.subList(0, updateSize);

            log.info("共有{}只基金，本次更新{}只", totalSize, updateSize);

            // 计算日期范围：最近2个交易日
            String formattedEndDate = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            String formattedStartDate = getPreviousTradingDay(formattedEndDate);

            log.info("查询日期范围: {} 到 {}", formattedStartDate, formattedEndDate);

            int successCount = 0;
            int failCount = 0;
            int skipCount = 0;

            for (FundLast fundLast : toUpdateList) {
                try {
                    // 判断该基金今日数据是否已获取
                    if (isHistoryDataExists(fundLast.getFundCode(), formattedEndDate)) {
                        skipCount++;
                        log.debug("基金{}的{}数据已存在，跳过", fundLast.getFundCode(), formattedEndDate);
                        continue;
                    }

                    // 获取基金历史净值
                    boolean success = fetchAndUpdateHistoryData(
                            fundLast.getFundCode(),
                            fundLast.getFundName(),
                            formattedStartDate,
                            formattedEndDate
                    );

                    if (success) {
                        successCount++;
                        log.info("成功获取基金{}的历史净值数据", fundLast.getFundCode());
                    } else {
                        failCount++;
                        log.warn("获取基金{}的历史净值数据失败", fundLast.getFundCode());
                    }

                    // 每次请求后等待1秒，避免频繁调用
                    Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                } catch (Exception e) {
                    failCount++;
                    log.error("处理基金{}数据时发生异常", fundLast.getFundCode(), e);
                }
            }

            log.info("基金历史净值更新任务执行完成 - 成功: {}, 失败: {}, 跳过: {}",
                    successCount, failCount, skipCount);
        } catch (Exception e) {
            log.error("基金历史净值更新任务执行失败", e);
        }
    }

    /**
     * 判断基金历史数据是否已存在
     */
    private boolean isHistoryDataExists(String fundCode, String date) {
        try {
            return fundHistoryService.existsByFundCodeAndDate(fundCode, date);
        } catch (Exception e) {
            log.error("检查基金{}的{}数据是否存在时发生异常", fundCode, date, e);
            return false;
        }
    }

    /**
     * 获取指定日期的前一个交易日
     */
    private String getPreviousTradingDay(String currentDate) {
        try {
            LocalDate date = LocalDate.parse(currentDate, DateFormatConstants.DATE_FORMATTER);
            // 向前查找，最多找7天（防止长假）
            for (int i = 1; i <= 7; i++) {
                LocalDate previousDate = date.minusDays(i);
                String dateStr = previousDate.format(DateFormatConstants.DATE_FORMATTER);

                if (holidayService.isTradingDay(dateStr)) {
                    log.debug("找到{}的前一个交易日: {}", currentDate, dateStr);
                    return dateStr;
                }
            }

            // 如果没找到，默认返回前一天的日历日期
            log.warn("未找到{}的前一个交易日，使用日历日期", currentDate);
            return date.minusDays(1).format(DateFormatConstants.DATE_FORMATTER);
        } catch (Exception e) {
            log.error("获取{}的前一个交易日失败", currentDate, e);
            return LocalDate.parse(currentDate, DateFormatConstants.DATE_FORMATTER).minusDays(1).format(DateFormatConstants.DATE_FORMATTER);
        }
    }

    /**
     * 从API获取基金历史净值并更新
     */
    private boolean fetchAndUpdateHistoryData(String fundCode, String fundName,
                                               String startDate, String endDate) {
        try {
            String url = String.format("%s?type=lsjz&code=%s&page=1&per=30&sdate=%s&edate=%s",
                    API_URL, fundCode, startDate, endDate);

            log.info("请求基金历史净值URL: {}", url);

            String response = HttpUtil.get(url, ApiConstants.API_TIMEOUT_MS); // 设置60秒超时

            // 打印完整的API响应
            log.info("基金{}的API完整响应: {}", fundCode, response);

            if (response == null || response.trim().isEmpty()) {
                log.warn("基金{}的API返回空响应", fundCode);
                return false;
            }

            // 解析返回数据
            List<FundHistory> historyList = parseApiResponse(response, fundCode, fundName);

            if (historyList == null || historyList.isEmpty()) {
                log.warn("基金{}的API返回数据解析失败", fundCode);
                return false;
            }

            log.info("基金{}解析到{}条历史净值数据", fundCode, historyList.size());

            // 批量保存历史净值数据
            for (FundHistory history : historyList) {
                fundHistoryService.saveOrUpdateHistory(history);
            }

            // 如果有当日数据，更新实时基金表
            updateRealTimeDataFromHistory(fundCode, historyList, endDate);

            return true;
        } catch (Exception e) {
            log.error("从API获取基金{}的历史净值数据失败", fundCode, e);
            return false;
        }
    }

    /**
     * 解析API响应数据（HTML表格格式）
     */
    private List<FundHistory> parseApiResponse(String response, String fundCode, String fundName) {
        try {
            // 提取content内容（使用 DOTALL 模式匹配跨行内容）
            Pattern pattern = Pattern.compile("content:\"(.*?)\",records", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);

            if (!matcher.find()) {
                log.warn("基金{}的API响应中未找到content字段", fundCode);
                return null;
            }

            String content = matcher.group(1);
            log.info("基金{}提取的content内容: {}", fundCode, content);

            List<FundHistory> historyList = new ArrayList<>();

            // 解析HTML表格中的每一行数据
            // 匹配 <tr><td>日期</td><td>净值</td><td>累计净值</td><td>增长率</td>...
            Pattern rowPattern = Pattern.compile(
                "<tr>\\s*" +
                "<td>([^<]+)</td>\\s*" +  // 净值日期
                "<td[^>]*>([^<]+)</td>\\s*" +  // 单位净值
                "<td[^>]*>([^<]+)</td>\\s*" +  // 累计净值
                "<td[^>]*>([^<]+)</td>"  // 日增长率
            );

            Matcher rowMatcher = rowPattern.matcher(content);

            while (rowMatcher.find()) {
                String date = rowMatcher.group(1).trim();
                String netValue = rowMatcher.group(2).trim();
                String accumulatedValue = rowMatcher.group(3).trim();
                String growthRate = rowMatcher.group(4).trim();

                log.debug("基金{}解析到一行数据: 日期={}, 净值={}, 累计净值={}, 增长率={}",
                    fundCode, date, netValue, accumulatedValue, growthRate);

                FundHistory history = new FundHistory();
                history.setFundCode(fundCode);
                history.setFundName(fundName);
                history.setDate(date);

                // 解析单位净值
                try {
                    history.setHistoryPrice(new BigDecimal(netValue));
                } catch (NumberFormatException e) {
                    log.warn("基金{}的日期{}净值解析失败: {}", fundCode, date, netValue);
                    continue;
                }

                // 解析日增长率（收益率）
                try {
                    if (growthRate != null && !growthRate.isEmpty() && !growthRate.equals("--")) {
                        // 去除百分号
                        String rateStr = growthRate.replace("%", "");
                        history.setProfitPercent(new BigDecimal(rateStr));
                    }
                } catch (NumberFormatException e) {
                    log.info("基金{}的日期{}日增长率解析失败: {}", fundCode, date, growthRate);
                }

                historyList.add(history);
            }

            if (historyList.isEmpty()) {
                log.warn("基金{}解析后没有有效数据", fundCode);
                return null;
            }

            log.info("基金{}成功解析到{}条历史净值数据", fundCode, historyList.size());
            return historyList;
        } catch (Exception e) {
            log.error("解析基金{}的API响应失败", fundCode, e);
            return null;
        }
    }

    /**
     * 从历史数据中更新实时基金表
     */
    private void updateRealTimeDataFromHistory(String fundCode, List<FundHistory> historyList, String today) {
        try {
            // 查找今日的数据
            FundHistory todayHistory = historyList.stream()
                    .filter(h -> today.equals(h.getDate()))
                    .findFirst()
                    .orElse(null);

            if (todayHistory == null) {
                log.debug("基金{}没有找到今日({})的历史数据", fundCode, today);
                return;
            }

            // 查找上一个交易日的数据（排除今天，取最新的一条）
            FundHistory prevHistory = historyList.stream()
                    .filter(h -> !today.equals(h.getDate()))
                    .findFirst()
                    .orElse(null);

            // 获取现有的实时数据
            FundLast existingFund = fundLastService.getFundLastByCode(fundCode);

            if (existingFund != null) {
                // 更新currentPrice为今天的净值
                existingFund.setCurrentPrice(todayHistory.getHistoryPrice());

                // 更新prevPrice为上一个交易日的净值
                if (prevHistory != null && prevHistory.getHistoryPrice() != null) {
                    existingFund.setPrevPrice(prevHistory.getHistoryPrice());
                    log.debug("基金{}的prevPrice设置为上一个交易日({})的净值: {}",
                            fundCode, prevHistory.getDate(), prevHistory.getHistoryPrice());
                } else if (existingFund.getCurrentPrice() != null) {
                    // 如果没有找到上一个交易日数据，保持原有的prevPrice
                    log.debug("基金{}未找到上一个交易日数据，保持原有prevPrice", fundCode);
                }

                // 直接使用API返回的日增长率作为收益率
                if (todayHistory.getProfitPercent() != null) {
                    existingFund.setProfitPercent(todayHistory.getProfitPercent());
                    log.debug("基金{}使用API返回的收益率: {}%", fundCode, todayHistory.getProfitPercent());
                }

                // 设置数据时间为历史数据的日期（转换为当天的23:59:59）
                try {
                    LocalDateTime dataTime = LocalDate.parse(todayHistory.getDate(), DateFormatConstants.DATE_FORMATTER)
                            .atTime(23, 59, 59);
                    existingFund.setDataTime(dataTime);
                    log.debug("基金{}的数据时间设置为历史数据日期: {}", fundCode, dataTime);
                } catch (Exception e) {
                    log.warn("基金{}的历史数据日期解析失败: {}", fundCode, todayHistory.getDate(), e);
                }

                fundLastService.saveOrUpdateFundLast(existingFund);
                log.info("成功更新基金{}的实时数据: currentPrice={}, prevPrice={}, profitPercent={}, dataTime={}",
                        fundCode, todayHistory.getHistoryPrice(),
                        existingFund.getPrevPrice(), existingFund.getProfitPercent(), existingFund.getDataTime());
            }
        } catch (Exception e) {
            log.error("更新基金{}的实时数据失败", fundCode, e);
        }
    }
}
