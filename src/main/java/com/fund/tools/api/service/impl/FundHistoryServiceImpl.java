package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.dto.FundHistoryResponse;
import com.fund.tools.api.entity.FundHistory;
import com.fund.tools.api.mapper.FundHistoryMapper;
import com.fund.tools.api.service.FundHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基金历史净值服务实现类
 */
@Service
public class FundHistoryServiceImpl implements FundHistoryService {

    @Resource
    private FundHistoryMapper fundHistoryMapper;

    @Override
    public List<FundHistoryResponse> getHistoryByFundCodeAndDateRange(String fundCode, String startDate, String endDate) {
        LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundHistory::getFundCode, fundCode);
        
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(FundHistory::getDate, startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(FundHistory::getDate, endDate);
        }
        
        wrapper.orderByDesc(FundHistory::getDate);
        
        List<FundHistory> historyList = fundHistoryMapper.selectList(wrapper);
        
        return historyList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FundHistory> getHistoryEntitiesByFundCodeAndDateRange(String fundCode, String startDate, String endDate) {
        LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundHistory::getFundCode, fundCode);
        
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(FundHistory::getDate, startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(FundHistory::getDate, endDate);
        }
        
        wrapper.orderByDesc(FundHistory::getDate);
        
        return fundHistoryMapper.selectList(wrapper);
    }

    @Override
    public boolean existsByFundCodeAndDate(String fundCode, String date) {
        LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundHistory::getFundCode, fundCode)
                .eq(FundHistory::getDate, date);
        Long count = fundHistoryMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateHistory(FundHistory history) {
        // 查询是否已存在
        LambdaQueryWrapper<FundHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundHistory::getFundCode, history.getFundCode())
                .eq(FundHistory::getDate, history.getDate());
        FundHistory existing = fundHistoryMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            history.setId(existing.getId());
            return fundHistoryMapper.updateById(history) > 0;
        } else {
            // 新增
            return fundHistoryMapper.insert(history) > 0;
        }
    }

    /**
     * 转换为响应对象
     */
    private FundHistoryResponse convertToResponse(FundHistory fundHistory) {
        FundHistoryResponse response = new FundHistoryResponse();
        BeanUtil.copyProperties(fundHistory, response);
        return response;
    }
}
