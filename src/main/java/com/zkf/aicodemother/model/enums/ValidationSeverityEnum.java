package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 校验严重性枚举
 */
@Getter
public enum ValidationSeverityEnum {

    INFO("信息", "info"),
    WARN("警告", "warn"),
    ERROR("错误", "error");

    private final String text;

    private final String value;

    ValidationSeverityEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ValidationSeverityEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ValidationSeverityEnum severity : ValidationSeverityEnum.values()) {
            if (severity.value.equals(value)) {
                return severity;
            }
        }
        return null;
    }
}