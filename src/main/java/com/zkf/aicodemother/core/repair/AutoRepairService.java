package com.zkf.aicodemother.core.repair;

/**
 * 自动修复服务接口
 * 提供单轮自动修复能力
 */
public interface AutoRepairService {

    /**
     * 执行一次自动修复
     *
     * @param context 修复上下文
     * @return 修复结果
     */
    RepairResult repair(RepairContext context);
}