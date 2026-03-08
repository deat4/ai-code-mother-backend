package com.zkf.aicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zkf.aicodemother.annotation.AuthCheck;
import com.zkf.aicodemother.common.BaseResponse;
import com.zkf.aicodemother.common.ResultUtils;
import com.zkf.aicodemother.constant.UserConstant;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionQueryRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionRollbackRequest;
import com.zkf.aicodemother.model.entity.App;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.vo.AppVersionDetailVO;
import com.zkf.aicodemother.model.vo.AppVersionVO;
import com.zkf.aicodemother.model.vo.VersionDiffVO;
import com.zkf.aicodemother.service.AppService;
import com.zkf.aicodemother.service.AppVersionService;
import com.zkf.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 应用版本控制器
 */
@Slf4j
@RestController
@RequestMapping("/app/version")
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    // region 内部辅助方法

    /**
     * 统一权限校验：检查当前用户是否有权限操作该应用
     *
     * @param appId       应用ID
     * @param request     HTTP请求
     * @param allowAdmin  是否允许管理员操作
     * @return 当前登录用户
     */
    private User checkAppAuth(Long appId, HttpServletRequest request, boolean allowAdmin) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        boolean isOwner = app.getUserId().equals(loginUser.getId());
        boolean hasAdminAccess = allowAdmin && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());

        ThrowUtils.throwIf(!isOwner && !hasAdminAccess, ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        return loginUser;
    }

    // endregion

    // region 用户接口

    @PostMapping("/list/page")
    public BaseResponse<Page<AppVersionVO>> listVersionsPage(
            @RequestBody AppVersionQueryRequest queryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数不能为空");

        // 校验权限（允许管理员查看）
        checkAppAuth(queryRequest.getAppId(), request, true);

        Page<AppVersionVO> result = appVersionService.listVersionsPage(queryRequest);
        return ResultUtils.success(result);
    }

    @GetMapping("/detail") // 优化了路径
    public BaseResponse<AppVersionDetailVO> getVersionDetail(
            @RequestParam Long versionId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID无效");

        // 建议在 service 层实现一个快速获取 appId 的方法，先校验权限再查详情
        // Long appId = appVersionService.getAppIdByVersionId(versionId);
        // checkAppAuth(appId, request, true);

        AppVersionDetailVO detail = appVersionService.getVersionDetail(versionId);
        checkAppAuth(detail.getAppId(), request, true);

        return ResultUtils.success(detail);
    }

    @PostMapping("/create")
    public BaseResponse<Long> createVersion(
            @RequestBody AppVersionAddRequest addRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 校验权限（只有本人能创建）
        User loginUser = checkAppAuth(addRequest.getAppId(), request, false);

        Long versionId = appVersionService.createVersion(addRequest, loginUser.getId());
        return ResultUtils.success(versionId);
    }

    @GetMapping("/diff")
    public BaseResponse<VersionDiffVO> diffVersions(
            @RequestParam Long appId,
            @RequestParam Integer oldVersion,
            @RequestParam Integer newVersion,
            HttpServletRequest request) {
        ThrowUtils.throwIf(oldVersion == null || oldVersion <= 0, ErrorCode.PARAMS_ERROR, "旧版本号无效");
        ThrowUtils.throwIf(newVersion == null || newVersion <= 0, ErrorCode.PARAMS_ERROR, "新版本号无效");

        // 校验权限
        checkAppAuth(appId, request, true);

        VersionDiffVO result = appVersionService.diffVersions(appId, oldVersion, newVersion);
        return ResultUtils.success(result);
    }

    @PostMapping("/rollback")
    public BaseResponse<Long> rollbackToVersion(
            @RequestBody AppVersionRollbackRequest rollbackRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(rollbackRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");

        // 校验权限（只有本人能回退）
        User loginUser = checkAppAuth(rollbackRequest.getAppId(), request, false);

        Long versionId = appVersionService.rollbackToVersion(rollbackRequest, loginUser.getId());
        return ResultUtils.success(versionId);
    }

    @GetMapping("/current/content")
    public BaseResponse<String> getCurrentVersionContent(
            @RequestParam Long appId,
            HttpServletRequest request) {

        // 校验权限
        checkAppAuth(appId, request, true);

        String content = appVersionService.getCurrentVersionContent(appId);
        return ResultUtils.success(content);
    }

    // endregion

    // region 管理员接口

    @DeleteMapping("/admin/{versionId}") // 优化了路径
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteVersion(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID无效");

        // 注意：建议在 removeById 之前或在 Service 层内部清理关联的物理文件
        boolean success = appVersionService.removeById(versionId);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");

        return ResultUtils.success(true);
    }

    // endregion
}