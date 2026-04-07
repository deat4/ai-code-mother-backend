package com.zkf.aicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载服务接口
 */
public interface ProjectDownloadService {

    /**
     * 下载项目代码压缩包
     *
     * @param projectPath 项目路径
     * @param fileName    下载文件名（不含扩展名）
     * @param response    HTTP响应
     */
    void downloadProject(String projectPath, String fileName, HttpServletResponse response);

    /**
     * 检查项目路径是否存在且可下载
     *
     * @param projectPath 项目路径
     * @return 是否可下载
     */
    boolean isProjectDownloadable(String projectPath);
}