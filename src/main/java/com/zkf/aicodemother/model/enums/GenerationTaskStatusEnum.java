package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum GenerationTaskStatusEnum {

    PENDING("待执行", "pending"),
    RUNNING("执行中", "running"),
    SUCCESS("成功", "success"),
    FAILED("失败", "failed"),
    CANCELED("已取消", "canceled");

    private final String text;

    private final String value;

    GenerationTaskStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static GenerationTaskStatusEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (GenerationTaskStatusEnum status : GenerationTaskStatusEnum.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}