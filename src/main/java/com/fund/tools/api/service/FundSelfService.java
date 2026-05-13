package com.fund.tools.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.tools.api.dto.FundSelfRequest;
import com.fund.tools.api.dto.FundSelfResponse;

import java.util.List;

/**
 * 自选基金服务接口
 */
public interface FundSelfService {

    /**
     * 添加自选基金
     *
     * @param request 请求参数
     * @return 是否成功
     */
    boolean addFundSelf(FundSelfRequest request);

    /**
     * 更新自选基金
     *
     * @param id      自选基金ID
     * @param request 请求参数
     * @return 是否成功
     */
    boolean updateFundSelf(Long id, FundSelfRequest request);

    /**
     * 删除自选基金
     *
     * @param id 自选基金ID
     * @return 是否成功
     */
    boolean deleteFundSelf(Long id);

    /**
     * 根据ID查询自选基金
     *
     * @param id 自选基金ID
     * @return 自选基金信息
     */
    FundSelfResponse getFundSelfById(Long id);

    /**
     * 根据用户账号查询自选基金列表
     *
     * @param userName 用户账号
     * @return 自选基金列表
     */
    List<FundSelfResponse> getFundSelfByUserName(String userName);

    /**
     * 分页查询自选基金
     *
     * @param userName 用户账号（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<FundSelfResponse> pageFundSelf(String userName, Integer pageNum, Integer pageSize);
}
