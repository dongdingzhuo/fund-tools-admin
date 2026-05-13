package com.fund.tools.api.task;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.fund.tools.api.dto.TiantianFundResponse;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.service.FundLastService;
import com.fund.tools.api.service.HolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 基金实时数据定时任务
 */
@Slf4j
@Component
public class FundRealTimeTask {

    @Resource
    private FundLastService fundLastService;

    @Resource
    private HolidayService holidayService;

    private static final String API_URL = "https://fundgz.1234567.com.cn/js/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_UPDATE_COUNT = 5; // 每次最多更新5条数据

    /**
     * 周一到周五 09:30-15:30 每10分钟执行一次
     * Cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/10 9-15 * * MON-FRI")
    public void updateFundRealTimeData() {
        log.info("开始执行基金实时数据更新任务");

        try {
            // 判断当前是否在交易时间内
            if (!isTradingTime()) {
                log.info("当前不在交易时间内，跳过执行");
                return;
            }

            // 判断今天是否为交易日
            String today = LocalDate.now().format(DATE_FORMATTER);
            if (!holidayService.isTradingDay(today)) {
                log.info("今天({})不是交易日，跳过执行", today);
                return;
            }

            log.info("当前是交易日且在交易时间内，开始更新基金数据");

            // 获取所有基金代码
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

            List<FundLast> updateList = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (FundLast fundLast : toUpdateList) {
                try {
                    // 获取数据库中现有的数据，保留prevPrice
                    FundLast existingFund = fundLastService.getFundLastByCode(fundLast.getFundCode());
                    
                    FundLast updatedFund = fetchAndUpdateFundData(fundLast.getFundCode(), existingFund);
                    if (updatedFund != null) {
                        updateList.add(updatedFund);
                        successCount++;
                        log.info("成功获取基金{}的实时数据", fundLast.getFundCode());
                    } else {
                        failCount++;
                        log.warn("获取基金{}的实时数据失败", fundLast.getFundCode());
                    }

                    // 每次请求后等待1秒，避免频繁调用
                    Thread.sleep(1000);
                } catch (Exception e) {
                    failCount++;
                    log.error("处理基金{}数据时发生异常", fundLast.getFundCode(), e);
                }
            }

            // 批量更新数据
            if (!updateList.isEmpty()) {
                fundLastService.batchSaveOrUpdateFundLast(updateList);
                log.info("批量更新完成，成功: {}, 失败: {}", successCount, failCount);
            }

            log.info("基金实时数据更新任务执行完成");
        } catch (Exception e) {
            log.error("基金实时数据更新任务执行失败", e);
        }
    }

    /**
     * 判断当前是否在交易时间内（09:30-15:30）
     */
    private boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.of(9, 30);
        LocalTime endTime = LocalTime.of(15, 30);

        boolean inTimeRange = !now.isBefore(startTime) && !now.isAfter(endTime);

        if (inTimeRange) {
            log.debug("当前时间{}在交易时间内", now);
        } else {
            log.debug("当前时间{}不在交易时间内", now);
        }

        return inTimeRange;
    }

    /**
     * 从 API获取基金数据并更新
     */
    private FundLast fetchAndUpdateFundData(String fundCode, FundLast existingFund) {
        try {
            String url = API_URL + fundCode + ".js";
            log.debug("请求基金数据: {}", url);
    
            String response = HttpUtil.get(url, 60000); // 设置60秒超时
    
            // 解析返回数据，格式为：jsonpgz({...});
            if (response == null || response.trim().isEmpty()) {
                log.warn("基金{}的 API返回空响应", fundCode);
                return null;
            }
    
            // 提取JSON部分
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("基金{}的 API返回数据格式错误", fundCode);
                return null;
            }
    
            TiantianFundResponse apiResponse = JSONUtil.toBean(jsonStr, TiantianFundResponse.class);
    
            if (apiResponse == null || apiResponse.getFundcode() == null) {
                log.warn("基金{}的 API数据解析失败", fundCode);
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
    
            // 设置估算涨跌幅作为收益率
            if (apiResponse.getGszzl() != null && !apiResponse.getGszzl().isEmpty()) {
                fundLast.setProfitPercent(new BigDecimal(apiResponse.getGszzl()));
            }
    
            // 处理prevPrice：如果存在旧数据，将旧的currentPrice保存为prevPrice
            if (existingFund != null && existingFund.getCurrentPrice() != null) {
                // 如果当前价格和之前不同，说明是新的交易日，将之前的价格保存为prevPrice
                if (currentPrice != null && currentPrice.compareTo(existingFund.getCurrentPrice()) != 0) {
                    fundLast.setPrevPrice(existingFund.getCurrentPrice());
                    log.debug("基金{}更新prevPrice: {}", fundCode, existingFund.getCurrentPrice());
                } else {
                    // 否则保持原有的prevPrice
                    fundLast.setPrevPrice(existingFund.getPrevPrice());
                }
            } else if (currentPrice != null) {
                // 如果是新基金，prevPrice默认等于currentPrice
                fundLast.setPrevPrice(currentPrice);
            }
    
            log.debug("成功获取基金{}的数据: name={}, price={}, prevPrice={}, profit={}",
                    fundCode, apiResponse.getName(), apiResponse.getGsz(), 
                    fundLast.getPrevPrice(), apiResponse.getGszzl());
    
            return fundLast;
        } catch (Exception e) {
            log.error("从 API获取基金{}的数据失败", fundCode, e);
            return null;
        }
    }

    /**
     * 从返回字符串中提取JSON部分
     * 输入：jsonpgz({"fundcode":"025209",...});
     * 输出：{"fundcode":"025209",...}
     */
    private String extractJson(String response) {
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
