package com.zkf.aicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具工厂类
 * 用于根据工具名称创建和管理工具实例
 * 支持生成工具执行结果格式
 * 
 * 使用 @Autowired 自动装配所有工具 Bean
 */
@Component
public class ToolFactory {

    // 使用自动注入来获得所有工具实例
    @Autowired(required = false)
    private Map<String, BaseTool> toolBeans = new HashMap<>();

    private final Map<String, BaseTool> toolMap = new HashMap<>();

    @PostConstruct
    public void initializeTools() {
        // 复制所有自动注入的工具 Bean
        toolMap.putAll(toolBeans);
    }

    /**
     * 注册工具实例
     */
    public void registerTool(BaseTool tool) {
        toolMap.put(tool.getToolName(), tool);
    }

    /**
     * 根据工具名称获取工具实例
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 根据工具名称生成工具执行结果
     */
    public String generateToolExecutedResult(String toolName, JSONObject arguments) {
        BaseTool tool = getTool(toolName);
        if (tool == null) {
            return String.format("[未知工具] %s", toolName);
        }
        return tool.generateToolExecutedResult(arguments);
    }

    /**
     * 获取所有已注册的工具名称
     */
    public String[] getRegisteredToolNames() {
        return toolMap.keySet().toArray(new String[0]);
    }
}