package com.fund.tools.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户请求DTO
 */
@Data
public class CreateUserRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 昵称
     */
    private String nickName;
}
