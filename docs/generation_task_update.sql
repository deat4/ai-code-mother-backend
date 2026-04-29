-- 更新 generation_task 表，新增校验相关字段
-- 执行前请确认表中不存在这些字段

-- 添加 validation_summary 字段（TEXT类型，用于存储较长的校验摘要）
ALTER TABLE generation_task ADD COLUMN validation_summary TEXT NULL COMMENT '校验摘要' AFTER duration_ms;

-- 添加 validation_passed 字段（INT类型，匹配Java Integer）
ALTER TABLE generation_task ADD COLUMN validation_passed INT NULL COMMENT '校验是否通过 1-通过 0-未通过' AFTER validation_summary;

-- 添加 issue_count 字段（INT类型，存储 ERROR 数量）
ALTER TABLE generation_task ADD COLUMN issue_count INT NULL DEFAULT 0 COMMENT '问题数量（仅统计ERROR）' AFTER validation_passed;

-- 添加 warning_count 字段（INT类型，存储 WARN 数量）
ALTER TABLE generation_task ADD COLUMN warning_count INT NULL DEFAULT 0 COMMENT '警告数量（仅统计WARN）' AFTER issue_count;