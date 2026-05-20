package com.fund.tools.api.service;

import com.fund.tools.api.dto.PlateFlowResponse;

import java.util.List;

/**
 * 板块资金流向服务接口
 */
public interface PlateFlowService {

    /**
     * 获取板块资金流向列表（从数据库查询）
     *
     * @return 板块资金流向列表
     */
    List<PlateFlowResponse> getPlateFlowListFromDb();

    /**
     * 从API获取并保存板块资金流向数据
     *
     * @return 是否成功
     */
    boolean fetchAndSavePlateFlow();
}
