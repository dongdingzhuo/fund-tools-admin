package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.tools.api.dto.FundSelfRequest;
import com.fund.tools.api.dto.FundSelfResponse;
import com.fund.tools.api.entity.FundSelf;
import com.fund.tools.api.mapper.FundSelfMapper;
import com.fund.tools.api.service.FundSelfService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自选基金服务实现类
 */
@Service
public class FundSelfServiceImpl implements FundSelfService {

    @Resource
    private FundSelfMapper fundSelfMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addFundSelf(FundSelfRequest request) {
        // 检查是否已存在
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, request.getUserName())
                .eq(FundSelf::getFundCode, request.getFundCode());
        Long count = fundSelfMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该基金已在自选列表中");
        }

        FundSelf fundSelf = new FundSelf();
        BeanUtil.copyProperties(request, fundSelf);
        return fundSelfMapper.insert(fundSelf) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFundSelf(Long id, FundSelfRequest request) {
        FundSelf fundSelf = fundSelfMapper.selectById(id);
        if (fundSelf == null) {
            throw new RuntimeException("自选基金不存在");
        }

        // 检查是否与其他记录冲突
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, request.getUserName())
                .eq(FundSelf::getFundCode, request.getFundCode())
                .ne(FundSelf::getId, id);
        Long count = fundSelfMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该基金已在自选列表中");
        }

        BeanUtil.copyProperties(request, fundSelf);
        return fundSelfMapper.updateById(fundSelf) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFundSelf(Long id) {
        return fundSelfMapper.deleteById(id) > 0;
    }

    @Override
    public FundSelfResponse getFundSelfById(Long id) {
        FundSelf fundSelf = fundSelfMapper.selectById(id);
        if (fundSelf == null) {
            throw new RuntimeException("自选基金不存在");
        }
        return convertToResponse(fundSelf);
    }

    @Override
    public List<FundSelfResponse> getFundSelfByUserName(String userName) {
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSelf::getUserName, userName)
                .orderByDesc(FundSelf::getCreateTime);
        List<FundSelf> fundSelfList = fundSelfMapper.selectList(wrapper);
        return fundSelfList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FundSelfResponse> pageFundSelf(String userName, Integer pageNum, Integer pageSize) {
        Page<FundSelf> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FundSelf> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isEmpty()) {
            wrapper.eq(FundSelf::getUserName, userName);
        }
        wrapper.orderByDesc(FundSelf::getCreateTime);
        
        Page<FundSelf> fundSelfPage = fundSelfMapper.selectPage(page, wrapper);
        
        Page<FundSelfResponse> responsePage = new Page<>();
        BeanUtil.copyProperties(fundSelfPage, responsePage, "records");
        responsePage.setRecords(fundSelfPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        
        return responsePage;
    }

    /**
     * 转换为响应对象
     */
    private FundSelfResponse convertToResponse(FundSelf fundSelf) {
        FundSelfResponse response = new FundSelfResponse();
        BeanUtil.copyProperties(fundSelf, response);
        return response;
    }
}
