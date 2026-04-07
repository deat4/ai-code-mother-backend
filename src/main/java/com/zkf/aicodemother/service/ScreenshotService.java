package com.zkf.aicodemother.service;

/**
 * 网页截图服务接口
 */
public interface ScreenshotService {

    /**
     * 生成网页截图并上传到对象存储
     *
     * @param webUrl 网页URL
     * @return 对象存储访问URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}