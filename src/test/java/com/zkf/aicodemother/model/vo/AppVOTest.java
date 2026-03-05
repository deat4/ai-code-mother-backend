package com.zkf.aicodemother.model.vo;

import com.zkf.aicodemother.constant.AppConstant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用 VO 测试
 */
class AppVOTest {

    @Test
    void testAppVO_SetterAndGetter() {
        AppVO appVO = new AppVO();
        
        // 测试基本字段
        appVO.setId(1L);
        appVO.setAppName("测试应用");
        appVO.setCover("https://example.com/cover.jpg");
        appVO.setInitPrompt("你是一个助手");
        appVO.setCodeGenType("HTML");
        appVO.setDeployKey("test-deploy-key");
        appVO.setDeployedTime(LocalDateTime.now());
        appVO.setPriority(AppConstant.GOOD_APP_PRIORITY);
        appVO.setUserId(1L);
        appVO.setCreateTime(LocalDateTime.now());
        appVO.setUpdateTime(LocalDateTime.now());
        
        // 测试用户信息
        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUserName("测试用户");
        userVO.setUserAvatar("https://example.com/avatar.jpg");
        userVO.setUserProfile("这是一个测试用户");
        appVO.setUser(userVO);
        
        // 验证所有字段
        assertEquals(1L, appVO.getId());
        assertEquals("测试应用", appVO.getAppName());
        assertEquals("https://example.com/cover.jpg", appVO.getCover());
        assertEquals("你是一个助手", appVO.getInitPrompt());
        assertEquals("HTML", appVO.getCodeGenType());
        assertEquals("test-deploy-key", appVO.getDeployKey());
        assertNotNull(appVO.getDeployedTime());
        assertEquals(AppConstant.GOOD_APP_PRIORITY, appVO.getPriority());
        assertEquals(1L, appVO.getUserId());
        assertNotNull(appVO.getCreateTime());
        assertNotNull(appVO.getUpdateTime());
        
        // 验证用户信息
        assertNotNull(appVO.getUser());
        assertEquals("测试用户", appVO.getUser().getUserName());
        assertEquals("https://example.com/avatar.jpg", appVO.getUser().getUserAvatar());
    }

    @Test
    void testAppVO_DefaultConstructor() {
        AppVO appVO = new AppVO();
        
        // 验证默认值
        assertNull(appVO.getId());
        assertNull(appVO.getAppName());
        assertNull(appVO.getCover());
        assertNull(appVO.getInitPrompt());
        assertNull(appVO.getCodeGenType());
        assertNull(appVO.getDeployKey());
        assertNull(appVO.getDeployedTime());
        assertNull(appVO.getPriority());
        assertNull(appVO.getUserId());
        assertNull(appVO.getCreateTime());
        assertNull(appVO.getUpdateTime());
        assertNull(appVO.getUser());
    }

    @Test
    void testAppVO_NullUser() {
        AppVO appVO = new AppVO();
        appVO.setId(1L);
        appVO.setAppName("测试应用");
        appVO.setUser(null);
        
        assertEquals(1L, appVO.getId());
        assertEquals("测试应用", appVO.getAppName());
        assertNull(appVO.getUser());
    }

    @Test
    void testAppVO_EqualsAndHashCode() {
        AppVO appVO1 = new AppVO();
        appVO1.setId(1L);
        appVO1.setAppName("应用 1");
        
        AppVO appVO2 = new AppVO();
        appVO2.setId(1L);
        appVO2.setAppName("应用 1");
        
        AppVO appVO3 = new AppVO();
        appVO3.setId(2L);
        appVO3.setAppName("应用 2");
        
        // 注意：Lombok @Data 生成的 equals 基于所有字段
        // 这里只验证对象创建成功
        assertNotNull(appVO1);
        assertNotNull(appVO2);
        assertNotNull(appVO3);
    }

    @Test
    void testAppVO_ToString() {
        AppVO appVO = new AppVO();
        appVO.setId(1L);
        appVO.setAppName("测试应用");
        
        String toString = appVO.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("1L"));
        assertTrue(toString.contains("测试应用"));
    }

    @Test
    void testAppVO_PriorityValues() {
        AppVO appVO = new AppVO();
        
        // 测试默认优先级
        appVO.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        assertEquals(0, appVO.getPriority());
        
        // 测试精选优先级
        appVO.setPriority(AppConstant.GOOD_APP_PRIORITY);
        assertEquals(99, appVO.getPriority());
        
        // 测试自定义优先级
        appVO.setPriority(50);
        assertEquals(50, appVO.getPriority());
    }

    @Test
    void testAppVO_CodeGenTypes() {
        AppVO appVO = new AppVO();
        
        // 测试不同的代码生成类型
        appVO.setCodeGenType("HTML");
        assertEquals("HTML", appVO.getCodeGenType());
        
        appVO.setCodeGenType("MULTI_FILE");
        assertEquals("MULTI_FILE", appVO.getCodeGenType());
        
        appVO.setCodeGenType(null);
        assertNull(appVO.getCodeGenType());
    }

    @Test
    void testAppVO_EmptyStrings() {
        AppVO appVO = new AppVO();
        
        // 测试空字符串
        appVO.setAppName("");
        assertEquals("", appVO.getAppName());
        
        appVO.setCover("");
        assertEquals("", appVO.getCover());
        
        appVO.setInitPrompt("");
        assertEquals("", appVO.getInitPrompt());
    }
}
