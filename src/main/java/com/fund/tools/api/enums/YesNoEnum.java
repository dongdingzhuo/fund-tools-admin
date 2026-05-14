package com.fund.tools.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是否标识枚举
 * 用于表示各种flag字段（工作日、节假日、周末、交易日等）
 */
@Getter
@AllArgsConstructor
public enum YesNoEnum {
    
    /**
     * 是/Yes
     */
    YES("Y", "是"),
    
    /**
     * 否/No
     */
    NO("N", "否");
    
    /**
     * 数据库存储值
     */
    private final String code;
    
    /**
     * 描述
     */
    private final String description;
    
    /**
     * 根据code获取枚举
     *
     * @param code 数据库存储值
     * @return 枚举对象
     */
    public static YesNoEnum fromCode(String code) {
        if (code == null) {
            return NO;
        }
        for (YesNoEnum yesNo : values()) {
            if (yesNo.getCode().equals(code)) {
                return yesNo;
            }
        }
        return NO;
    }
    
    /**
     * 判断是否为YES
     *
     * @param code 数据库存储值
     * @return 是否为YES
     */
    public static boolean isYes(String code) {
        return YES.getCode().equals(code);
    }
    
    /**
     * 判断是否为NO
     *
     * @param code 数据库存储值
     * @return 是否为NO
     */
    public static boolean isNo(String code) {
        return NO.getCode().equals(code);
    }
}
