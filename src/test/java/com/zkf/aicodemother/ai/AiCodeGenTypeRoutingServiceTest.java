package com.zkf.aicodemother.ai;

import com.zkf.aicodemother.config.AiCodeGenTypeRoutingServiceFactory;
import com.zkf.aicodemother.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

@Slf4j
@SpringBootTest
public class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Test
    public void testRouteCodeGenType() {
        // 每次测试创建新的路由服务实例（多例模式）
        AiCodeGenTypeRoutingService routingService = routingServiceFactory.createRoutingService();

        // 测试简单页面 -> HTML
        String userPrompt = "做一个简单的个人介绍页面";
        CodeGenTypeEnum result = routingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());

        // 测试多页面 -> MULTI_FILE
        userPrompt = "做一个公司官网，需要首页、关于我们、联系我们三个页面";
        result = routingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());

        // 测试复杂项目 -> VUE_PROJECT
        userPrompt = "做一个电商管理系统，包含用户管理、商品管理、订单管理，需要路由和状态管理";
        result = routingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result.getValue());
    }
}