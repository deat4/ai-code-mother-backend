package com.zkf.aicodemother.model.enums;

import lombok.Getter;

/**
 * 任务日志类型枚举
 */
@Getter
public enum GenerationTaskLogTypeEnum {

    INFO("信息", "info"),
    ERROR("错误", "error"),
    STAGE_CHANGE("阶段变更", "stage_change"),
    VALIDATION("校验日志", "validation"),
    BUILD("构建日志", "build");

    private final String text;

    private final String value;

    GenerationTaskLogTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static GenerationTaskLogTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (GenerationTaskLogTypeEnum logType : GenerationTaskLogTypeEnum.values()) {
            if (logType.value.equals(value)) {
                return logType;
            }
        }
        return null;
    }
}