package com.zkf.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zkf.aicodemother.exception.ErrorCode;
import com.zkf.aicodemother.exception.ThrowUtils;
import com.zkf.aicodemother.mapper.GenerationTaskMapper;
import com.zkf.aicodemother.mapper.GenerationTaskLogMapper;
import com.zkf.aicodemother.model.entity.GenerationTask;
import com.zkf.aicodemother.model.entity.GenerationTaskLog;
import com.zkf.aicodemother.model.enums.GenerationTaskStageEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskStatusEnum;
import com.zkf.aicodemother.model.enums.GenerationTaskLogTypeEnum;
import com.zkf.aicodemother.service.GenerationTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生成任务服务实现
 */
@Slf4j
@Service
public class GenerationTaskServiceImpl implements GenerationTaskService {

    @Resource
    private GenerationTaskMapper generationTaskMapper;

    @Resource
    private GenerationTaskLogMapper generationTaskLogMapper;

    @Override
    public Long createTask(Long appId, Long userId, String codeGenType, String scene) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        LocalDateTime now = LocalDateTime.now();
        GenerationTask task = GenerationTask.builder()
                .appId(appId)
                .userId(userId)
                .codeGenType(codeGenType)
                .scene(scene)
                .status(GenerationTaskStatusEnum.PENDING.getValue())
                .currentStage(GenerationTaskStageEnum.INIT.getValue())
                .createTime(now)
                .updateTime(now)
                .build();

        generationTaskMapper.insert(task);
        log.info("创建生成任务: taskId={}, appId={}, userId={}, codeGenType={}, scene={}",
                task.getId(), appId, userId, codeGenType, scene);

