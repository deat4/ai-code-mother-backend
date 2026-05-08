-- 自动修复闭环：扩展generation_task表
-- 添加修复相关字段

ALTER TABLE generation_task
ADD COLUMN repair_count INT DEFAULT 0 COMMENT '当前修复轮次',
ADD COLUMN max_repair_count INT DEFAULT 1 COMMENT '最大修复轮次',
ADD COLUMN repair_summary VARCHAR(500) COMMENT '修复摘要';