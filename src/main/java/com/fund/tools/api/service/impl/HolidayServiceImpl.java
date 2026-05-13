package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.entity.Holiday;
import com.fund.tools.api.mapper.HolidayMapper;
import com.fund.tools.api.service.HolidayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 节假日日期服务实现类
 */
@Service
public class HolidayServiceImpl implements HolidayService {

    @Resource
    private HolidayMapper holidayMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateHoliday(Holiday holiday) {
        // 查询是否已存在
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getDate, holiday.getDate());
        Holiday existing = holidayMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            holiday.setId(existing.getId());
            holiday.setUpdateTime(LocalDateTime.now());
            return holidayMapper.updateById(holiday) > 0;
        } else {
            // 新增
            holiday.setCreateTime(LocalDateTime.now());
            holiday.setUpdateTime(LocalDateTime.now());
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
        wrapper.eq(Holiday::getWorkdayFlag, "Y")
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getHolidays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getHolidayFlag, "Y")
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getWeekends() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getWeekendFlag, "Y")
                .orderByAsc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public List<Holiday> getTradingDays() {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getTradingDayFlag, "Y")
                .orderByDesc(Holiday::getDate);
        return holidayMapper.selectList(wrapper);
    }

    @Override
    public boolean isWorkday(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && "Y".equals(holiday.getWorkdayFlag());
    }

    @Override
    public boolean isHoliday(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && "Y".equals(holiday.getHolidayFlag());
    }

    @Override
    public boolean isWeekend(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && "Y".equals(holiday.getWeekendFlag());
    }

    @Override
    public boolean isTradingDay(String date) {
        Holiday holiday = getHolidayByDate(date);
        return holiday != null && "Y".equals(holiday.getTradingDayFlag());
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
}
