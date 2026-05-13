package com.fund.tools.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.tools.api.common.Result;
import com.fund.tools.api.dto.FundSelfRequest;
import com.fund.tools.api.dto.FundSelfResponse;
import com.fund.tools.api.service.FundSelfService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 自选基金控制器
 */
@RestController
@RequestMapping("/api/fund-self")
public class FundSelfController {

    @Resource
    private FundSelfService fundSelfService;

    /**
     * 添加自选基金
     */
    @PostMapping
    public Result<Boolean> addFundSelf(@Validated @RequestBody FundSelfRequest request) {
        try {
            boolean success = fundSelfService.addFundSelf(request);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除自选基金
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteFundSelf(@PathVariable Long id) {
        try {
            boolean success = fundSelfService.deleteFundSelf(id);
            if (success) {
                return Result.success("删除成功", true);
            } else {
                return Result.error("删除失败，记录不存在");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据用户账号查询自选基金列表
     */
    @GetMapping("/list/{userName}")
    public Result<List<FundSelfResponse>> getFundSelfByUserName(@PathVariable String userName) {
        try {
            List<FundSelfResponse> list = fundSelfService.getFundSelfByUserName(userName);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询自选基金
     */
    @GetMapping("/page")
    public Result<Page<FundSelfResponse>> pageFundSelf(@RequestParam(required = false) String userName,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Page<FundSelfResponse> page = fundSelfService.pageFundSelf(userName, pageNum, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
