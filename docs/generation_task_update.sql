-- 更新 generation_task 表，新增校验相关字段
ALTER TABLE generation_task ADD COLUMN validation_summary VARCHAR(512) COMMENT '校验摘要';
ALTER TABLE generation_task ADD COLUMN validation_passed TINYINT(1) COMMENT '校验是否通过';