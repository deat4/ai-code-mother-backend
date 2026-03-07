package com.zkf.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkf.aicodemother.annotation.AuthCheck;
import com.zkf.aicodemother.common.BaseResponse;
import com.zkf.aicodemother.common.DeleteRequest;
import com.zkf.aicodemother.common.ResultUtils;
import com.zkf.aicodemother.constant.AppConstant;
import com.zkf.aicodemother.constant.UserConstant;
import com.zkf.aicodemother.exception.BusinessException;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.model.dto.app.AppAddRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateRequest;
import com.zkf.aicodemother.model.entity.App;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.vo.AppVO;
import com.zkf.aicodemother.service.AppService;
import com.zkf.aicodemother.service.UserService;
import com.zkf.aicodemother.core.GenerationSessionManager;
import lombok.extern.slf4j.Slf4j;

import lombok.extern.slf4j.Slf4j;


import java.util.Map;
import lombok.extern.slf4j.Slf4j;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * 应用 控制层
 */
@Slf4j

@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private GenerationSessionManager sessionManager;


    @Resource
    private GenerationSessionManager sessionManager;


    @Resource
    private GenerationSessionManager sessionManager;


    // region 用户接口

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param request       请求对象
     * @return 应用id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long appId = appService.addApp(appAddRequest, loginUser.getId());
        return ResultUtils.success(appId);
    }

    /**
     * 修改应用（仅修改名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = appService.updateApp(appUpdateRequest, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 删除应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // Check if user is admin OR app owner
        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isOwner = loginUser.getId().equals(app.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isOwner, ErrorCode.NO_AUTH_ERROR, "无权限删除此应用");
        
        // Admin uses admin delete method, owner uses user delete method
        boolean result = isAdmin ? appService.deleteAppByAdmin(deleteRequest.getId()) 
                                 : appService.deleteApp(deleteRequest.getId(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情（包含用户信息）
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用（包含用户信息）
     *
     * @param appQueryRequest 查询请求
     * @param request         请求对象
     * @return 分页结果
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getCurrent();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询自己的应用
     *
     * @param appQueryRequest 查询请求
     * @param request         请求对象
     * @return 分页结果
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<AppVO> appVOPage = appService.listMyAppByPage(appQueryRequest, loginUser.getId());
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用（包含用户信息）
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getCurrent();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    // endregion

    // region 管理员接口

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.deleteAppByAdmin(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppUpdateAdminRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getCurrent();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }

    // endregion

    // region AI 生成接口

    /**
     * 应用聊天生成代码（流式 SSE）
     * 支持 session 模式，可通过 stopGeneration 接口中断
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流（首个事件为 sessionId）
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(
            @RequestParam Long appId,
            @RequestParam String message,
            HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 创建会话 ID
        String sessionId = sessionManager.createSession();
        
        // 调用服务生成代码（流式）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        
        // 构建带会话管理的 SSE 流
        return Flux.push(sink -> {
                    // 发送 sessionId 作为第一个事件
                    Map<String, String> sessionWrapper = Map.of("sessionId", sessionId);
                    String sessionJson = JSONUtil.toJsonStr(sessionWrapper);
                    sink.next(ServerSentEvent.<String>builder()
                            .event("session")
                            .data(sessionJson)
                            .build());
                    
                    // 订阅内容流
                    Disposable subscription = contentFlux
                            .map(chunk -> {
                                Map<String, String> wrapper = Map.of("d", chunk);
                                String jsonData = JSONUtil.toJsonStr(wrapper);
                                return ServerSentEvent.<String>builder()
                                        .data(jsonData)
                                        .build();
                            })
                            .doOnNext(sink::next)
                            .doOnComplete(() -> {
                                // 发送结束事件
                                sink.next(ServerSentEvent.<String>builder()
                                        .event("done")
                                        .data("")
                                        .build());
                                // 清理会话
                                sessionManager.removeSession(sessionId);
                            })
                            .doOnError(e -> {
                                log.error("生成代码出错: {}", e.getMessage());
                                sessionManager.removeSession(sessionId);
                            })
                            .doOnCancel(() -> {
                                log.info("生成被取消: {}", sessionId);
                                sessionManager.removeSession(sessionId);
                            })
                            .subscribe();
                    
                    // 注册会话
                    sessionManager.registerSession(sessionId, subscription);
                })
                .doOnCancel(() -> sessionManager.cancelSession(sessionId));
    }

    /**
     * 停止 AI 生成
     *
     * @param sessionId 会话 ID
     * @return 操作结果
     */
    @PostMapping("/chat/stop")
    public BaseResponse<Boolean> stopGeneration(@RequestParam String sessionId) {
        ThrowUtils.throwIf(StrUtil.isBlank(sessionId), ErrorCode.PARAMS_ERROR, "会话ID不能为空");
        boolean success = appService.stopGeneration(sessionId);
        return ResultUtils.success(success);
    }

    // endregion
}