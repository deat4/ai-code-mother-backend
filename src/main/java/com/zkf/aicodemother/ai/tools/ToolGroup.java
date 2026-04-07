package com.zkf.aicodemother.ai.tools;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 工具分组管理器
 * 为不同场景提供不同的工具集合
 */
@Component
public class ToolGroup {

    /**
     * 用于创建场景的工具集合（仅包含写入工具）
     */
    @Resource
    @Qualifier("creationTools")
    private BaseTool[] creationTools;

    /**
     * 用于修改场景的工具集合（包含完整的读/写/改/删工具）
     */
    @Resource
    @Qualifier("modificationTools")
    private BaseTool[] modificationTools;

    public BaseTool[] getCreationTools() {
        return creationTools;
    }

    public BaseTool[] getModificationTools() {
        return modificationTools;
    }
}