package com.zkf.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import com.zkf.aicodemother.common.BaseResponse;
import com.zkf.aicodemother.common.ResultUtils;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.model.entity.GenerationTaskLog;
import com.zkf.aicodemother.model.vo.GenerationTaskVO;
import com.zkf.aicodemother.model.vo.GenerationTaskLogVO;
import com.zkf.aicodemother.service.GenerationTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/task")
public class GenerationTaskController {

    @Resource
    private GenerationTaskService generationTaskService;

    /**
     * 根据任务 ID 获取任务详情
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    @GetMapping("/get")
    public BaseResponse<GenerationTaskVO> getTask(@RequestParam Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskService.getTaskById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        GenerationTaskVO taskVO = BeanUtil.copyProperties(task, GenerationTaskVO.class);
        return ResultUtils.success(taskVO);
    }

    /**
     * 获取任务日志列表
     *
     * @param taskId 任务 ID
     * @return 日志列表
     */
    @GetMapping("/logs")
    public BaseResponse<List<GenerationTaskLogVO>> getTaskLogs(@RequestParam Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        List<GenerationTaskLog> logs = generationTaskService.getTaskLogs(taskId);
        List<GenerationTaskLogVO> logVOs = logs.stream()
                .map(log -> BeanUtil.copyProperties(log, GenerationTaskLogVO.class))
                .collect(Collectors.toList());

        return ResultUtils.success(logVOs);
    }

    /**
     * 根据应用 ID 获取最新任务
     *
     * @param appId 应用 ID
     * @return 最新任务详情
     */
    @GetMapping("/app/latest")
    public BaseResponse<GenerationTaskVO> getLatestTaskByAppId(@RequestParam Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        GenerationTask task = generationTaskService.getLatestTaskByAppId(appId);
        if (task == null) {
            return ResultUtils.success(null);
        }

        GenerationTaskVO taskVO = BeanUtil.copyProperties(task, GenerationTaskVO.class);
        return ResultUtils.success(taskVO);
    }
}