        return task.getId();
    }

    @Override
    @Transactional
    public void startTask(Long taskId, String sessionId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(GenerationTaskStatusEnum.RUNNING.getValue());
        task.setSessionId(sessionId);
        task.setStartedAt(now);
        task.setUpdateTime(now);

        generationTaskMapper.update(task);
        appendLog(taskId, GenerationTaskStageEnum.INIT, GenerationTaskLogTypeEnum.STAGE_CHANGE,
                "任务启动，状态从 PENDING 变为 RUNNING");
        log.info("启动生成任务: taskId={}, sessionId={}", taskId, sessionId);
    }

    @Override
    @Transactional
    public void updateStage(Long taskId, GenerationTaskStageEnum stage) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");
        ThrowUtils.throwIf(stage == null, ErrorCode.PARAMS_ERROR, "阶段不能为空");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 只有 RUNNING 状态才能更新阶段
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
            log.warn("任务状态不是 RUNNING，无法更新阶段: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        String oldStage = task.getCurrentStage();
        task.setCurrentStage(stage.getValue());
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);
        appendLog(taskId, stage, GenerationTaskLogTypeEnum.STAGE_CHANGE,
                String.format("阶段从 %s 变为 %s", oldStage, stage.getValue()));
        log.info("更新任务阶段: taskId={}, oldStage={}, newStage={}", taskId, oldStage, stage.getValue());
    }

    @Override
    @Transactional
    public void markSuccess(Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 只有 RUNNING 状态才能标记成功（防止 validation 失败后 markFailed 被覆盖）
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
            log.warn("任务状态不是 RUNNING，无法标记成功: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = task.getStartedAt();
        long durationMs = startedAt != null ?
                java.time.Duration.between(startedAt, now).toMillis() : 0L;

        task.setStatus(GenerationTaskStatusEnum.SUCCESS.getValue());
        task.setCurrentStage(GenerationTaskStageEnum.DONE.getValue());
        task.setFinishedAt(now);
        task.setDurationMs(durationMs);
        task.setUpdateTime(now);

        generationTaskMapper.update(task);
        appendLog(taskId, GenerationTaskStageEnum.DONE, GenerationTaskLogTypeEnum.STAGE_CHANGE,
                String.format("任务完成，耗时 %d 毫秒", durationMs));
        log.info("标记任务成功: taskId={}, durationMs={}", taskId, durationMs);
    }

    @Override
    @Transactional
    public void markFailed(Long taskId, String errorMessage) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 只有 RUNNING 状态才能标记失败（防止取消状态被覆盖）
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())) {
            log.warn("任务状态不是 RUNNING，无法标记失败: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = task.getStartedAt();
        long durationMs = startedAt != null ?
                java.time.Duration.between(startedAt, now).toMillis() : 0L;

        task.setStatus(GenerationTaskStatusEnum.FAILED.getValue());
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(now);
        task.setDurationMs(durationMs);
        task.setUpdateTime(now);

        generationTaskMapper.update(task);
        appendLog(taskId, GenerationTaskStageEnum.getEnumByValue(task.getCurrentStage()),
                GenerationTaskLogTypeEnum.ERROR,
                String.format("任务失败: %s", errorMessage));
        log.error("标记任务失败: taskId={}, errorMessage={}", taskId, errorMessage);
    }

    @Override
    @Transactional
    public void markCanceled(Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 只有 RUNNING 或 PENDING 状态才能取消
        String currentStatus = task.getStatus();
        if (!GenerationTaskStatusEnum.RUNNING.getValue().equals(currentStatus)
                && !GenerationTaskStatusEnum.PENDING.getValue().equals(currentStatus)) {
            log.warn("任务状态不允许取消: taskId={}, status={}", taskId, currentStatus);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = task.getStartedAt();
        long durationMs = startedAt != null ?
                java.time.Duration.between(startedAt, now).toMillis() : 0L;

        task.setStatus(GenerationTaskStatusEnum.CANCELED.getValue());
        task.setFinishedAt(now);
        task.setDurationMs(durationMs);
        task.setUpdateTime(now);

        generationTaskMapper.update(task);
        appendLog(taskId, GenerationTaskStageEnum.getEnumByValue(task.getCurrentStage()),
                GenerationTaskLogTypeEnum.INFO,
                "用户取消任务");
        log.info("标记任务取消: taskId={}", taskId);
    }

    @Override
    public void appendLog(Long taskId, GenerationTaskStageEnum stage, GenerationTaskLogTypeEnum logType, String content) {
        if (taskId == null || taskId <= 0) {
            log.warn("taskId 无效，跳过日志记录");
            return;
        }

        GenerationTaskLog taskLog = GenerationTaskLog.builder()
                .taskId(taskId)
                .stage(stage != null ? stage.getValue() : null)
                .logType(logType.getValue())
                .content(StrUtil.isNotBlank(content) ? content : "")
                .createTime(LocalDateTime.now())
                .build();

        generationTaskLogMapper.insert(taskLog);
        log.debug("添加任务日志: taskId={}, stage={}, logType={}, content={}",
                taskId, stage, logType, content);
    }

    @Override
    public GenerationTask getTaskById(Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");
        return generationTaskMapper.selectOneById(taskId);
    }

    @Override
    public GenerationTask getLatestTaskByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId)
                .orderBy("create_time", false)
                .limit(1);

        return generationTaskMapper.selectOneByQuery(queryWrapper);
    }

    @Override
    public GenerationTask getTaskBySessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("session_id", sessionId)
                .limit(1);

        return generationTaskMapper.selectOneByQuery(queryWrapper);
    }

    @Override
    public List<GenerationTaskLog> getTaskLogs(Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("task_id", taskId)
                .orderBy("create_time", true);

        return generationTaskLogMapper.selectListByQuery(queryWrapper);
    }

    @Override
    public void updateValidationSummary(Long taskId, String summary, boolean passed) {
        updateValidationSummary(taskId, summary, passed, 0, 0);
    }

    @Override
    public void updateValidationSummary(Long taskId, String summary, boolean passed, int issueCount, int warningCount) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        task.setValidationSummary(summary);
        task.setValidationPassed(passed ? 1 : 0);
        task.setIssueCount(issueCount);
        task.setWarningCount(warningCount);
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);
        log.info("更新校验摘要: taskId={}, summary={}, passed={}, issueCount={}, warningCount={}",
                taskId, summary, passed, issueCount, warningCount);
    }

    @Override
    public void saveValidationResult(Long taskId, String summary, boolean passed, String issuesJson) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        task.setValidationSummary(summary);
        task.setValidationPassed(passed ? 1 : 0);
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);

        // 记录校验详情到日志表
        if (StrUtil.isNotBlank(issuesJson)) {
            appendLog(taskId, GenerationTaskStageEnum.VALIDATING,
                    GenerationTaskLogTypeEnum.VALIDATION, "校验问题详情: " + issuesJson);
        }

        log.info("保存校验结果: taskId={}, summary={}, passed={}", taskId, summary, passed);
    }

    @Override
    @Transactional
    public void updateRepairInfo(Long taskId, int repairCount, int maxRepairCount, String summary) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        task.setRepairCount(repairCount);
        task.setMaxRepairCount(maxRepairCount);
        task.setRepairSummary(summary);
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);
        log.info("更新修复信息: taskId={}, repairCount={}, maxRepairCount={}, summary={}",
                taskId, repairCount, maxRepairCount, summary);
    }

    @Override
    @Transactional
    public int incrementRepairCount(Long taskId) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        int currentCount = task.getRepairCount() != null ? task.getRepairCount() : 0;
        int newCount = currentCount + 1;
        task.setRepairCount(newCount);
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);
        log.info("增加修复轮次: taskId={}, oldCount={}, newCount={}", taskId, currentCount, newCount);
        return newCount;
    }

    @Override
    @Transactional
    public void initRepairQuota(Long taskId, int maxRepairCount) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务ID无效");

        GenerationTask task = generationTaskMapper.selectOneById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        task.setRepairCount(0);
        task.setMaxRepairCount(maxRepairCount);
        task.setUpdateTime(LocalDateTime.now());

        generationTaskMapper.update(task);
        log.info("初始化修复配额: taskId={}, maxRepairCount={}", taskId, maxRepairCount);
    }
}