package com.zkf.aicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkf.aicodemother.model.dto.app.AppAddRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppQueryRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateAdminRequest;
import com.zkf.aicodemother.model.dto.app.AppUpdateRequest;
import com.zkf.aicodemother.model.entity.App;
import com.zkf.aicodemother.model.entity.User;
import com.zkf.aicodemother.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层接口
 */
public interface AppService {

    /**
     * 添加应用
     *
     * @param appAddRequest 添加请求
     * @param userId       用户 id
     * @return 新应用 id
     */
    Long addApp(AppAddRequest appAddRequest, Long userId);

    /**
     * 更新应用
     *
     * @param appUpdateRequest 更新请求
     * @param userId           用户 id
     * @return 是否成功
     */
    boolean updateApp(AppUpdateRequest appUpdateRequest, Long userId);

    /**
     * 管理员更新应用
     *
     * @param appUpdateAdminRequest 更新请求
     * @return 是否成功
     */
    boolean updateAppByAdmin(AppUpdateAdminRequest appUpdateAdminRequest);

    /**
     * 用户删除应用
     *
     * @param id     应用id
     * @param userId 用户id
     * @return 是否成功
     */
    boolean deleteApp(Long id, Long userId);

    /**
     * 管理员删除应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    boolean deleteAppByAdmin(Long id);

    /**
     * 根据 id 获取应用（未脱敏）
     *
     * @param id 应用id
     * @return 应用实体
     */
    App getById(Long id);

    /**
     * 根据 id 获取应用（脱敏）
     *
     * @param id 应用id
     * @return 应用VO
     */
    AppVO getAppVO(Long id);

    /**
     * 分页查询用户自己的应用
     *
     * @param appQueryRequest 查询请求
     * @param userId          用户id
     * @return 分页结果
     */
    Page<AppVO> listMyAppByPage(AppQueryRequest appQueryRequest, Long userId);

    /**
     * 分页查询精选应用
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果
     */
    Page<AppVO> listFeaturedAppByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员分页查询应用
     *
     * @param appQueryAdminRequest 查询请求
     * @return 分页结果
     */
    Page<App> listAppByPageForAdmin(AppQueryAdminRequest appQueryAdminRequest);

    /**
     * 获取查询条件封装（管理员查询）
     *
     * @param appQueryAdminRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryAdminRequest appQueryAdminRequest);

    /**
     * 获取脱敏后的应用信息
     *
     * @param app 应用实体
     * @return 应用 VO
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询条件封装（用户查询）
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 批量获取脱敏后的应用信息（包含用户信息）
     *
     * @param appList 应用列表
     * @return 应用 VO 列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 分页查询（支持 QueryWrapper）
     *
     * @param page          分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    Page<App> page(Page<App> page, QueryWrapper queryWrapper);

    /**
     * 根据 id 更新应用
     *
     * @param app 应用实体
     * @return 是否成功
     */
    boolean updateById(App app);

    /**
     * 与 AI 对话生成代码（流式）
     *
     * @param appId     应用 ID
     * @param message   用户消息
     * @param loginUser 登录用户
     * @return 流式响应
     */
    reactor.core.publisher.Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户
     * @return 部署后的访问 URL
     */
    String deployApp(Long appId, com.zkf.aicodemother.model.entity.User loginUser);

}
}
