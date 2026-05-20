package com.fund.tools.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
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
import java.math.BigDecimal;
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
        wrapper.orderByDesc(PlateFlow::getCreateTime)
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

            // 使用HttpRequest添加必要的请求头
            String response = cn.hutool.http.HttpUtil.createGet(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Referer", "https://quote.eastmoney.com/")
                .timeout(ApiConstants.API_TIMEOUT_MS)
                .execute()
                .body();

            log.info("API原始响应长度: {}", response != null ? response.length() : 0);
            log.debug("API原始响应前500字符: {}", response != null ? response.substring(0, Math.min(500, response.length())) : "null");

            if (response == null || response.trim().isEmpty()) {
                log.warn("板块资金流向API返回空响应");
                return false;
            }

            // 手动解析JSON，避免科学计数法问题
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            Integer rc = jsonResponse.getInt("rc");
            
            if (rc == null || rc != 0) {
                log.warn("板块资金流向API返回错误码: {}", rc);
                return false;
            }
            
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                log.warn("板块资金流向API返回数据为空");
                return false;
            }
            
            JSONArray diffArray = data.getJSONArray("diff");
            if (diffArray == null || diffArray.isEmpty()) {
                log.info("板块资金流向API返回数据列表为空");
                return false;
            }
            
            log.info("获取到{}个板块的资金流向数据", diffArray.size());
            
            // 打印第一个item的详细信息用于调试
            if (!diffArray.isEmpty()) {
                JSONObject firstItem = diffArray.getJSONObject(0);
                log.info("第一个板块原始数据: {}", firstItem.toString());
            }

            LocalDateTime now = LocalDateTime.now();

            // 清空整个表的数据
            int deletedCount = plateFlowMapper.delete(new LambdaQueryWrapper<>());
            if (deletedCount > 0) {
                log.info("清空了表中的{}条旧数据", deletedCount);
            }

            // 保存新数据
            int successCount = 0;
            for (int i = 0; i < diffArray.size(); i++) {
                try {
                    JSONObject item = diffArray.getJSONObject(i);
                    
                    PlateFlow plateFlow = new PlateFlow();
                    plateFlow.setPlateName(item.getStr("f14"));
                    
                    // 直接从JSONObject获取数值，Hutool会自动处理大数
                    Object f62 = item.get("f62");
                    Object f63 = item.get("f63");
                    Object f64 = item.get("f64");
                    
                    log.debug("板块{}的原始数值: f62={}, f63={}, f64={}", 
                        item.getStr("f14"), f62, f63, f64);
                    
                    // 转换为BigDecimal
                    plateFlow.setMainFlow(f62 != null ? new BigDecimal(f62.toString()) : null);
                    plateFlow.setMainDealAmount(f63 != null ? new BigDecimal(f63.toString()) : null);
                    plateFlow.setMainFlowAmount(f64 != null ? new BigDecimal(f64.toString()) : null);
                    
                    plateFlow.setCreateTime(now);

                    plateFlowMapper.insert(plateFlow);
                    successCount++;

                    log.debug("成功保存板块{}的资金流向数据", item.getStr("f14"));
                } catch (Exception e) {
                    log.error("保存板块资金流向数据失败", e);
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
