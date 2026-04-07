package com.zkf.aicodemother.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置类
 * 只有在配置了 cos.client.secret-id 和 secret-key 时才会启用
 *
 * @author yupi
 */
@Configuration
@Data
public class CosClientConfig {

    @Value("${cos.client.secret-id:}")
    private String secretId;

    @Value("${cos.client.secret-key:}")
    private String secretKey;

    @Value("${cos.client.region:}")
    private String region;

    @Value("${cos.client.bucket:}")
    private String bucket;

    @Value("${cos.client.host:}")
    private String host;

    @Bean
    @ConditionalOnProperty(prefix = "cos.client", name = {"secret-id", "secret-key", "region"})
    public COSClient cosClient() {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        com.qcloud.cos.ClientConfig clientConfig = new com.qcloud.cos.ClientConfig(new Region(region));
        // 设置使用https协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}