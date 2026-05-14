package com.fund.tools.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.tools.api.entity.FundSelf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 自选基金 Mapper 接口
 */
@Mapper
public interface FundSelfMapper extends BaseMapper<FundSelf> {

    /**
     * 获取所有唯一的基金代码列表
     *
     * @return 基金代码列表
     */
    @Select("SELECT DISTINCT fund_code FROM t_fund_self")
    List<String> selectDistinctFundCodes();
}
