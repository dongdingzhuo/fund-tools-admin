package com.fund.tools.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.tools.api.entity.FundSelf;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自选基金 Mapper 接口
 */
@Mapper
public interface FundSelfMapper extends BaseMapper<FundSelf> {
}
