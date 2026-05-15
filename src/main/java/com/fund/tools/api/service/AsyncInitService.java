package com.fund.tools.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.entity.Holiday;
import com.fund.tools.api.mapper.FundHistoryMapper;
import com.fund.tools.api.mapper.FundLastMapper;
import com.fund.tools.api.mapper.FundSelfMapper;
import com.fund.tools.api.mapper.HolidayMapper;
import com.fund.tools.api.util.FundApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步初始化任务服务
 * 专门处理需要异步执行的初始化任务，避免循环依赖问题
 */
@Slf4j
@Service
public class AsyncInitService {

    @Resource
    private HolidayService holidayService;

    @Resource
    private HolidayMapper holidayMapper;

    @Resource
    private FundSelfMapper fundSelfMapper;

    @Resource
    private FundLastService fundLastService;

    @Resource
    private FundHistoryMapper fundHistoryMapper;

    @Resource
    private FundLastMapper fundLastMapper;

    /**
     * 异步同步节假日数据
     */
    @Async
    public void asyncSyncHolidayData() {
        try {
            log.info("[异步任务1] 开始执行节假日数据同步任务");

            String today = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);

            // 调用 isTradingDay 方法，它会自动检查并获取缺失的数据
            boolean isTradingDay = holidayService.isTradingDay(today);
            log.info("[异步任务1] 日期{}的交易日状态: {}", today, isTradingDay);

            // 验证数据库中最后一条数据是否为当天
            LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(Holiday::getId).last("LIMIT 1");
            Holiday lastHoliday = holidayMapper.selectOne(wrapper);

            if (lastHoliday != null && today.equals(lastHoliday.getDate())) {
                log.info("[异步任务1] 数据库中最后一条数据为当天日期{}，节假日数据已最新", today);
            } else {
                log.info("[异步任务1] 节假日数据同步完成，数据库最后日期: {}", 
                        lastHoliday != null ? lastHoliday.getDate() : "无数据");
            }
            
            log.info("[异步任务1] 节假日数据同步任务完成");
        } catch (Exception e) {
            log.error("[异步任务1] 节假日数据同步失败", e);
        }
    }

    /**
     * 异步删除旧数据
     */
    @Async
    public void asyncDeleteOldData() {
        try {
            log.info("[异步任务2] 开始执行数据清理任务");
            
            // 删除3个月之前的历史数据
            deleteOldHistoryData();
            
            // 删除非自选基金的历史数据
            deleteNonSelfFundHistory();
            
            // 删除非自选基金的实时数据
            deleteNonSelfFundLastData();
            
            log.info("[异步任务2] 数据清理任务完成");
        } catch (Exception e) {
            log.error("[异步任务2] 数据清理失败", e);
        }
    }

    /**
     * 异步初始化历史基金数据
     */
    @Async
    public void asyncInitFundHistoryData() {
        try {
            log.info("[异步任务3] 开始执行历史基金数据初始化任务");

            // 获取所有唯一的基金代码
            List<String> fundCodeList = fundSelfMapper.selectDistinctFundCodes();
            if (fundCodeList == null || fundCodeList.isEmpty()) {
                log.info("[异步任务3] 没有自选基金，跳过历史数据初始化");
                return;
            }

            log.info("[异步任务3] 共有{}只基金需要初始化历史数据", fundCodeList.size());

            int totalFail = 0;

            for (String fundCode : fundCodeList) {
                try {
                    // 获取基金名称
                    FundLast fundLast = fundLastService.getFundLastByCode(fundCode);
                    String fundName = fundLast != null ? fundLast.getFundName() : fundCode;

                    log.info("[异步任务3] 开始处理基金{}的历史数据", fundCode);

                    // 调用公共方法获取历史数据（从数据库最后一天到今天）
                    fetchHistoryDataForFund(fundCode, fundName);
                    
                    // 每处理完一只基金，等待1秒
                    Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                } catch (Exception e) {
                    log.error("[异步任务3] 处理基金{}的历史数据失败", fundCode, e);
                    totalFail++;
                }
            }

            log.info("[异步任务3] 历史基金数据初始化完成 - 失败: {}", totalFail);
        } catch (Exception e) {
            log.error("[异步任务3] 历史基金数据初始化失败", e);
        }
    }

    /**
     * 为单个基金获取历史数据
     */
    private void fetchHistoryDataForFund(String fundCode, String fundName) {
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
                        // 设置创建时间
                        history.setCreateTime(LocalDateTime.now());
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

    /**
     * 删除3个月之前的历史数据
     */
    private void deleteOldHistoryData() {
        log.info("开始删除3个月之前的历史数据");
        
        String cutoffDate = LocalDate.now().minusMonths(3).format(DateFormatConstants.DATE_FORMATTER);
        log.info("删除日期早于{}的历史数据", cutoffDate);
        
        try {
            // 查询所有早于截止日期的历史数据
            LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(FundHistory::getDate, cutoffDate);
            
            List<FundHistory> oldDataList = fundHistoryMapper.selectList(wrapper);
            
            if (oldDataList == null || oldDataList.isEmpty()) {
                log.info("没有3个月之前的历史数据需要删除");
                return;
            }
            
            int deleteCount = oldDataList.size();
            log.info("找到{}条3个月之前的历史数据，开始删除", deleteCount);
            
            // 批量删除
            for (FundHistory history : oldDataList) {
                fundHistoryMapper.deleteById(history.getId());
            }
            
            log.info("成功删除{}条3个月之前的历史数据", deleteCount);
        } catch (Exception e) {
            log.error("删除3个月之前的历史数据失败", e);
        }
    }

    /**
     * 删除非自选基金的历史数据
     */
    private void deleteNonSelfFundHistory() {
        log.info("开始删除非自选基金的历史数据");
        
        try {
            // 获取所有唯一的自选基金代码
            List<String> selfFundCodes = fundSelfMapper.selectDistinctFundCodes();
            
            if (selfFundCodes == null || selfFundCodes.isEmpty()) {
                log.info("没有自选基金，将删除所有历史数据");
                // 如果没有自选基金，删除所有历史数据
                LambdaQueryWrapper<FundHistory> allWrapper = new LambdaQueryWrapper<>();
                List<FundHistory> allHistory = fundHistoryMapper.selectList(allWrapper);
                if (allHistory != null && !allHistory.isEmpty()) {
                    for (FundHistory history : allHistory) {
                        fundHistoryMapper.deleteById(history.getId());
                    }
                    log.info("已删除所有{}条历史数据", allHistory.size());
                }
                return;
            }
            
            log.info("当前有{}只自选基金: {}", selfFundCodes.size(), selfFundCodes);
            
            // 获取所有历史数据中的唯一基金代码
            LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(FundHistory::getFundCode)
                   .groupBy(FundHistory::getFundCode);
            List<FundHistory> distinctFundList = fundHistoryMapper.selectList(wrapper);
            
            if (distinctFundList == null || distinctFundList.isEmpty()) {
                log.info("没有历史数据需要清理");
                return;
            }
            
            int deleteCount = 0;
            
            // 遍历所有有历史数据的基金代码
            for (FundHistory fundHistory : distinctFundList) {
                String fundCode = fundHistory.getFundCode();
                
                // 如果不是自选基金，则删除其所有历史数据
                if (!selfFundCodes.contains(fundCode)) {
                    log.info("基金{}不是自选基金，删除其所有历史数据", fundCode);
                    
                    LambdaQueryWrapper<FundHistory> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(FundHistory::getFundCode, fundCode);
                    List<FundHistory> toDeleteList = fundHistoryMapper.selectList(deleteWrapper);
                    
                    for (FundHistory history : toDeleteList) {
                        fundHistoryMapper.deleteById(history.getId());
                        deleteCount++;
                    }
                    
                    log.info("已删除基金{}的{}条历史数据", fundCode, toDeleteList.size());
                }
            }
            
            log.info("成功删除非自选基金的{}条历史数据", deleteCount);
        } catch (Exception e) {
            log.error("删除非自选基金历史数据失败", e);
        }
    }

    /**
     * 删除非自选基金的实时基金数据
     */
    private void deleteNonSelfFundLastData() {
        log.info("开始删除非自选基金的实时基金数据");
        
        try {
            // 获取所有唯一的自选基金代码
            List<String> selfFundCodes = fundSelfMapper.selectDistinctFundCodes();
            
            if (selfFundCodes == null || selfFundCodes.isEmpty()) {
                log.info("没有自选基金，将删除所有实时基金数据");
                // 如果没有自选基金，删除所有实时数据
                LambdaQueryWrapper<FundLast> allWrapper = new LambdaQueryWrapper<>();
                List<FundLast> allFundLast = fundLastMapper.selectList(allWrapper);
                if (allFundLast != null && !allFundLast.isEmpty()) {
                    for (FundLast fundLast : allFundLast) {
                        fundLastMapper.deleteById(fundLast.getId());
                    }
                    log.info("已删除所有{}条实时基金数据", allFundLast.size());
                }
                return;
            }
            
            log.info("当前有{}只自选基金: {}", selfFundCodes.size(), selfFundCodes);
            
            // 获取所有实时基金数据
            LambdaQueryWrapper<FundLast> wrapper = new LambdaQueryWrapper<>();
            List<FundLast> allFundLastList = fundLastMapper.selectList(wrapper);
            
            if (allFundLastList == null || allFundLastList.isEmpty()) {
                log.info("没有实时基金数据需要清理");
                return;
            }
            
            int deleteCount = 0;
            
            // 遍历所有实时基金数据
            for (FundLast fundLast : allFundLastList) {
                String fundCode = fundLast.getFundCode();
                
                // 如果不是自选基金，则删除其实时数据
                if (!selfFundCodes.contains(fundCode)) {
                    log.info("基金{}不是自选基金，删除其实时数据", fundCode);
                    fundLastMapper.deleteById(fundLast.getId());
                    deleteCount++;
                }
            }
            
            log.info("成功删除非自选基金的{}条实时基金数据", deleteCount);
        } catch (Exception e) {
            log.error("删除非自选基金实时数据失败", e);
        }
    }
}
