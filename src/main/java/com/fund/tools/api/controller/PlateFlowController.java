package com.fund.tools.api.controller;

import com.fund.tools.api.common.Result;
import com.fund.tools.api.dto.PlateFlowResponse;
import com.fund.tools.api.service.PlateFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 板块资金流向控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/plate-flow")
public class PlateFlowController {

    @Resource
    private PlateFlowService plateFlowService;

    /**
     * 获取板块资金流向列表
     * 直接从数据库查询最新数据
     */
    @GetMapping("/list")
    public Result<List<PlateFlowResponse>> getPlateFlowList() {
        try {
            log.info("开始查询板块资金流向数据");
            List<PlateFlowResponse> plateFlowList = plateFlowService.getPlateFlowListFromDb();
            log.info("成功查询到{}个板块的资金流向数据", plateFlowList.size());
            return Result.success(plateFlowList);
        } catch (Exception e) {
            log.error("查询板块资金流向数据失败", e);
            return Result.error("查询板块资金流向数据失败: " + e.getMessage());
        }
    }
}
