package com.fund.tools.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.mapper.FundHistoryMapper;
import com.fund.tools.api.util.FundApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 异步基金任务服务
 * 专门处理基金相关的异步任务，避免循环依赖问题
 */
@Slf4j
@Service
public class AsyncFundTaskService {

    @Resource
    private FundLastService fundLastService;

    @Resource
    private FundHistoryMapper fundHistoryMapper;

    /**
     * 异步同步到实时基金表
     */
    @Async
    public void asyncSyncToFundLast(String fundCode) {
        try {
            // 检查实时基金表中是否已存在该基金
            FundLast existingFund = fundLastService.getFundLastByCode(fundCode);
            if (existingFund != null) {
                log.info("基金{}已存在于实时数据表中，跳过添加", fundCode);
                return;
            }

            log.info("开始异步获取基金{}的实时数据", fundCode);
            
            // 获取实时基金数据
            FundLast fundLast = FundApiUtil.fetchFundRealTimeData(fundCode);
            if (fundLast != null) {
                fundLastService.saveOrUpdateFundLast(fundLast);
                log.info("成功异步同步基金{}到实时数据表", fundCode);
            } else {
                log.warn("异步获取基金{}的实时数据失败", fundCode);
            }
        } catch (Exception e) {
            log.error("异步同步基金{}到实时数据表失败", fundCode, e);
        }
    }

    /**
     * 异步获取历史基金数据（从数据库最后一天到今天）
     */
    @Async
    public void asyncFetchHistoryData(String fundCode) {
        try {
            log.info("开始异步获取基金{}的历史数据", fundCode);
            
            // 先从实时数据表获取基金名称
            String fundName = fundCode;
            FundLast fundLast = fundLastService.getFundLastByCode(fundCode);
            if (fundLast != null && fundLast.getFundName() != null) {
                fundName = fundLast.getFundName();
            }
            
            // 调用公共方法获取历史数据
            fetchHistoryDataBetweenDates(fundCode, fundName);
            
        } catch (Exception e) {
            log.error("异步获取基金{}历史数据失败", fundCode, e);
        }
    }

    /**
     * 公共方法：获取指定基金从数据库最后一天到当前日期的历史数据
     * @param fundCode 基金代码
     * @param fundName 基金名称
     */
    public void fetchHistoryDataBetweenDates(String fundCode, String fundName) {
        try {
            // 查询数据库中该基金的最后一条记录日期
            LambdaQueryWrapper<FundHistory> lastWrapper = new LambdaQueryWrapper<>();
            lastWrapper.eq(FundHistory::getFundCode, fundCode)
                      .orderByDesc(FundHistory::getDate)
                      .last("LIMIT 1");
            FundHistory lastHistory = fundHistoryMapper.selectOne(lastWrapper);
            
            String startDate;
            if (lastHistory != null) {
                // 从最后一天的下一天开始
                LocalDate lastDate = LocalDate.parse(lastHistory.getDate(), DateFormatConstants.DATE_FORMATTER);
                startDate = lastDate.plusDays(1).format(DateFormatConstants.DATE_FORMATTER);
                log.info("基金{}数据库中最后日期为{}，将从{}开始获取", fundCode, lastHistory.getDate(), startDate);
            } else {
                // 如果没有历史记录，从最近3个月开始
                startDate = LocalDate.now().minusMonths(3).format(DateFormatConstants.DATE_FORMATTER);
                log.info("基金{}没有历史记录，将从{}开始获取", fundCode, startDate);
            }
            
            String endDate = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            
            // 如果起始日期大于结束日期，说明数据已经是最新的
            if (LocalDate.parse(startDate, DateFormatConstants.DATE_FORMATTER).isAfter(LocalDate.parse(endDate, DateFormatConstants.DATE_FORMATTER))) {
                log.info("基金{}的数据已是最新，无需获取", fundCode);
                return;
            }
            
            log.info("开始获取基金{}历史数据，范围: {} 到 {}", fundCode, startDate, endDate);
            
            // 按日期顺序获取数据
            LocalDate currentDate = LocalDate.parse(startDate, DateFormatConstants.DATE_FORMATTER);
            LocalDate endLocalDate = LocalDate.parse(endDate, DateFormatConstants.DATE_FORMATTER);
            
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            
            while (!currentDate.isAfter(endLocalDate)) {
                String dateStr = currentDate.format(DateFormatConstants.DATE_FORMATTER);
                
                try {
                    // 检查该日期的数据是否已存在
                    LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FundHistory::getFundCode, fundCode)
                           .eq(FundHistory::getDate, dateStr);
                    Long dateCount = fundHistoryMapper.selectCount(wrapper);
                    
                    if (dateCount > 0) {
                        log.debug("基金{}日期{}的数据已存在，跳过", fundCode, dateStr);
                        skipCount++;
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
                    
                    // 调用API获取单日数据（使用工具类）
                    FundHistory history = FundApiUtil.fetchSingleDayHistory(fundCode, fundName, dateStr);
                    
                    if (history != null) {
                        fundHistoryMapper.insert(history);
                        successCount++;
                        log.debug("成功获取基金{}日期{}的历史数据", fundCode, dateStr);
                    } else {
                        skipCount++;  // 节假日或无数据计入跳过数
                        log.debug("基金{}日期{}没有数据（可能是节假日）", fundCode, dateStr);
                    }
                    
                    // 每次请求后等待1秒，避免频繁调用
                    Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                    
                } catch (Exception e) {
                    log.error("获取基金{}日期{}的历史数据失败", fundCode, dateStr, e);
                    failCount++;
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            log.info("基金{}历史数据获取完成 - 成功: {}, 跳过: {}, 失败: {}", fundCode, successCount, skipCount, failCount);
            
        } catch (Exception e) {
            log.error("获取基金{}历史数据失败", fundCode, e);
        }
    }
}
