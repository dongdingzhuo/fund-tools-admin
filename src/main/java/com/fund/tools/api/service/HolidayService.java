package com.fund.tools.api.service;

import com.fund.tools.api.entity.Holiday;

import java.util.List;

/**
 * 节假日日期服务接口
 */
public interface HolidayService {

    /**
     * 保存或更新日期信息
     *
     * @param holiday 日期信息
     * @return 是否成功
     */
    boolean saveOrUpdateHoliday(Holiday holiday);

    /**
     * 批量保存或更新日期信息
     *
     * @param holidayList 日期信息列表
     * @return 是否成功
     */
    boolean batchSaveOrUpdateHoliday(List<Holiday> holidayList);

    /**
     * 根据日期查询
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 日期信息
     */
    Holiday getHolidayByDate(String date);

    /**
     * 查询所有日期信息
     *
     * @return 日期信息列表
     */
    List<Holiday> getAllHolidays();

    /**
     * 查询指定日期范围内的日期信息
     *
     * @param startDate 开始日期（yyyy-MM-dd格式）
     * @param endDate   结束日期（yyyy-MM-dd格式）
     * @return 日期信息列表
     */
    List<Holiday> getHolidaysByDateRange(String startDate, String endDate);

    /**
     * 查询所有工作日
     *
     * @return 工作日列表
     */
    List<Holiday> getWorkdays();

    /**
     * 查询所有节假日
     *
     * @return 节假日列表
     */
    List<Holiday> getHolidays();

    /**
     * 查询所有周末
     *
     * @return 周末列表
     */
    List<Holiday> getWeekends();

    /**
     * 查询所有交易日
     *
     * @return 交易日列表
     */
    List<Holiday> getTradingDays();

    /**
     * 判断指定日期是否为工作日
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 是否为工作日
     */
    boolean isWorkday(String date);

    /**
     * 判断指定日期是否为节假日
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 是否为节假日
     */
    boolean isHoliday(String date);

    /**
     * 判断指定日期是否为周末
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 是否为周末
     */
    boolean isWeekend(String date);

    /**
     * 判断指定日期是否为交易日
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 是否为交易日
     */
    boolean isTradingDay(String date);

    /**
     * 获取指定日期最近的交易日
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 最近的交易日日期，如果未找到返回null
     */
    String getNearestTradingDay(String date);

    /**
     * 删除日期信息
     *
     * @param date 日期（yyyy-MM-dd格式）
     * @return 是否成功
     */
    boolean deleteHoliday(String date);
}
