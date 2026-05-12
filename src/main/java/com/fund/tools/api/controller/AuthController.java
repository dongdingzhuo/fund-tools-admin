package com.fund.tools.api.controller;

import cn.hutool.core.util.StrUtil;
import com.fund.tools.api.common.Result;
import com.fund.tools.api.dto.LoginRequest;
import com.fund.tools.api.dto.LoginResponse;
import com.fund.tools.api.entity.User;
import com.fund.tools.api.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     * 只需要用户名存在即验证通过
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 参数校验
        if (request == null || StrUtil.isBlank(request.getUserName())) {
            return Result.error("用户名不能为空");
        }

        // 查询用户
        User user = userService.findByUserName(request.getUserName());
        
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证通过，返回用户信息
        LoginResponse response = new LoginResponse(
            user.getId(),
            user.getUserName(),
            user.getNickName()
        );

        return Result.success("登录成功", response);
    }
}
