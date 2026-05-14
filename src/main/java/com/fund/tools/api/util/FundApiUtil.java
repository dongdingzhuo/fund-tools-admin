package com.fund.tools.api.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.dto.TiantianFundResponse;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.entity.FundLast;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基金数据API调用工具类
 */
@Slf4j
public class FundApiUtil {

    /**
     * 从F10 API获取单日基金历史数据（带重试机制）
     *
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @param date     日期（yyyy-MM-dd格式）
     * @return 基金历史数据，如果获取失败返回null
     */
    public static FundHistory fetchSingleDayHistory(String fundCode, String fundName, String date) {
        int maxRetries = 3; // 最大重试次数
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String url = String.format("%s?type=lsjz&code=%s&page=1&per=1&sdate=%s&edate=%s",
                        ApiConstants.EASTMONEY_F10_API, fundCode, date, date);

                log.debug("请求基金历史净值URL: {}", url);

                // 使用 HttpUtil.createGet 并添加请求头，模拟浏览器请求
                String response = HttpUtil.createGet(url)
                        .timeout(ApiConstants.API_TIMEOUT_MS) // 60秒超时
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Connection", "keep-alive")
                        .execute()
                        .body();

                if (response == null || response.trim().isEmpty()) {
                    log.warn("基金{}日期{}的API返回空响应", fundCode, date);
                    return null;
                }

                // 解析API响应
                List<FundHistory> historyList = parseFundHistoryResponse(response, fundCode, fundName);

                if (historyList == null || historyList.isEmpty()) {
                    log.debug("基金{}日期{}的API返回数据为空或解析失败", fundCode, date);
                    return null;
                }

                // 返回第一条数据（应该只有当天的数据）
                return historyList.get(0);
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    log.warn("从API获取基金{}日期{}的历史数据失败，第{}次重试: {}",
                            fundCode, date, retryCount, e.getMessage());
                    try {
                        // 重试前等待2秒
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", ie);
                        return null;
                    }
                } else {
                    log.error("从API获取基金{}日期{}的历史数据失败，已重试{}次", fundCode, date, maxRetries, e);
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * 解析基金历史API响应
     *
     * @param response API响应字符串
     * @param fundCode 基金代码
     * @param fundName 基金名称
     * @return 基金历史数据列表
     */
    public static List<FundHistory> parseFundHistoryResponse(String response, String fundCode, String fundName) {
        try {
            // 提取content内容
            Pattern pattern = Pattern.compile("content:\"(.*?)\",records", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);

            if (!matcher.find()) {
                log.debug("基金{}的API响应中未找到content字段", fundCode);
                return null;
            }

            String content = matcher.group(1);

            List<FundHistory> historyList = new ArrayList<>();

            // 解析HTML表格行
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
                String growthRate = rowMatcher.group(4).trim();

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

                // 解析日增长率
                try {
                    if (growthRate != null && !growthRate.isEmpty() && !growthRate.equals("--")) {
                        String rateStr = growthRate.replace("%", "");
                        history.setProfitPercent(new BigDecimal(rateStr));
                    }
                } catch (NumberFormatException e) {
                    log.debug("基金{}的日期{}日增长率解析失败: {}", fundCode, date, growthRate);
                }

                historyList.add(history);
            }

            return historyList;
        } catch (Exception e) {
            log.error("解析基金{}的API响应失败", fundCode, e);
            return null;
        }
    }

    /**
     * 从天天基金API获取实时基金数据
     *
     * @param fundCode 基金代码
     * @return 基金实时数据，如果获取失败返回null
     */
    public static FundLast fetchFundRealTimeData(String fundCode) {
        try {
            String url = ApiConstants.TIAN_TIAN_FUND_API + fundCode + ".js";
            log.debug("请求基金数据: {}", url);

            String response = HttpUtil.get(url, ApiConstants.QUICK_API_TIMEOUT_MS); // 设置5秒超时

            // 解析返回数据，格式为：jsonpgz({...});
            if (response == null || response.trim().isEmpty()) {
                log.warn("基金{}的API返回空响应", fundCode);
                return null;
            }

            // 提取JSON部分
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("基金{}的API返回数据格式错误", fundCode);
                return null;
            }

            TiantianFundResponse apiResponse = JSONUtil.toBean(jsonStr, TiantianFundResponse.class);

            if (apiResponse == null || apiResponse.getFundcode() == null) {
                log.warn("基金{}的API数据解析失败", fundCode);
                return null;
            }

            // 构建FundLast对象
            FundLast fundLast = new FundLast();
            fundLast.setFundCode(apiResponse.getFundcode());
            fundLast.setFundName(apiResponse.getName());

            // 设置估算值作为当前价格
            BigDecimal currentPrice = null;
            if (apiResponse.getGsz() != null && !apiResponse.getGsz().isEmpty()) {
                currentPrice = new BigDecimal(apiResponse.getGsz());
                fundLast.setCurrentPrice(currentPrice);
            }

            // 设置单位净值(dwjz)作为上一个交易日净值
            if (apiResponse.getDwjz() != null && !apiResponse.getDwjz().isEmpty()) {
                fundLast.setPrevPrice(new BigDecimal(apiResponse.getDwjz()));
                log.debug("基金{}的prevPrice设置为API返回的dwjz: {}", fundCode, apiResponse.getDwjz());
            } else if (currentPrice != null) {
                // 如果dwjz为空，使用currentPrice作为prevPrice
                fundLast.setPrevPrice(currentPrice);
            }

            // 设置估算涨跌幅作为收益率
            if (apiResponse.getGszzl() != null && !apiResponse.getGszzl().isEmpty()) {
                fundLast.setProfitPercent(new BigDecimal(apiResponse.getGszzl()));
            }

            // 设置数据时间为API返回的估算时间（gztime）
            if (apiResponse.getGztime() != null && !apiResponse.getGztime().isEmpty()) {
                try {
                    // gztime格式为 "yyyy-MM-dd HH:mm"，需要转换为LocalDateTime
                    LocalDateTime dataTime = LocalDateTime.parse(apiResponse.getGztime(), DateFormatConstants.DATETIME_MINUTE_FORMATTER);
                    fundLast.setDataTime(dataTime);
                    log.debug("基金{}的数据时间设置为: {}", fundCode, dataTime);
                } catch (Exception e) {
                    log.warn("基金{}的gztime解析失败: {}", fundCode, apiResponse.getGztime(), e);
                }
            }

            log.info("成功获取基金{}的实时数据: name={}, price={}, prevPrice={}, profit={}",
                    fundCode, apiResponse.getName(), apiResponse.getGsz(),
                    fundLast.getPrevPrice(), apiResponse.getGszzl());

            return fundLast;
        } catch (Exception e) {
            log.error("从API获取基金{}的数据失败", fundCode, e);
            return null;
        }
    }

    /**
     * 从返回字符串中提取JSON部分
     * 输入：jsonpgz({"fundcode":"025209",...});
     * 输出：{"fundcode":"025209",...}
     *
     * @param response API响应字符串
     * @return JSON字符串，如果提取失败返回null
     */
    private static String extractJson(String response) {
        try {
            // 找到第一个 { 和最后一个 }
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');

            if (start != -1 && end != -1 && end > start) {
                return response.substring(start, end + 1);
            }

            return null;
        } catch (Exception e) {
            log.error("提取JSON失败", e);
            return null;
        }
    }
}
