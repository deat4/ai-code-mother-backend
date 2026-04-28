package com.zkf.aicodemother.core.validation;

import java.nio.file.Path;

/**
 * 构建执行器接口
 * 把 VueProjectBuilder 里原本只是"直接跑构建"的逻辑，提炼成可返回结构化结果的构建执行器
 */
public interface BuildExecutor {

    /**
     * 执行构建
     *
     * @param projectDir 项目目录
     * @return 构建结果
     */
    BuildValidationResult executeBuild(Path projectDir);

    /**
     * 执行构建（带项目路径字符串）
     *
     * @param projectPath 项目路径
     * @return 构建结果
     */
    BuildValidationResult executeBuild(String projectPath);
}