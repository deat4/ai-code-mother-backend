package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 校验问题类型枚举
 */
@Getter
public enum ValidationIssueTypeEnum {

    STRUCTURE("结构问题", "structure"),
    CONTENT("内容问题", "content"),
    BUILD("构建问题", "build"),
    CONFIG("配置问题", "config");

    private final String text;

    private final String value;

    ValidationIssueTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ValidationIssueTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ValidationIssueTypeEnum type : ValidationIssueTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}