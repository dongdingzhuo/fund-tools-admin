package com.fund.tools.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.tools.api.entity.Holiday;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节假日日期 Mapper 接口
 */
@Mapper
public interface HolidayMapper extends BaseMapper<Holiday> {
}
