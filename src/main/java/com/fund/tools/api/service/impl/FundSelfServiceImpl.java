package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.tools.api.dto.FundSelfRequest;
import com.fund.tools.api.dto.FundSelfResponse;
import com.fund.tools.api.dto.TiantianFundResponse;
import com.fund.tools.api.entity.FundLast;
import com.fund.tools.api.entity.FundSelf;
import com.fund.tools.api.mapper.FundSelfMapper;
import com.fund.tools.api.service.FundLastService;
import com.fund.tools.api.service.FundSelfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自选基金服务实现类
 */
@Slf4j
@Service
public class FundSelfServiceImpl implements FundSelfService {

    @Resource
    private FundSelfMapper fundSelfMapper;

    @Resource
    private FundLastService fundLastService;

    private static final String TIAN_TIAN_API_URL = "https://fundgz.1234567.com.cn/js/";

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
        boolean insertSuccess = fundSelfMapper.insert(fundSelf) > 0;
        
        if (insertSuccess) {
            // 同步到实时基金表
            syncToFundLast(request.getFundCode());
        }
        
        return insertSuccess;
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

        // 关联查询实时基金数据
        try {
            FundLast fundLast = fundLastService.getFundLastByCode(fundSelf.getFundCode());
            if (fundLast != null) {
                // 设置基金名称
                response.setFundName(fundLast.getFundName());
                // 设置最新净值
                response.setCurrentPrice(fundLast.getCurrentPrice());
                // 设置最新收益率
                response.setProfitPercent(fundLast.getProfitPercent());
                // 设置实时数据更新时间
                response.setFundUpdateTime(fundLast.getUpdateTime());

                // 计算持有收益和持有收益率
                if (fundSelf.getBaseAmount() != null && fundSelf.getShareQuantity() != null 
                        && fundLast.getCurrentPrice() != null) {
                    // 持有收益 = (当前净值 - 成本价) * 持有份额
                    BigDecimal holdingProfit = fundLast.getCurrentPrice().subtract(fundSelf.getBaseAmount())
                            .multiply(fundSelf.getShareQuantity());
                    response.setHoldingProfit(holdingProfit.setScale(2, BigDecimal.ROUND_HALF_UP));

                    // 持有收益率 = (当前净值 - 成本价) / 成本价 * 100%
                    if (fundSelf.getBaseAmount().compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal holdingProfitPercent = fundLast.getCurrentPrice().subtract(fundSelf.getBaseAmount())
                                .divide(fundSelf.getBaseAmount(), 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal("100"));
                        response.setHoldingProfitPercent(holdingProfitPercent.setScale(2, BigDecimal.ROUND_HALF_UP));
                    } else {
                        response.setHoldingProfitPercent(BigDecimal.ZERO);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取基金{}的实时数据失败", fundSelf.getFundCode(), e);
        }

        return response;
    }

    /**
     * 同步基金到实时数据表
     */
    private void syncToFundLast(String fundCode) {
        try {
            // 检查实时基金表中是否已存在该基金
            FundLast existingFund = fundLastService.getFundLastByCode(fundCode);
            if (existingFund != null) {
                log.info("基金{}已存在于实时数据表中，跳过添加", fundCode);
                return;
            }

            // 获取实时基金数据
            FundLast fundLast = fetchFundRealTimeData(fundCode);
            if (fundLast != null) {
                fundLastService.saveOrUpdateFundLast(fundLast);
                log.info("成功同步基金{}到实时数据表", fundCode);
            } else {
                log.warn("获取基金{}的实时数据失败，仅添加到自选列表", fundCode);
            }
        } catch (Exception e) {
            log.error("同步基金{}到实时数据表失败", fundCode, e);
        }
    }

    /**
     * 从天天基金API获取实时数据
     */
    private FundLast fetchFundRealTimeData(String fundCode) {
        try {
            String url = TIAN_TIAN_API_URL + fundCode + ".js";
            log.debug("请求基金数据: {}", url);

            String response = HttpUtil.get(url, 5000); // 设置5秒超时

            // 解析返回数据，格式为：jsonpgz({...});
            if (response == null || response.trim().isEmpty()) {
                log.warn("基金{}的API返回空响应", fundCode);
                return null;
            }

            // 提取JSON部分
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                log.warn("基金{}的API返回数据格式错误", fundCode);
                return null;
            }

            TiantianFundResponse apiResponse = JSONUtil.toBean(jsonStr, TiantianFundResponse.class);

            if (apiResponse == null || apiResponse.getFundcode() == null) {
                log.warn("基金{}的API数据解析失败", fundCode);
                return null;
            }

            // 构建FundLast对象
            FundLast fundLast = new FundLast();
            fundLast.setFundCode(apiResponse.getFundcode());
            fundLast.setFundName(apiResponse.getName());

            // 设置估算值作为当前价格
            if (apiResponse.getGsz() != null && !apiResponse.getGsz().isEmpty()) {
                fundLast.setCurrentPrice(new BigDecimal(apiResponse.getGsz()));
            }

            // 设置估算涨跌幅作为收益率
            if (apiResponse.getGszzl() != null && !apiResponse.getGszzl().isEmpty()) {
                fundLast.setProfitPercent(new BigDecimal(apiResponse.getGszzl()));
            }

            log.info("成功获取基金{}的实时数据: name={}, price={}, profit={}",
                    fundCode, apiResponse.getName(), apiResponse.getGsz(), apiResponse.getGszzl());

            return fundLast;
        } catch (Exception e) {
            log.error("从 API获取基金{}的数据失败", fundCode, e);
            return null;
        }
    }

    /**
     * 从返回字符串中提取JSON部分
     * 输入：jsonpgz({"fundcode":"025209",...});
     * 输出：{"fundcode":"025209",...}
     */
    private String extractJson(String response) {
        try {
            // 找到第一个 { 和最后一个 }
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');

            if (start != -1 && end != -1 && end > start) {
                return response.substring(start, end + 1);
            }

            return null;
        } catch (Exception e) {
            log.error("提取JSON失败", e);
            return null;
        }
    }
}
