package com.fund.tools.api.task;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.fund.tools.api.dto.HolidayApiResponse;
import com.fund.tools.api.entity.Holiday;
import com.fund.tools.api.service.HolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日数据定时任务
 */
@Slf4j
@Component
public class HolidayTask {

    @Resource
    private HolidayService holidayService;

    private static final String API_URL = "https://timor.tech/api/holiday/info/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每日凌晨1点执行，更新节假日数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateHolidayData() {
        log.info("开始执行节假日数据更新任务");
        try {
            // 获取当前日期
            String today = LocalDate.now().format(DATE_FORMATTER);

            // 检查今天的数据是否已存在
            Holiday existingHoliday = holidayService.getHolidayByDate(today);
            if (existingHoliday != null) {
                log.info("今日({})的节假日数据已存在，跳过新增", today);
            } else {
                // 新增今天的数据
                addHolidayData(today);
            }

            // 删除12个月以前的数据
            deleteOldHolidayData();

            log.info("节假日数据更新任务执行完成");
        } catch (Exception e) {
            log.error("节假日数据更新任务执行失败", e);
        }
    }

    /**
     * 首次执行时批量初始化数据（从2026-01-01到今天）
     * 可以通过手动调用此方法或应用启动时执行
     */
    public void initHolidayData() {
        log.info("开始初始化节假日数据");
        try {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.now();

            log.info("初始化日期范围: {} 到 {}", startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER));

            List<Holiday> holidayList = new ArrayList<>();
            LocalDate currentDate = startDate;
            int successCount = 0;
            int failCount = 0;
            int skipCount = 0;
            int totalCount = 0;
            int batchSaveCount = 0;
            final int BATCH_SIZE = 7; // 每7条保存一次

            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                totalCount++;

                // 检查是否已存在
                Holiday existing = holidayService.getHolidayByDate(dateStr);
                if (existing != null) {
                    skipCount++;
                    log.debug("日期{}的数据已存在，跳过", dateStr);
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                Holiday holiday = fetchAndCreateHoliday(dateStr);
                if (holiday != null) {
                    holidayList.add(holiday);
                    successCount++;
                    log.info("成功获取日期{}的数据 ({}/{})", dateStr, successCount + failCount, totalCount);
                } else {
                    failCount++;
                    log.warn("获取日期{}的数据失败 ({}/{})", dateStr, successCount + failCount, totalCount);
                }

                // 每7条保存一次
                if (holidayList.size() >= BATCH_SIZE) {
                    holidayService.batchSaveOrUpdateHoliday(holidayList);
                    batchSaveCount += holidayList.size();
                    log.info("批次保存完成，已保存{}条数据", batchSaveCount);
                    holidayList.clear();
                }

                // 每次API调用后等待10秒，避免被限流
                if (!currentDate.isAfter(endDate)) {
                    log.debug("等待10秒后继续...");
                    Thread.sleep(3000);
                }

                currentDate = currentDate.plusDays(1);
            }

            // 保存剩余的数据
            if (!holidayList.isEmpty()) {
                holidayService.batchSaveOrUpdateHoliday(holidayList);
                batchSaveCount += holidayList.size();
                log.info("保存剩余{}条数据，总计保存{}条", holidayList.size(), batchSaveCount);
            }

            log.info("初始化统计 - 总天数: {}, 成功: {}, 失败: {}, 跳过: {}, 分批保存次数: {}",
                    totalCount, successCount, failCount, skipCount,
                    holidayList.isEmpty() ? batchSaveCount / BATCH_SIZE + 1 : batchSaveCount / BATCH_SIZE);
        } catch (InterruptedException e) {
            log.error("初始化过程被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("初始化节假日数据失败", e);
        }
    }

    /**
     * 添加单天节假日数据
     */
    private void addHolidayData(String date) {
        try {
            Holiday holiday = fetchAndCreateHoliday(date);
            if (holiday != null) {
                holidayService.saveOrUpdateHoliday(holiday);
                log.info("成功添加节假日数据: {}", date);
            }
        } catch (Exception e) {
            log.error("添加节假日数据失败: {}", date, e);
        }
    }

    /**
     * 从 API获取数据并创建 Holiday对象
     */
    private Holiday fetchAndCreateHoliday(String date) {
        try {
            String url = API_URL + date;
            String response = HttpUtil.get(url, 60000); // 设置60秒超时
            log.info("请求 API: {}, API返回: {}", url, response);
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
            holiday.setWeekendFlag(isWeekend ? "Y" : "N");

            // 判断是否为节假日
            boolean isHoliday = holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday());
            holiday.setHolidayFlag(isHoliday ? "Y" : "N");

            // 判断是否为工作日（非周末且非节假日）
            boolean isWorkday = !isWeekend && !isHoliday;
            holiday.setWorkdayFlag(isWorkday ? "Y" : "N");

            // 判断是否为交易日（周一到周五，且不是节假日）
            // week: 0-周日, 1-周一, 2-周二, 3-周三, 4-周四, 5-周五, 6-周六
            boolean isTradingDay = typeInfo.getWeek() >= 1 && typeInfo.getWeek() <= 5 && !isHoliday;
            holiday.setTradingDayFlag(isTradingDay ? "Y" : "N");

            log.debug("成功获取日期{}的数据: week={}, weekend={}, holiday={}, workday={}, tradingDay={}",
                    date, holiday.getWeek(), holiday.getWeekendFlag(), holiday.getHolidayFlag(),
                    holiday.getWorkdayFlag(), holiday.getTradingDayFlag());

            return holiday;
        } catch (Exception e) {
            log.error("从 API获取日期{}的数据失败", date, e);
            return null;
        }
    }

    /**
     * 删除12个月以前的数据
     */
    private void deleteOldHolidayData() {
        try {
            LocalDate twelveMonthsAgo = LocalDate.now().minusMonths(12);
            String cutoffDate = twelveMonthsAgo.format(DATE_FORMATTER);

            // 查询所有数据
            List<Holiday> allHolidays = holidayService.getAllHolidays();

            int deletedCount = 0;
            for (Holiday holiday : allHolidays) {
                // 如果日期早于12个月前，则删除
                if (holiday.getDate().compareTo(cutoffDate) < 0) {
                    holidayService.deleteHoliday(holiday.getDate());
                    deletedCount++;
                }
            }

            if (deletedCount > 0) {
                log.info("删除12个月以前的节假日数据，共{}条", deletedCount);
            }
        } catch (Exception e) {
            log.error("删除旧节假日数据失败", e);
        }
    }
}
