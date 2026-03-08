package com.zkf.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zkf.aicodemother.core.VersionDiffUtils;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.mapper.AppVersionMapper;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionQueryRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionRollbackRequest;
import com.zkf.aicodemother.model.entity.App;
import com.zkf.aicodemother.model.entity.AppVersion;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.enums.ChangeTypeEnum;
import com.zkf.aicodemother.model.vo.AppVersionDetailVO;
import com.zkf.aicodemother.model.vo.AppVersionVO;
import com.zkf.aicodemother.model.vo.UserVO;
import com.zkf.aicodemother.model.vo.VersionDiffVO;
import com.zkf.aicodemother.mapper.AppMapper;

import com.zkf.aicodemother.service.AppVersionService;
import com.zkf.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用版本服务层实现
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Resource
    private AppVersionMapper appVersionMapper;

    @Resource
    private AppMapper appMapper;



    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(AppVersionAddRequest request, Long userId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(request.getAppId() == null || request.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "代码内容不能为空");

        // 查询应用
        App app = appMapper.selectOneById(request.getAppId());

        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 获取当前版本号
        Integer currentVersion = app.getCurrentVersion();
        if (currentVersion == null) {
            currentVersion = 0;
        }

        // 获取上一版本内容，计算差异
        String oldContent = getCurrentVersionContent(request.getAppId());
        String diffSummary = "";
        if (oldContent != null && !oldContent.isEmpty()) {
            diffSummary = VersionDiffUtils.generateDiffSummary(oldContent, request.getContent());
        }

        // 将之前的当前版本设为非当前
        QueryWrapper updateWrapper = QueryWrapper.create();
        updateWrapper.eq("app_id", request.getAppId());

        updateWrapper.eq("is_current", 1);

        AppVersion updateVersion = new AppVersion();
        updateVersion.setIsCurrent(0);
        appVersionMapper.updateByQuery(updateVersion, updateWrapper);


        // 创建新版本
        AppVersion version = new AppVersion();
        version.setAppId(request.getAppId());
        version.setVersionNumber(currentVersion + 1);
        version.setVersionName(StrUtil.isNotBlank(request.getVersionName()) 
                ? request.getVersionName() 
                : "v" + (currentVersion + 1));
        version.setContent(request.getContent());
        version.setSummary(request.getSummary());
        version.setChangeType(StrUtil.isNotBlank(request.getChangeType()) 
                ? request.getChangeType() 
                : ChangeTypeEnum.UPDATE.getValue());
        version.setDiffSummary(diffSummary);
        version.setCreatedAt(LocalDateTime.now());
        version.setCreatedBy(userId);
        version.setIsCurrent(1);
        version.setParentVersion(request.getParentVersion());

        boolean success = this.save(version);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "创建版本失败");

        // 更新应用版本信息
        app.setCurrentVersion(currentVersion + 1);
        app.setTotalVersions(currentVersion + 1);
        app.setLatestVersionTime(LocalDateTime.now());
        appMapper.update(app);


        log.info("创建版本成功: appId={}, versionId={}, versionNumber={}", 
                request.getAppId(), version.getId(), version.getVersionNumber());

        return version.getId();
    }

    @Override
    public AppVersionDetailVO getVersionDetail(Long versionId) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID无效");

        AppVersion version = this.getById(versionId);
        ThrowUtils.throwIf(version == null, ErrorCode.NOT_FOUND_ERROR, "版本不存在");

        AppVersionDetailVO detailVO = BeanUtil.copyProperties(version, AppVersionDetailVO.class);
        detailVO.setIsCurrent(version.getIsCurrent() == 1);

        // 查询创建者信息
        if (version.getCreatedBy() != null) {
            User creator = userService.getById(version.getCreatedBy());
            if (creator != null) {
                UserVO creatorVO = BeanUtil.copyProperties(creator, UserVO.class);
                detailVO.setCreator(creatorVO);
                detailVO.setCreatorName(creator.getUserName());
            }
        }

        // 判断是否可回退（当前版本不能回退）
        App app = appMapper.selectOneById(version.getAppId());

        if (app != null && app.getCurrentVersion() != null) {
            detailVO.setCanRollback(!version.getVersionNumber().equals(app.getCurrentVersion()));
        }

        // 查询前后版本号
        QueryWrapper prevWrapper = QueryWrapper.create();
        prevWrapper.eq("app_id", version.getAppId());

        prevWrapper.lt("versionNumber", version.getVersionNumber());
        prevWrapper.orderBy("versionNumber", false);
        prevWrapper.limit(1);
        AppVersion prevVersion = this.getOne(prevWrapper);
        if (prevVersion != null) {
            detailVO.setPrevVersion(prevVersion.getVersionNumber());
        }

        QueryWrapper nextWrapper = QueryWrapper.create();
        nextWrapper.eq("app_id", version.getAppId());

        nextWrapper.gt("versionNumber", version.getVersionNumber());
        nextWrapper.orderBy("versionNumber", true);
        nextWrapper.limit(1);
        AppVersion nextVersion = this.getOne(nextWrapper);
        if (nextVersion != null) {
            detailVO.setNextVersion(nextVersion.getVersionNumber());
        }

        return detailVO;
    }

    @Override
    public Page<AppVersionVO> listVersionsPage(AppVersionQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        ThrowUtils.throwIf(queryRequest.getAppId() == null || queryRequest.getAppId() <= 0, 
                ErrorCode.PARAMS_ERROR, "应用ID无效");

        // PageRequest 使用 current 字段
        int pageNum = queryRequest.getCurrent() > 0 ? queryRequest.getCurrent() : 1;
        int pageSize = queryRequest.getPageSize() > 0 ? queryRequest.getPageSize() : 10;

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("app_id", queryRequest.getAppId());


        if (queryRequest.getVersionNumber() != null) {
            queryWrapper.eq("version_number", queryRequest.getVersionNumber());

        }

        queryWrapper.orderBy("versionNumber", false);

        Page<AppVersion> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 转换为 VO：使用构造函数创建 Page
        Page<AppVersionVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(page.getRecords().stream()
                .map(version -> {
                    AppVersionVO vo = BeanUtil.copyProperties(version, AppVersionVO.class);
                    vo.setIsCurrent(version.getIsCurrent() == 1);
                    // 查询创建者姓名
                    if (version.getCreatedBy() != null) {
                        User creator = userService.getById(version.getCreatedBy());
                        if (creator != null) {
                            vo.setCreatorName(creator.getUserName());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList()));


        return voPage;
    }

    @Override
    public VersionDiffVO diffVersions(Long appId, Integer oldVersion, Integer newVersion) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(oldVersion == null || oldVersion <= 0, ErrorCode.PARAMS_ERROR, "旧版本号无效");
        ThrowUtils.throwIf(newVersion == null || newVersion <= 0, ErrorCode.PARAMS_ERROR, "新版本号无效");

        // 查询两个版本
        AppVersion oldV = getVersionByNumber(appId, oldVersion);
        AppVersion newV = getVersionByNumber(appId, newVersion);

        ThrowUtils.throwIf(oldV == null, ErrorCode.NOT_FOUND_ERROR, "旧版本不存在");
        ThrowUtils.throwIf(newV == null, ErrorCode.NOT_FOUND_ERROR, "新版本不存在");

        // 计算差异
        VersionDiffVO.DiffStats stats = VersionDiffUtils.calculateDiff(oldV.getContent(), newV.getContent());
        String diffHtml = VersionDiffUtils.generateDiffHtml(oldV.getContent(), newV.getContent());

        return new VersionDiffVO(
                oldVersion,
                newVersion,
                oldV.getContent(),
                newV.getContent(),
                diffHtml,
                stats
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long rollbackToVersion(AppVersionRollbackRequest request, Long userId) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(request.getAppId() == null || request.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(request.getTargetVersion() == null || request.getTargetVersion() <= 0, 
                ErrorCode.PARAMS_ERROR, "目标版本号无效");

        // 查询目标版本
        AppVersion targetVersion = getVersionByNumber(request.getAppId(), request.getTargetVersion());
        ThrowUtils.throwIf(targetVersion == null, ErrorCode.NOT_FOUND_ERROR, "目标版本不存在");

        // 查询应用
        App app = appMapper.selectOneById(request.getAppId());

        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 创建回退版本
        AppVersionAddRequest createRequest = new AppVersionAddRequest();
        createRequest.setAppId(request.getAppId());
        createRequest.setContent(targetVersion.getContent());
        createRequest.setVersionName("回退到 v" + request.getTargetVersion());
        createRequest.setSummary("回退到版本 v" + request.getTargetVersion());
        createRequest.setChangeType(ChangeTypeEnum.ROLLBACK.getValue());
        createRequest.setParentVersion(request.getTargetVersion());

        return createVersion(createRequest, userId);
    }

    @Override
    public String getCurrentVersionContent(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("app_id", appId);

        queryWrapper.eq("is_current", 1);

        queryWrapper.limit(1);

        AppVersion version = this.getOne(queryWrapper);
        return version != null ? version.getContent() : null;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create().eq("app_id", appId);

        return this.remove(queryWrapper);
    }

    /**
     * 根据版本号获取版本
     */
    private AppVersion getVersionByNumber(Long appId, Integer versionNumber) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq("app_id", appId);

        queryWrapper.eq("version_number", versionNumber);

        queryWrapper.limit(1);
        return this.getOne(queryWrapper);
    }
}