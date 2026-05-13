package com.fund.tools.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.tools.api.entity.FundLast;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实时基金数据 Mapper 接口
 */
@Mapper
public interface FundLastMapper extends BaseMapper<FundLast> {
}
