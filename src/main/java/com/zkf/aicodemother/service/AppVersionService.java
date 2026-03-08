package com.zkf.aicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.zkf.aicodemother.model.dto.appversion.AppVersionAddRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionQueryRequest;
import com.zkf.aicodemother.model.dto.appversion.AppVersionRollbackRequest;
import com.zkf.aicodemother.model.entity.AppVersion;
import com.zkf.aicodemother.model.vo.AppVersionDetailVO;
import com.zkf.aicodemother.model.vo.AppVersionVO;
import com.zkf.aicodemother.model.vo.VersionDiffVO;

/**
 * 应用版本服务层
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
public interface AppVersionService extends IService<AppVersion> {

    /**
     * 创建新版本
     *
     * @param request    创建请求
     * @param userId     用户ID
     * @return 新版本ID
     */
    Long createVersion(AppVersionAddRequest request, Long userId);

    /**
     * 获取版本详情
     *
     * @param versionId 版本ID
     * @return 版本详情
     */
    AppVersionDetailVO getVersionDetail(Long versionId);

    /**
     * 分页查询版本列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<AppVersionVO> listVersionsPage(AppVersionQueryRequest queryRequest);

    /**
     * 对比两个版本
     *
     * @param appId      应用ID
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     * @return 差异结果
     */
    VersionDiffVO diffVersions(Long appId, Integer oldVersion, Integer newVersion);

    /**
     * 回退到指定版本
     *
     * @param request  回退请求
     * @param userId   用户ID
     * @return 新版本ID
     */
    Long rollbackToVersion(AppVersionRollbackRequest request, Long userId);

    /**
     * 获取当前版本内容
     *
     * @param appId 应用ID
     * @return 代码内容
     */
    String getCurrentVersionContent(Long appId);

    /**
     * 删除应用的所有版本
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);
}