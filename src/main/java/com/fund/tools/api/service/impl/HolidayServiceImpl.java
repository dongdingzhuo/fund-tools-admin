package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.config.BatchConstants;
import com.fund.tools.api.config.DateFormatConstants;
import com.fund.tools.api.dto.HolidayApiResponse;
import com.fund.tools.api.entity.Holiday;
import com.fund.tools.api.enums.YesNoEnum;
import com.fund.tools.api.mapper.HolidayMapper;
import com.fund.tools.api.service.HolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日日期服务实现类
 */
@Slf4j
@Service
public class HolidayServiceImpl implements HolidayService {

    @Resource
    private HolidayMapper holidayMapper;

    private static final String API_URL = ApiConstants.HOLIDAY_API;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateHoliday(Holiday holiday) {
        // 查询是否已存在
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getDate, holiday.getDate());
        Holiday existing = holidayMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新（t_holiday不需要更新时间）
            holiday.setId(existing.getId());
            return holidayMapper.updateById(holiday) > 0;
        } else {
            // 新增，设置创建时间
            holiday.setCreateTime(LocalDateTime.now());
            return holidayMapper.insert(holiday) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveOrUpdateHoliday(List<Holiday> holidayList) {
        if (holidayList == null || holidayList.isEmpty()) {
            return false;
        }

        for (Holiday holiday : holidayList) {
            saveOrUpdateHoliday(holiday);
        }
        return true;
    }

    @Override
    public Holiday getHolidayByDate(String date) {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getDate, date);
        return holidayMapper.selectOne(wrapper);
    }

    @Override
    public List<Holiday> getAllHolidays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getHolidaysByDateRange(String startDate, String endDate) {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Holiday::getDate, startDate)
                .le(Holiday::getDate, endDate)
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getWorkdays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getWorkdayFlag, YesNoEnum.YES.getCode())
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getHolidays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getHolidayFlag, YesNoEnum.YES.getCode())
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getWeekends() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getWeekendFlag, YesNoEnum.YES.getCode())
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getTradingDays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getTradingDayFlag, YesNoEnum.YES.getCode())
                .orderByDesc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public boolean isWorkday(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && YesNoEnum.isYes(holiday.getWorkdayFlag());
    }

    @Override
    public boolean isHoliday(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && YesNoEnum.isYes(holiday.getHolidayFlag());
    }

    @Override
    public boolean isWeekend(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && YesNoEnum.isYes(holiday.getWeekendFlag());
    }

    @Override
    public boolean isTradingDay(String date) {
        // 先检查数据库中是否存在该日期
        Holiday holiday = getHolidayByDate(date);
        if (holiday != null) {
            log.debug("日期{}在数据库中存在，直接返回交易日状态: {}", date, YesNoEnum.isYes(holiday.getTradingDayFlag()));
            return YesNoEnum.isYes(holiday.getTradingDayFlag());
        }

        // 如果不存在，需要获取缺失的日期数据
        log.info("日期{}在数据库中不存在，开始获取缺失的节假日数据", date);
        fetchMissingHolidayData(date);

        // 重新查询
        holiday = getHolidayByDate(date);
        if (holiday != null) {
            log.info("成功获取日期{}的数据，交易日状态: {}", date, YesNoEnum.isYes(holiday.getTradingDayFlag()));
            return YesNoEnum.isYes(holiday.getTradingDayFlag());
        } else {
            log.error("获取日期{}的数据失败", date);
            return false;
        }
    }

    @Override
    public String getNearestTradingDay(String date) {
        // 查询所有交易日，按日期降序排列
        List<Holiday> tradingDays = getTradingDays();
        
        if (tradingDays.isEmpty()) {
            return null;
        }
        
        // 查找小于等于指定日期的最近交易日
        String nearestTradingDay = null;
        for (Holiday holiday : tradingDays) {
            if (holiday.getDate().compareTo(date) <= 0) {
                nearestTradingDay = holiday.getDate();
                break;
            }
        }
        
        return nearestTradingDay;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteHoliday(String date) {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getDate, date);
        return holidayMapper.delete(wrapper) > 0;
    }

    /**
     * 获取缺失的节假日数据
     * 从数据库中最后一个日期开始，到目标日期结束
     *
     * @param targetDate 目标日期
     */
    private void fetchMissingHolidayData(String targetDate) {
        try {
            // 获取数据库中最后一个日期（根据ID倒序取第一条）
            LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(Holiday::getId)
                   .last("LIMIT 1");
            Holiday lastHoliday = holidayMapper.selectOne(wrapper);
            
            String startDate;
            if (lastHoliday == null) {
                // 如果数据库为空，从2026-01-01开始
                startDate = "2026-01-01";
                log.info("数据库中没有节假日数据，从{}开始获取", startDate);
            } else {
                // 获取最后一个日期
                startDate = lastHoliday.getDate();
                log.info("数据库中最后日期为{}，从该日期之后开始获取", startDate);
                
                // 如果目标日期早于或等于最后日期，不需要获取
                if (targetDate.compareTo(startDate) <= 0) {
                    log.warn("目标日期{}早于或等于数据库最后日期{}，无需获取新数据", targetDate, startDate);
                    return;
                }
                
                // 从最后日期的下一天开始
                LocalDate startLocalDate = LocalDate.parse(startDate, DateFormatConstants.DATE_FORMATTER);
                startDate = startLocalDate.plusDays(1).format(DateFormatConstants.DATE_FORMATTER);
            }

            log.info("开始获取节假日数据，范围: {} 到 {}", startDate, targetDate);

            List<Holiday> holidayList = new ArrayList<>();
            LocalDate currentDate = LocalDate.parse(startDate, DateFormatConstants.DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(targetDate, DateFormatConstants.DATE_FORMATTER);
            int successCount = 0;
            int failCount = 0;

            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(DateFormatConstants.DATE_FORMATTER);

                // 再次检查是否已存在（防止并发情况）
                Holiday existing = getHolidayByDate(dateStr);
                if (existing != null) {
                    log.debug("日期{}的数据已存在，跳过", dateStr);
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                Holiday holiday = fetchAndCreateHoliday(dateStr);
                if (holiday != null) {
                    holidayList.add(holiday);
                    successCount++;
                    log.info("成功获取日期{}的数据", dateStr);
                } else {
                    failCount++;
                    log.warn("获取日期{}的数据失败", dateStr);
                }

                // 每7条保存一次
                if (holidayList.size() >= BatchConstants.HOLIDAY_BATCH_SIZE) {
                    batchSaveOrUpdateHoliday(holidayList);
                    log.info("批次保存完成，本批次保存{}条数据", holidayList.size());
                    holidayList.clear();
                }

                // 每次API调用后等待3秒，避免被限流
                if (!currentDate.isAfter(endDate)) {
                    Thread.sleep(ApiConstants.HOLIDAY_API_INTERVAL_MS);
                }

                currentDate = currentDate.plusDays(1);
            }

            // 保存剩余的数据
            if (!holidayList.isEmpty()) {
                batchSaveOrUpdateHoliday(holidayList);
                log.info("保存剩余{}条数据", holidayList.size());
            }

            log.info("获取节假日数据统计 - 成功: {}, 失败: {}", successCount, failCount);
        } catch (InterruptedException e) {
            log.error("获取节假日数据过程被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("获取缺失的节假日数据失败", e);
        }
    }

    /**
     * 从API获取数据并创建Holiday对象
     */
    private Holiday fetchAndCreateHoliday(String date) {
        try {
            String url = API_URL + date;
            String response = HttpUtil.get(url, ApiConstants.API_TIMEOUT_MS); // 设置60秒超时
            log.debug("请求API: {}, API返回: {}", url, response);
            
            // 检查响应是否为空
            if (response == null || response.trim().isEmpty()) {
                log.warn("获取日期{}的API返回空响应", date);
                return null;
            }

            HolidayApiResponse apiResponse = JSONUtil.toBean(response, HolidayApiResponse.class);

            // 检查响应码和类型信息
            if (apiResponse == null || apiResponse.getCode() == null || apiResponse.getCode() != 0) {
                log.warn("获取日期{}的API数据异常，响应码: {}, 响应内容: {}", date,
                        apiResponse != null ? apiResponse.getCode() : "null", response);
                return null;
            }

            if (apiResponse.getType() == null) {
                log.warn("获取日期{}的API数据缺少type字段，响应内容: {}", date, response);
                return null;
            }

            Holiday holiday = new Holiday();
            holiday.setDate(date);

            HolidayApiResponse.TypeInfo typeInfo = apiResponse.getType();
            HolidayApiResponse.HolidayInfo holidayInfo = apiResponse.getHoliday();

            // 设置星期
            if (typeInfo.getWeek() == null) {
                log.warn("获取日期{}的API数据缺少week字段", date);
                return null;
            }
            holiday.setWeek(typeInfo.getWeek());

            // 判断是否为周末（type=1表示周末）
            boolean isWeekend = typeInfo.getType() != null && typeInfo.getType() == 1;
            holiday.setWeekendFlag(isWeekend ? YesNoEnum.YES.getCode() : YesNoEnum.NO.getCode());

            // 判断是否为节假日
            boolean isHoliday = holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday());
            holiday.setHolidayFlag(isHoliday ? YesNoEnum.YES.getCode() : YesNoEnum.NO.getCode());

            // 判断是否为工作日（非周末且非节假日）
            boolean isWorkday = !isWeekend && !isHoliday;
            holiday.setWorkdayFlag(isWorkday ? YesNoEnum.YES.getCode() : YesNoEnum.NO.getCode());

            // 判断是否为交易日（周一到周五，且不是节假日）
            // week: 0-周日, 1-周一, 2-周二, 3-周三, 4-周四, 5-周五, 6-周六
            boolean isTradingDay = typeInfo.getWeek() >= 1 && typeInfo.getWeek() <= 5 && !isHoliday;
            holiday.setTradingDayFlag(isTradingDay ? YesNoEnum.YES.getCode() : YesNoEnum.NO.getCode());

            log.debug("成功获取日期{}的数据: week={}, weekend={}, holiday={}, workday={}, tradingDay={}",
                    date, holiday.getWeek(), holiday.getWeekendFlag(), holiday.getHolidayFlag(),
                    holiday.getWorkdayFlag(), holiday.getTradingDayFlag());

            return holiday;
        } catch (Exception e) {
            log.error("从API获取日期{}的数据失败", date, e);
            return null;
        }
    }
}
