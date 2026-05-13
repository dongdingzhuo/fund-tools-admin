package com.fund.tools.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.tools.api.entity.FundHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基金历史净值 Mapper 接口
 */
@Mapper
public interface FundHistoryMapper extends BaseMapper<FundHistory> {
}
