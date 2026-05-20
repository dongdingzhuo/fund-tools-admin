package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.tools.api.config.ApiConstants;
import com.fund.tools.api.dto.EastmoneyPlateFlowResponse;
import com.fund.tools.api.dto.PlateFlowResponse;
import com.fund.tools.api.entity.PlateFlow;
import com.fund.tools.api.mapper.PlateFlowMapper;
import com.fund.tools.api.service.PlateFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 板块资金流向服务实现类
 */
@Slf4j
@Service
public class PlateFlowServiceImpl implements PlateFlowService {

    @Resource
    private PlateFlowMapper plateFlowMapper;

    @Override
    public List<PlateFlowResponse> getPlateFlowListFromDb() {
        // 直接从数据库查询最新数据
        LambdaQueryWrapper<PlateFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PlateFlow::getDataTime)
                .orderByAsc(PlateFlow::getId);

        List<PlateFlow> plateFlowList = plateFlowMapper.selectList(wrapper);

        return plateFlowList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean fetchAndSavePlateFlow() {
        try {
            String url = "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=10&po=1&np=1&fltt=2&invt=2&fid=f62&fs=m:90+t:3&fields=f14,f62,f63,f64,f65";

            log.debug("请求板块资金流向URL: {}", url);

            String response = HttpUtil.get(url, ApiConstants.API_TIMEOUT_MS);

            if (response == null || response.trim().isEmpty()) {
                log.warn("板块资金流向API返回空响应");
                return false;
            }

            EastmoneyPlateFlowResponse apiResponse = JSONUtil.toBean(response, EastmoneyPlateFlowResponse.class);

            if (apiResponse == null || apiResponse.getRc() != 0 || apiResponse.getData() == null) {
                log.warn("板块资金流向API返回数据格式错误或请求失败");
                return false;
            }

            List<EastmoneyPlateFlowResponse.PlateFlowItem> items = apiResponse.getData().getDiff();

            if (items == null || items.isEmpty()) {
                log.info("板块资金流向API返回数据为空");
                return false;
            }

            log.info("获取到{}个板块的资金流向数据", items.size());

            LocalDateTime now = LocalDateTime.now();

            // 清空整个表的数据
            int deletedCount = plateFlowMapper.delete(new LambdaQueryWrapper<>());
            if (deletedCount > 0) {
                log.info("清空了表中的{}条旧数据", deletedCount);
            }

            // 保存新数据
            int successCount = 0;
            for (EastmoneyPlateFlowResponse.PlateFlowItem item : items) {
                try {
                    PlateFlow plateFlow = new PlateFlow();
                    plateFlow.setPlateName(item.getPlateName());
                    plateFlow.setMainFlow(item.getMainFlow());
                    plateFlow.setMainDealAmount(item.getMainDealAmount());
                    plateFlow.setMainFlowAmount(item.getMainFlowAmount());
                    plateFlow.setMainFlowRate(item.getMainFlowRate());
                    plateFlow.setDataTime(now);
                    plateFlow.setCreateTime(now);

                    plateFlowMapper.insert(plateFlow);
                    successCount++;

                    log.debug("成功保存板块{}的资金流向数据", item.getPlateName());
                } catch (Exception e) {
                    log.error("保存板块{}的资金流向数据失败", item.getPlateName(), e);
                }
            }

            log.info("板块资金流向数据保存完成 - 成功: {}", successCount);
            return successCount > 0;

        } catch (Exception e) {
            log.error("获取板块资金流向数据失败", e);
            return false;
        }
    }

    /**
     * 转换为响应对象
     */
    private PlateFlowResponse convertToResponse(PlateFlow plateFlow) {
        PlateFlowResponse response = new PlateFlowResponse();
        BeanUtil.copyProperties(plateFlow, response);
        return response;
    }
}
