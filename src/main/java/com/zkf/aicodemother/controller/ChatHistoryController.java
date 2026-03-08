package com.zkf.aicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zkf.aicodemother.annotation.AuthCheck;
import com.zkf.aicodemother.common.BaseResponse;
import com.zkf.aicodemother.common.ResultUtils;
import com.zkf.aicodemother.constant.UserConstant;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.model.dto.chathistory.ChatHistoryAddRequest;
import com.zkf.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.zkf.aicodemother.model.entity.ChatHistory;
import com.zkf.aicodemother.model.enums.MessageTypeEnum;
import com.zkf.aicodemother.service.ChatHistoryService;
import com.zkf.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史控制器
 *
 * @author <a href="https://github.com/deat4/ai-code-mother-backend">zkf</a>
 */
@Slf4j
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    // region 用户接口

    /**
     * 保存用户消息
     *
     * @param addRequest 添加请求
     * @param request    HTTP请求
     * @return 是否成功
     */
    @PostMapping("/user/save")
    public BaseResponse<Boolean> saveUserMessage(@RequestBody ChatHistoryAddRequest addRequest,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(addRequest.getAppId() == null || addRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getMessage()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        Long userId = userService.getLoginUser(request).getId();

        boolean success = chatHistoryService.addChatMessage(
                addRequest.getAppId(),
                addRequest.getMessage(),
                MessageTypeEnum.USER.getValue(),
                userId
        );

        return ResultUtils.success(success);
    }

    /**
     * 保存AI消息
     *
     * @param addRequest 添加请求
     * @param request    HTTP请求
     * @return 是否成功
     */
    @PostMapping("/ai/save")
    public BaseResponse<Boolean> saveAiMessage(@RequestBody ChatHistoryAddRequest addRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(addRequest.getAppId() == null || addRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getMessage()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        Long userId = userService.getLoginUser(request).getId();

        boolean success = chatHistoryService.addChatMessage(
                addRequest.getAppId(),
                addRequest.getMessage(),
                MessageTypeEnum.AI.getValue(),
                userId
        );

        return ResultUtils.success(success);
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 游标时间(最后一条记录的创建时间)
     * @param request        HTTP请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(
            @PathVariable Long appId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) LocalDateTime lastCreateTime,
            HttpServletRequest request) {
        // 验证权限：只有应用创建者和管理员可以查看
        var loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime);
        return ResultUtils.success(result);
    }

    // endregion

    // region 管理员接口

    /**
     * 管理员分页查询所有对话历史
     *
     * @param queryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(
            @RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数不能为空");

        Page<ChatHistory> result = chatHistoryService.listAllChatHistoryByPageForAdmin(queryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除对话历史
     *
     * @param id      对话历史ID
     * @param request HTTP请求
     * @return 是否成功
     */
    @DeleteMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistory(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "ID无效");

        boolean success = chatHistoryService.removeById(id);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");

        return ResultUtils.success(true);
    }

    // endregion
}