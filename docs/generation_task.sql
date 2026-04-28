-- 生成任务表
CREATE TABLE IF NOT EXISTS generation_task (
    id BIGINT PRIMARY KEY COMMENT '任务 ID',
    app_id BIGINT NOT NULL COMMENT '应用 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    code_gen_type VARCHAR(32) COMMENT '代码生成类型',
    scene VARCHAR(32) COMMENT '生成场景',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态',
    current_stage VARCHAR(32) COMMENT '当前阶段',
    session_id VARCHAR(64) COMMENT '会话 ID',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME COMMENT '开始时间',
    finished_at DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '执行时长（毫秒）',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_app_id (app_id),
    INDEX idx_session_id (session_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务表';

-- 生成任务日志表
CREATE TABLE IF NOT EXISTS generation_task_log (
    id BIGINT PRIMARY KEY COMMENT '日志 ID',
    task_id BIGINT NOT NULL COMMENT '任务 ID',
    stage VARCHAR(32) COMMENT '阶段',
    log_type VARCHAR(32) NOT NULL COMMENT '日志类型',
    content TEXT COMMENT '日志内容',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务日志表';