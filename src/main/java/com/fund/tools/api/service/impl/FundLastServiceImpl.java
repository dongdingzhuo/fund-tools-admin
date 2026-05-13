package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.mapper.FundLastMapper;
import com.fund.tools.api.service.FundLastService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时基金数据服务实现类
 */
@Service
public class FundLastServiceImpl implements FundLastService {

    @Resource
    private FundLastMapper fundLastMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateFundLast(FundLast fundLast) {
        // 查询是否已存在
        LambdaQueryWrapper<FundLast> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundLast::getFundCode, fundLast.getFundCode());
        FundLast existing = fundLastMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            fundLast.setId(existing.getId());
            fundLast.setUpdateTime(LocalDateTime.now());
            return fundLastMapper.updateById(fundLast) > 0;
        } else {
            // 新增
            fundLast.setUpdateTime(LocalDateTime.now());
            return fundLastMapper.insert(fundLast) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveOrUpdateFundLast(List<FundLast> fundLastList) {
        if (fundLastList == null || fundLastList.isEmpty()) {
            return false;
        }

        for (FundLast fundLast : fundLastList) {
            saveOrUpdateFundLast(fundLast);
        }
        return true;
    }

    @Override
    public FundLast getFundLastByCode(String fundCode) {
        LambdaQueryWrapper<FundLast> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundLast::getFundCode, fundCode);
        return fundLastMapper.selectOne(wrapper);
    }

    @Override
    public List<FundLast> getAllFundLast() {
        LambdaQueryWrapper<FundLast> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FundLast::getUpdateTime);
        return fundLastMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFundLast(String fundCode) {
        LambdaQueryWrapper<FundLast> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundLast::getFundCode, fundCode);
        return fundLastMapper.delete(wrapper) > 0;
    }
}
