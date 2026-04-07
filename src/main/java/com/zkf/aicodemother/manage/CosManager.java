package com.zkf.aicodemother.manage;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.zkf.aicodemother.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS对象存储管理器
 * 只有在COS客户端配置完成后才会创建
 *
 * @author yupi
 */
@Component
@Slf4j
public class CosManager {

    @Autowired(required = false)
    private CosClientConfig cosClientConfig;

    @Autowired(required = false)
    private COSClient cosClient;

    /**
     * 检查COS是否已配置
     */
    public boolean isConfigured() {
        return cosClient != null && cosClientConfig != null;
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        if (!isConfigured()) {
            throw new IllegalStateException("COS未配置");
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        if (!isConfigured()) {
            log.warn("COS未配置，无法上传文件");
            return null;
        }
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }

    /**
     * 删除 COS 对象
     *
     * @param key COS对象键
     * @return 是否删除成功
     */
    public boolean deleteObject(String key) {
        if (!isConfigured()) {
            log.warn("COS未配置，无法删除对象");
            return false;
        }
        if (StrUtil.isBlank(key)) {
            log.warn("删除对象失败：key为空");
            return false;
        }
        try {
            DeleteObjectRequest deleteRequest = new DeleteObjectRequest(cosClientConfig.getBucket(), key);
            cosClient.deleteObject(deleteRequest);
            log.info("删除COS对象成功: {}", key);
            return true;
        } catch (Exception e) {
            log.error("删除COS对象失败: {}", key, e);
            return false;
        }
    }

    /**
     * 根据URL删除COS对象
     * 自动从URL中提取对象键
     *
     * @param url COS对象访问URL
     * @return 是否删除成功
     */
    public boolean deleteObjectByUrl(String url) {
        if (!isConfigured()) {
            log.warn("COS未配置，无法删除对象");
            return false;
        }
        if (StrUtil.isBlank(url)) {
            log.warn("删除对象失败：URL为空");
            return false;
        }
        try {
            // 从URL中提取对象键
            // URL格式: https://bucket.cos.region.myqcloud.com/screenshots/2025/01/01/xxx.jpg
            String host = cosClientConfig.getHost();
            if (!url.startsWith(host)) {
                log.warn("URL不是当前COS配置的地址: {}", url);
                return false;
            }
            String key = url.substring(host.length());
            return deleteObject(key);
        } catch (Exception e) {
            log.error("根据URL删除COS对象失败: {}", url, e);
            return false;
        }
    }
}