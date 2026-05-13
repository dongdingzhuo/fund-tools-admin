package com.fund.tools.api.service;

import com.fund.tools.api.entity.FundLast;

import java.util.List;

/**
 * 实时基金数据服务接口
 */
public interface FundLastService {

    /**
     * 保存或更新基金数据
     *
     * @param fundLast 基金数据
     * @return 是否成功
     */
    boolean saveOrUpdateFundLast(FundLast fundLast);

    /**
     * 批量保存或更新基金数据
     *
     * @param fundLastList 基金数据列表
     * @return 是否成功
     */
    boolean batchSaveOrUpdateFundLast(List<FundLast> fundLastList);

    /**
     * 根据基金代码查询基金数据
     *
     * @param fundCode 基金代码
     * @return 基金数据
     */
    FundLast getFundLastByCode(String fundCode);

    /**
     * 查询所有基金数据
     *
     * @return 基金数据列表
     */
    List<FundLast> getAllFundLast();

    /**
     * 删除基金数据
     *
     * @param fundCode 基金代码
     * @return 是否成功
     */
    boolean deleteFundLast(String fundCode);
}
