#!/bin/bash
# 自动验收闭环测试脚本 - 完整版
# 测试时间: 2026-05-06

BASE_URL="http://localhost:8123/api"
COOKIES="E:/BankProject/ai-code-mother/ai-code-mother/cookies.txt"
OUTPUT_DIR="E:/BankProject/ai-code-mother/ai-code-mother/testcases"
LOG_FILE="$OUTPUT_DIR/test_report.log"

mkdir -p "$OUTPUT_DIR"

# 初始化日志文件
echo "========================================" > "$LOG_FILE"
echo "自动验收闭环标准化测试报告" >> "$LOG_FILE"
echo "========================================" >> "$LOG_FILE"
echo "测试时间: $(date)" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# 记录测试结果函数
log_test() {
    local test_id="$1"
    local test_name="$2"
    local expected="$3"
    local actual="$4"
    local status="$5"
    echo "" >> "$LOG_FILE"
    echo "【${test_id}】${test_name}" >> "$LOG_FILE"
    echo "预期: ${expected}" >> "$LOG_FILE"
    echo "实际: ${actual}" >> "$LOG_FILE"
    echo "结论: ${status}" >> "$LOG_FILE"
}

echo "========================================" | tee -a "$LOG_FILE"
echo "自动验收闭环标准化测试" | tee -a "$LOG_FILE"
echo "========================================" | tee -a "$LOG_FILE"
echo "测试时间: $(date)" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

echo "【测试环境】" | tee -a "$LOG_FILE"
echo "- 服务地址: $BASE_URL" | tee -a "$LOG_FILE"
echo "- Cookie文件: $COOKIES" | tee -a "$LOG_FILE"
echo "- 输出目录: $OUTPUT_DIR" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# 检查登录状态
echo "【检查登录状态】" | tee -a "$LOG_FILE"
LOGIN_RESULT=$(curl -s "$BASE_URL/user/get/login" -b "$COOKIES")
echo "$LOGIN_RESULT" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# 列出可用应用
echo "【列出可用应用】" | tee -a "$LOG_FILE"
APPS=$(curl -s "$BASE_URL/app/my/list/page" -X POST -H "Content-Type: application/json" -b "$COOKIES" -d '{"current":1,"pageSize":20}')
echo "$APPS" | jq '.data.records[] | {id, appName, codeGenType}' | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

echo "========================================" | tee -a "$LOG_FILE"
echo "开始测试场景执行" | tee -a "$LOG_FILE"
echo "========================================" | tee -a "$LOG_FILE"

# ========================================
# A 组：成功场景测试
# ========================================

echo "" | tee -a "$LOG_FILE"
echo "=== A 组：成功场景测试 ===" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# A1: HTML 成功场景
echo "【A1】HTML 成功场景测试" | tee -a "$LOG_FILE"
echo "使用 appId: 404528013417308160 (HTML类型)" | tee -a "$LOG_FILE"
echo "发起修改请求: '修改标题文字'" | tee -a "$LOG_FILE"

# 测试 SSE 接口
SSE_OUTPUT="$OUTPUT_DIR/a1_sse_output.txt"
curl -s "$BASE_URL/app/chat/gen/code?appId=404528013417308160&message=修改页面标题文字" \
  -b "$COOKIES" \
  -H "Accept: text/event-stream" \
  --max-time 180 \
  > "$SSE_OUTPUT" 2>&1

echo "SSE 输出已保存到: $SSE_OUTPUT" | tee -a "$LOG_FILE"
echo "SSE 输出内容（前100行）:" | tee -a "$LOG_FILE"
head -100 "$SSE_OUTPUT" | tee -a "$LOG_FILE"

# 解析 sessionId 和 taskId
SESSION_ID=$(grep "event: session" "$SSE_OUTPUT" -A 1 | grep "data:" | jq -r '.sessionId' 2>/dev/null || echo "未找到")
TASK_ID=$(grep "event: task_created" "$SSE_OUTPUT" -A 1 | grep "data:" | jq -r '.taskId' 2>/dev/null || echo "未找到")

echo "sessionId: $SESSION_ID" | tee -a "$LOG_FILE"
echo "taskId: $TASK_ID" | tee -a "$LOG_FILE"

# 查询任务详情
if [ "$TASK_ID" != "未找到" ] && [ -n "$TASK_ID" ]; then
    echo "查询任务详情..." | tee -a "$LOG_FILE"
    TASK_DETAIL=$(curl -s "$BASE_URL/task/get?taskId=$TASK_ID" -b "$COOKIES")
    echo "$TASK_DETAIL" | jq '.' | tee -a "$LOG_FILE"

    # 查询任务日志
    echo "查询任务日志..." | tee -a "$LOG_FILE"
    TASK_LOGS=$(curl -s "$BASE_URL/task/logs?taskId=$TASK_ID" -b "$COOKIES")
    echo "$TASK_LOGS" | jq '.' | tee -a "$LOG_FILE"

    # 验证关键字段
    STATUS=$(echo "$TASK_DETAIL" | jq -r '.data.status' 2>/dev/null)
    CURRENT_STAGE=$(echo "$TASK_DETAIL" | jq -r '.data.currentStage' 2>/dev/null)
    VALIDATION_PASSED=$(echo "$TASK_DETAIL" | jq -r '.data.validationPassed' 2>/dev/null)
    VALIDATION_SUMMARY=$(echo "$TASK_DETAIL" | jq -r '.data.validationSummary' 2>/dev/null)
    ISSUE_COUNT=$(echo "$TASK_DETAIL" | jq -r '.data.issueCount' 2>/dev/null)

    log_test "A1" "HTML 成功场景" \
        "status=SUCCESS, validationPassed=true, stage=done" \
        "status=$STATUS, validationPassed=$VALIDATION_PASSED, stage=$CURRENT_STAGE, summary=$VALIDATION_SUMMARY" \
        "待确认"
fi

echo "" | tee -a "$LOG_FILE"
echo "A1 测试完成" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

echo "测试报告已保存到: $LOG_FILE"