package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.BatchConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.dto.FundSelfRequest;
import com.fund.tools.api.dto.FundSelfResponse;
import com.fund.tools.api.dto.TiantianFundResponse;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.entity.FundSelf;
import com.fund.tools.api.enums.TradingPeriodEnum;
import com.fund.tools.api.mapper.FundHistoryMapper;
import com.fund.tools.api.mapper.FundSelfMapper;
import com.fund.tools.api.service.AsyncFundTaskService;
import com.fund.tools.api.service.FundLastService;
import com.fund.tools.api.service.FundSelfService;
import com.fund.tools.api.service.HolidayService;
import com.fund.tools.api.util.FundApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自选基金服务实现类
 */
@Slf4j
@Service
public class FundSelfServiceImpl implements FundSelfService {

    @Resource
    private FundSelfMapper fundSelfMapper;

    @Resource
    private FundLastService fundLastService;

    @Resource
    private HolidayService holidayService;

    @Resource
    private AsyncFundTaskService asyncFundTaskService;  // 注入异步基金任务服务

    @Resource
    private FundHistoryMapper fundHistoryMapper;  // 注入历史数据Mapper

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFundSelf(FundSelfRequest request) {
        // 检查是否已存在
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, request.getUserName())
                .eq(FundSelf::getFundCode, request.getFundCode());
        Long count = fundSelfMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该基金已在自选列表中");
        }

        FundSelf fundSelf = new FundSelf();
        BeanUtil.copyProperties(request, fundSelf);
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        fundSelf.setCreateTime(now);
        fundSelf.setUpdateTime(now);
        boolean insertSuccess = fundSelfMapper.insert(fundSelf) > 0;

        if (insertSuccess) {
            // 调用异步基金任务服务
            asyncFundTaskService.asyncSyncToFundLast(request.getFundCode());
            asyncFundTaskService.asyncFetchHistoryData(request.getFundCode());
        }

        return insertSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFundSelf(Long id, FundSelfRequest request) {
        FundSelf fundSelf = fundSelfMapper.selectById(id);
        if (fundSelf == null) {
            throw new RuntimeException("自选基金不存在");
        }

        // 检查是否与其他记录冲突
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, request.getUserName())
                .eq(FundSelf::getFundCode, request.getFundCode())
                .ne(FundSelf::getId, id);
        Long count = fundSelfMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该基金已在自选列表中");
        }

        BeanUtil.copyProperties(request, fundSelf);
        // 设置更新时间
        fundSelf.setUpdateTime(LocalDateTime.now());
        return fundSelfMapper.updateById(fundSelf) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFundSelf(Long id) {
        return fundSelfMapper.deleteById(id) > 0;
    }

    @Override
    public FundSelfResponse getFundSelfById(Long id) {
        FundSelf fundSelf = fundSelfMapper.selectById(id);
        if (fundSelf == null) {
            throw new RuntimeException("自选基金不存在");
        }
        return convertToResponse(fundSelf);
    }

    @Override
    public List<FundSelfResponse> getFundSelfByUserName(String userName) {
        // 判断当前时间并决定是否需要刷新数据
        refreshFundDataIfNeeded(userName);

        // 拉取数据库数据
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, userName)
                .orderByDesc(FundSelf::getCreateTime);
        List<FundSelf> fundSelfList = fundSelfMapper.selectList(wrapper);
        return fundSelfList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FundSelfResponse> pageFundSelf(String userName, Integer pageNum, Integer pageSize) {
        Page<FundSelf> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isEmpty()) {
            wrapper.eq(FundSelf::getUserName, userName);
        }
        wrapper.orderByDesc(FundSelf::getCreateTime);

        Page<FundSelf> fundSelfPage = fundSelfMapper.selectPage(page, wrapper);

        Page<FundSelfResponse> responsePage = new Page<>();
        BeanUtil.copyProperties(fundSelfPage, responsePage, "records");
        responsePage.setRecords(fundSelfPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));

        return responsePage;
    }

    /**
     * 根据当前时间判断是否需要刷新基金数据
     */
    private void refreshFundDataIfNeeded(String userName) {
        try {
            LocalTime now = LocalTime.now();
            String today = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);

            // 判断今天是否为交易日
            boolean isTradingDay = holidayService.isTradingDay(today);
            if (!isTradingDay) {
                log.debug("今天({})不是交易日，不需要刷新数据", today);
                return;
            }

            // 条件1: 交易日早盘交易时段，从实时接口获取数据
            if (TradingPeriodEnum.MORNING_TRADING.isInRange(now)) {
                log.info("当前时间在{}，检查是否需要从实时接口刷新数据", TradingPeriodEnum.MORNING_TRADING.getDescription());
                refreshFromRealTimeApi(userName);
                return;
            }

            // 条件2: 交易日晚间时段，从历史接口获取数据
            if (TradingPeriodEnum.EVENING_UPDATE.isInRange(now)) {
                log.info("当前时间在{}，检查是否需要从历史接口刷新数据", TradingPeriodEnum.EVENING_UPDATE.getDescription());
                refreshFromHistoryApi(userName);
                return;
            }

            log.debug("当前时间不在需要刷新的时间段内，直接返回数据库数据");
        } catch (Exception e) {
            log.error("判断是否需要刷新基金数据失败", e);
        }
    }

    /**
     * 从实时接口刷新数据
     */
    private void refreshFromRealTimeApi(String userName) {
        try {
            // 获取用户的所有自选基金
            LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FundSelf::getUserName, userName);
            List<FundSelf> fundSelfList = fundSelfMapper.selectList(wrapper);

            if (fundSelfList == null || fundSelfList.isEmpty()) {
                log.info("用户{}没有自选基金，跳过刷新", userName);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int refreshCount = 0;

            for (FundSelf fundSelf : fundSelfList) {
                try {
                    // 获取数据库中现有的基金数据
                    FundLast existingFund = fundLastService.getFundLastByCode(fundSelf.getFundCode());

                    if (existingFund != null && existingFund.getDataTime() != null) {
                        // 判断数据时间是否超过30分钟
                        long minutesDiff = Duration.between(existingFund.getDataTime(), now).toMinutes();

                        if (minutesDiff > BatchConstants.REALTIME_REFRESH_THRESHOLD_MINUTES) {
                            log.info("基金{}的数据时间为{}，已超过{}分钟，开始从实时接口刷新",
                                    fundSelf.getFundCode(), existingFund.getDataTime(),
                                    BatchConstants.REALTIME_REFRESH_THRESHOLD_MINUTES);

                            // 从实时接口获取最新数据
                            FundLast updatedFund = fetchFundRealTimeData(fundSelf.getFundCode());
                            if (updatedFund != null) {
                                // 保留原有的prevPrice
                                if (existingFund.getCurrentPrice() != null) {
                                    updatedFund.setPrevPrice(existingFund.getCurrentPrice());
                                }
                                fundLastService.saveOrUpdateFundLast(updatedFund);
                                refreshCount++;
                                log.info("成功刷新基金{}的实时数据", fundSelf.getFundCode());
                            }

                            // 每次请求后等待1秒，避免频繁调用
                            Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                        }
                    } else if (existingFund == null) {
                        // 如果数据库中不存在该基金数据，直接获取
                        log.info("基金{}在数据库中不存在，从实时接口获取数据", fundSelf.getFundCode());
                        FundLast newFund = fetchFundRealTimeData(fundSelf.getFundCode());
                        if (newFund != null) {
                            fundLastService.saveOrUpdateFundLast(newFund);
                            refreshCount++;
                        }
                        Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    log.error("刷新基金{}的实时数据失败", fundSelf.getFundCode(), e);
                }
            }

            log.info("实时数据刷新完成，共刷新{}只基金", refreshCount);
        } catch (Exception e) {
            log.error("从实时接口刷新数据失败", e);
        }
    }

    /**
     * 从历史接口刷新数据
     */
    private void refreshFromHistoryApi(String userName) {
        try {
            // 获取用户的所有自选基金
            LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FundSelf::getUserName, userName);
            List<FundSelf> fundSelfList = fundSelfMapper.selectList(wrapper);

            if (fundSelfList == null || fundSelfList.isEmpty()) {
                log.info("用户{}没有自选基金，跳过刷新", userName);
                return;
            }

            String today = LocalDate.now().format(DateFormatConstants.DATE_FORMATTER);
            LocalDateTime today1800 = LocalDate.now().atTime(BatchConstants.HISTORY_REFRESH_HOUR, 0, 0);
            int refreshCount = 0;

            for (FundSelf fundSelf : fundSelfList) {
                try {
                    // 获取数据库中现有的基金数据
                    FundLast existingFund = fundLastService.getFundLastByCode(fundSelf.getFundCode());

                    if (existingFund != null && existingFund.getDataTime() != null) {
                        // 判断数据时间是否在今天18:00之前
                        if (existingFund.getDataTime().isBefore(today1800)) {
                            log.info("基金{}的数据时间为{}，在今天{}:00之前，开始从历史接口刷新",
                                    fundSelf.getFundCode(), existingFund.getDataTime(),
                                    BatchConstants.HISTORY_REFRESH_HOUR);

                            // 从历史接口获取最新数据
                            boolean success = fetchAndUpdateHistoryData(
                                    fundSelf.getFundCode(),
                                    existingFund.getFundName(),
                                    today,
                                    today
                            );

                            if (success) {
                                refreshCount++;
                                log.info("成功刷新基金{}的历史数据", fundSelf.getFundCode());
                            }

                            // 每次请求后等待1秒，避免频繁调用
                            Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                        }
                    } else if (existingFund == null) {
                        // 如果数据库中不存在该基金数据，先从实时接口获取基金名称，再从历史接口获取数据
                        log.info("基金{}在数据库中不存在，先获取实时数据得到基金名称", fundSelf.getFundCode());
                        FundLast realTimeFund = fetchFundRealTimeData(fundSelf.getFundCode());
                        String actualFundName = fundSelf.getFundCode(); // 默认使用基金代码

                        if (realTimeFund != null && realTimeFund.getFundName() != null) {
                            actualFundName = realTimeFund.getFundName();
                            // 保存实时数据
                            fundLastService.saveOrUpdateFundLast(realTimeFund);
                            log.info("成功获取基金{}的实时数据和名称: {}", fundSelf.getFundCode(), actualFundName);
                        } else {
                            log.warn("获取基金{}的实时数据失败，使用基金代码作为名称", fundSelf.getFundCode());
                        }

                        boolean success = fetchAndUpdateHistoryData(
                                fundSelf.getFundCode(),
                                actualFundName,
                                today,
                                today
                        );
                        if (success) {
                            refreshCount++;
                        }
                        Thread.sleep(ApiConstants.API_REQUEST_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    log.error("刷新基金{}的历史数据失败", fundSelf.getFundCode(), e);
                }
            }

            log.info("历史数据刷新完成，共刷新{}只基金", refreshCount);
        } catch (Exception e) {
            log.error("从历史接口刷新数据失败", e);
        }
    }

    /**
     * 从 F10 API 获取基金历史净值并更新
     */
    private boolean fetchAndUpdateHistoryData(String fundCode, String fundName,
                                               String startDate, String endDate) {
        try {
            String url = String.format("%s?type=lsjz&code=%s&page=1&per=30&sdate=%s&edate=%s",
                    ApiConstants.EASTMONEY_F10_API, fundCode, startDate, endDate);

            log.debug("请求基金历史净值URL: {}", url);

            String response = HttpUtil.get(url, ApiConstants.API_TIMEOUT_MS); // 设置60秒超时

            if (response == null || response.trim().isEmpty()) {
                log.warn("基金{}的API返回空响应", fundCode);
                return false;
            }

            // 解析返回数据（复用 FundApiUtil 的解析逻辑）
            List<FundHistory> historyList = FundApiUtil.parseFundHistoryResponse(response, fundCode, fundName);

            if (historyList == null || historyList.isEmpty()) {
                log.warn("基金{}在时间范围{}-{}内没有获取到历史数据", fundCode, startDate, endDate);
                return false;
            }

            log.info("基金{}获取到{}条历史数据，开始保存", fundCode, historyList.size());

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            // 保存或更新每条历史数据
            for (FundHistory history : historyList) {
                try {
                    // 检查该日期的数据是否已存在
                    LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FundHistory::getFundCode, fundCode)
                           .eq(FundHistory::getDate, history.getDate());
                    Long dateCount = fundHistoryMapper.selectCount(wrapper);

                    if (dateCount > 0) {
                        log.debug("基金{}日期{}的数据已存在，跳过", fundCode, history.getDate());
                        skipCount++;
                        continue;
                    }

                    // 设置创建时间并插入
                    history.setCreateTime(LocalDateTime.now());
                    fundHistoryMapper.insert(history);
                    successCount++;
                    log.debug("成功保存基金{}日期{}的历史数据", fundCode, history.getDate());

                } catch (Exception e) {
                    log.error("保存基金{}日期{}的历史数据失败", fundCode, history.getDate(), e);
                    failCount++;
                }
            }

            log.info("基金{}历史数据获取完成 - 成功: {}, 跳过: {}, 失败: {}", fundCode, successCount, skipCount, failCount);

            // 同时更新实时数据表中的最新净值
            if (!historyList.isEmpty()) {
                FundHistory latestHistory = historyList.get(0); // 第一条是最新的
                FundLast existingFund = fundLastService.getFundLastByCode(fundCode);
                if (existingFund != null) {
                    // 更新当前净值（历史接口返回的单位净值）
                    existingFund.setCurrentPrice(latestHistory.getHistoryPrice());
                    // 注意：不更新prevPrice，保留原有的上一个交易日净值
                    // 如果有收益率信息，也更新
                    if (latestHistory.getProfitPercent() != null) {
                        existingFund.setProfitPercent(latestHistory.getProfitPercent());
                    }
                    // 设置数据时间为历史数据的日期
                    try {
                        LocalDateTime dataTime = LocalDate.parse(latestHistory.getDate(), DateFormatConstants.DATE_FORMATTER).atStartOfDay();
                        existingFund.setDataTime(dataTime);
                    } catch (Exception e) {
                        log.warn("基金{}的日期解析失败: {}", fundCode, latestHistory.getDate(), e);
                    }
                    fundLastService.saveOrUpdateFundLast(existingFund);
                    log.info("成功更新基金{}的实时数据表中的历史净值，净值={}, 数据时间={}",
                            fundCode, latestHistory.getHistoryPrice(), latestHistory.getDate());
                } else {
                    // 如果实时数据表中不存在该基金，创建新记录
                    FundLast newFundLast = new FundLast();
                    newFundLast.setFundCode(fundCode);
                    newFundLast.setFundName(fundName);
                    newFundLast.setCurrentPrice(latestHistory.getHistoryPrice());
                    // 新记录中，prevPrice设置为与currentPrice相同
                    newFundLast.setPrevPrice(latestHistory.getHistoryPrice());
                    if (latestHistory.getProfitPercent() != null) {
                        newFundLast.setProfitPercent(latestHistory.getProfitPercent());
                    }
                    try {
                        LocalDateTime dataTime = LocalDate.parse(latestHistory.getDate(), DateFormatConstants.DATE_FORMATTER).atStartOfDay();
                        newFundLast.setDataTime(dataTime);
                    } catch (Exception e) {
                        log.warn("基金{}的日期解析失败: {}", fundCode, latestHistory.getDate(), e);
                    }
                    fundLastService.saveOrUpdateFundLast(newFundLast);
                    log.info("成功为基金{}创建实时数据记录，净值={}, 数据时间={}",
                            fundCode, latestHistory.getHistoryPrice(), latestHistory.getDate());
                }
            }

            return successCount > 0;
        } catch (Exception e) {
            log.error("从 API 获取基金{}的历史净值数据失败", fundCode, e);
            return false;
        }
    }

    /**
     * 转换为响应对象
     */
    private FundSelfResponse convertToResponse(FundSelf fundSelf) {
        FundSelfResponse response = new FundSelfResponse();
        BeanUtil.copyProperties(fundSelf, response);

        // 关联查询实时基金数据
        try {
            FundLast fundLast = fundLastService.getFundLastByCode(fundSelf.getFundCode());
            if (fundLast != null) {
                // 设置基金名称
                response.setFundName(fundLast.getFundName());
                // 设置最新净值
                response.setCurrentPrice(fundLast.getCurrentPrice());
                // 设置上一个交易日净值
                response.setPrevPrice(fundLast.getPrevPrice());
                // 设置最新收益率
                response.setProfitPercent(fundLast.getProfitPercent());
                // 设置数据时间（优先使用dataTime，如果为空则使用updateTime）
                LocalDateTime fundUpdateTime = fundLast.getDataTime() != null ?
                        fundLast.getDataTime() : fundLast.getUpdateTime();
                response.setFundUpdateTime(fundUpdateTime);

                // 计算持有收益和持有收益率
                if (fundSelf.getBaseAmount() != null && fundSelf.getShareQuantity() != null
                        && fundLast.getCurrentPrice() != null) {
                    // 持有收益 = (当前净值 - 成本价) * 持有份额
                    BigDecimal holdingProfit = fundLast.getCurrentPrice().subtract(fundSelf.getBaseAmount())
                            .multiply(fundSelf.getShareQuantity());
                    response.setHoldingProfit(holdingProfit.setScale(2, RoundingMode.HALF_UP));

                    // 持有收益率 = (当前净值 - 成本价) / 成本价 * 100%
                    if (fundSelf.getBaseAmount().compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal holdingProfitPercent = fundLast.getCurrentPrice().subtract(fundSelf.getBaseAmount())
                                .divide(fundSelf.getBaseAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        response.setHoldingProfitPercent(holdingProfitPercent.setScale(2, RoundingMode.HALF_UP));
                    } else {
                        response.setHoldingProfitPercent(BigDecimal.ZERO);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取基金{}的实时数据失败", fundSelf.getFundCode(), e);
        }

        return response;
    }

    /**
     * 同步基金到实时数据表
     */
    private void syncToFundLast(String fundCode) {
        try {
            // 检查实时基金表中是否已存在该基金
            FundLast existingFund = fundLastService.getFundLastByCode(fundCode);
            if (existingFund != null) {
                log.info("基金{}已存在于实时数据表中，跳过添加", fundCode);
                return;
            }

            // 获取实时基金数据
            FundLast fundLast = fetchFundRealTimeData(fundCode);
            if (fundLast != null) {
                fundLastService.saveOrUpdateFundLast(fundLast);
                log.info("成功同步基金{}到实时数据表", fundCode);
            } else {
                log.warn("获取基金{}的实时数据失败，仅添加到自选列表", fundCode);
            }
        } catch (Exception e) {
            log.error("同步基金{}到实时数据表失败", fundCode, e);
        }
    }

    /**
     * 从天天基金API获取实时数据
     */
    private FundLast fetchFundRealTimeData(String fundCode) {
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
