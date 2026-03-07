package com.zkf.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.config.AppConfig;
import com.zkf.aicodemother.core.CodeGenTypeEnum;
import com.zkf.aicodemother.core.GenerationSessionManager;
import com.zkf.aicodemother.core.GenerationSessionManager;

import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.mapper.AppMapper;
import com.zkf.aicodemother.model.dto.app.AppAddRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateRequest;
import com.zkf.aicodemother.model.entity.App;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.vo.AppVO;
import com.zkf.aicodemother.model.vo.UserVO;
import com.zkf.aicodemother.service.AppService;
import com.zkf.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现
 */
@Slf4j
@Service
public class AppServiceImpl implements AppService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private UserService userService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private GenerationSessionManager sessionManager;


    @Override
    public Page<App> page(Page<App> page, QueryWrapper queryWrapper) {
        return appMapper.paginate(page, queryWrapper);
    }

    @Override
    public Long addApp(AppAddRequest appAddRequest, Long userId) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String appName = appAddRequest.getAppName();
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(userId);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        app.setEditTime(LocalDateTime.now());
        app.setIsDelete(0);
        // 设置默认封面
        if (StrUtil.isBlank(app.getCover())) {
            app.setCover("https://picsum.photos/800/600?random=1");
        }

        // 设置默认代码生成类型
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isBlank(codeGenType)) {
            app.setCodeGenType(CodeGenTypeEnum.HTML.getValue());
        } else {
            // 校验 codeGenType 是否有效
            CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
            ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, 
                "不支持的代码生成类型: " + codeGenType + "，仅支持 HTML 和 MULTI_FILE");
            // 统一转换为大写格式
            app.setCodeGenType(codeGenTypeEnum.getValue());
        }
        if (app.getPriority() == null) {
            app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        }
        
        boolean result = appMapper.insert(app) > 0;
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建应用失败");
        return app.getId();
    }

    @Override
    public boolean updateApp(AppUpdateRequest appUpdateRequest, Long userId) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        App existingApp = appMapper.selectOneById(appUpdateRequest.getId());
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!existingApp.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权限修改此应用");
        
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        app.setCover(appUpdateRequest.getCover());
        app.setInitPrompt(appUpdateRequest.getInitPrompt());
        app.setCodeGenType(appUpdateRequest.getCodeGenType());
        app.setPriority(appUpdateRequest.getPriority());
        app.setEditTime(LocalDateTime.now());
        
        return appMapper.update(app) > 0;
    }

    @Override
    public boolean updateAppByAdmin(AppUpdateAdminRequest appUpdateAdminRequest) {
        ThrowUtils.throwIf(appUpdateAdminRequest == null || appUpdateAdminRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        App existingApp = appMapper.selectOneById(appUpdateAdminRequest.getId());
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        App app = new App();
        app.setId(appUpdateAdminRequest.getId());
        app.setAppName(appUpdateAdminRequest.getAppName());
        app.setCover(appUpdateAdminRequest.getCover());
        app.setPriority(appUpdateAdminRequest.getPriority());
        app.setEditTime(LocalDateTime.now());
        
        return appMapper.update(app) > 0;
    }

    @Override
    public boolean deleteApp(Long id, Long userId) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        
        App existingApp = appMapper.selectOneById(id);
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!existingApp.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权限删除此应用");
        
        // 清理关联文件
        cleanAppFiles(existingApp);
        
        return appMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteAppByAdmin(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        
        App existingApp = appMapper.selectOneById(id);
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 清理关联文件
        cleanAppFiles(existingApp);
        
        return appMapper.deleteById(id) > 0;
    }

    /**
     * 清理应用关联的文件
     * @param app 应用实体
     */
    private void cleanAppFiles(App app) {
        if (app == null || app.getId() == null) {
            return;
        }
        
        // 1. 清理预览目录: tmp/code_output/{codeGenType}_{appId}/
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isNotBlank(codeGenType)) {
            String previewDir = StrUtil.format("{}{}{}_{}", 
                AppConstant.CODE_OUTPUT_ROOT_DIR, 
                File.separator, 
                codeGenType, 
                app.getId());
            if (FileUtil.exist(previewDir)) {
                FileUtil.del(previewDir);
                log.info("已清理预览目录: {}", previewDir);
            }
        }
        
        // 2. 清理部署目录: tmp/code_deploy/{deployKey}/
        String deployKey = app.getDeployKey();
        if (StrUtil.isNotBlank(deployKey)) {
            String deployDir = StrUtil.format("{}{}{}", 
                AppConstant.CODE_DEPLOY_ROOT_DIR, 
                File.separator, 
                deployKey);
            if (FileUtil.exist(deployDir)) {
                FileUtil.del(deployDir);
                log.info("已清理部署目录: {}", deployDir);
            }
        }
    }

    @Override
    public App getById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return appMapper.selectOneById(id);
    }

    @Override
    public AppVO getAppVO(Long id) {
        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        return getAppVO(app);
    }

    @Override
    public Page<AppVO> listMyAppByPage(AppQueryRequest appQueryRequest, Long userId) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        int current = appQueryRequest.getCurrent();
        int pageSize = Math.min(appQueryRequest.getPageSize(), 20);
        
        // 设置当前用户 id，只查询当前用户的应用
        appQueryRequest.setUserId(userId);
        
        // 使用统一的查询条件构建方法
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        
        Page<App> appPage = appMapper.paginate(Page.of(current, pageSize), queryWrapper);
        
        // 数据封装，使用批量查询避免 N+1 问题
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return appVOPage;
    }

    @Override
    public Page<App> listAppByPageForAdmin(AppQueryAdminRequest appQueryAdminRequest) {
        ThrowUtils.throwIf(appQueryAdminRequest == null, ErrorCode.PARAMS_ERROR);
        
        int current = appQueryAdminRequest.getCurrent();
        int pageSize = appQueryAdminRequest.getPageSize();
        
        QueryWrapper queryWrapper = getQueryWrapper(appQueryAdminRequest);
        queryWrapper.orderBy("createTime", false);
        
        return appMapper.paginate(Page.of(current, pageSize), queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryAdminRequest appQueryAdminRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("id", appQueryAdminRequest.getId(), appQueryAdminRequest.getId() != null);
        queryWrapper.like("appName", appQueryAdminRequest.getAppName(), StrUtil.isNotBlank(appQueryAdminRequest.getAppName()));
        queryWrapper.eq("codeGenType", appQueryAdminRequest.getCodeGenType(), StrUtil.isNotBlank(appQueryAdminRequest.getCodeGenType()));
        queryWrapper.eq("deployKey", appQueryAdminRequest.getDeployKey(), StrUtil.isNotBlank(appQueryAdminRequest.getDeployKey()));
        queryWrapper.eq("priority", appQueryAdminRequest.getPriority(), appQueryAdminRequest.getPriority() != null);
        queryWrapper.eq("userId", appQueryAdminRequest.getUserId(), appQueryAdminRequest.getUserId() != null);
        return queryWrapper;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id, id != null)
                .like("appName", appName, StrUtil.isNotBlank(appName))
                .like("cover", cover, StrUtil.isNotBlank(cover))
                .like("initPrompt", initPrompt, StrUtil.isNotBlank(initPrompt))
                .eq("codeGenType", codeGenType, StrUtil.isNotBlank(codeGenType))
                .eq("deployKey", deployKey, StrUtil.isNotBlank(deployKey))
                .eq("priority", priority, priority != null)
                .eq("userId", userId, userId != null);
        
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }
        
        return queryWrapper;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> userService.getUserVO(user)));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<AppVO> listFeaturedAppByPage(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        int current = appQueryRequest.getCurrent();
        int pageSize = Math.min(appQueryRequest.getPageSize(), 20);
        
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        
        // 使用统一的查询条件构建方法
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        
        Page<App> appPage = appMapper.paginate(Page.of(current, pageSize), queryWrapper);
        
        // 数据封装，使用批量查询避免 N+1 问题
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return appVOPage;
    }

    @Override
    public boolean updateById(App app) {
        return appMapper.update(app) > 0;
    }

    @Resource
    private com.zkf.aicodemother.core.AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public reactor.core.publisher.Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        com.zkf.aicodemother.core.CodeGenTypeEnum codeGenTypeEnum = 
            com.zkf.aicodemother.core.CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            // 如果 codeGenType 无效，默认使用 HTML
            codeGenTypeEnum = com.zkf.aicodemother.core.CodeGenTypeEnum.HTML;
            log.warn("应用 {} 的代码生成类型无效: {}，使用默认值 HTML", appId, codeGenTypeStr);
        }
        
        // 5. 调用 AI 生成代码（流式）
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = cn.hutool.core.util.RandomUtil.randomString(6);
        }
        
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = com.zkf.aicodemother.constant.AppConstant.CODE_OUTPUT_ROOT_DIR + java.io.File.separator + sourceDirName;
        
        // 6. 检查源目录是否存在
        java.io.File sourceDir = new java.io.File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        
        // 7. 复制文件到部署目录
        String deployDirPath = com.zkf.aicodemother.constant.AppConstant.CODE_DEPLOY_ROOT_DIR + java.io.File.separator + deployKey;
        try {
            cn.hutool.core.io.FileUtil.copyContent(sourceDir, new java.io.File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(java.time.LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", appConfig.getDeploy().getHost(), deployKey);
    }

    @Override
    public boolean stopGeneration(String sessionId) {
        return sessionManager.cancelSession(sessionId);
    }

}
