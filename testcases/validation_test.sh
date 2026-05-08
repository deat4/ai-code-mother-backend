#!/bin/bash
# 自动验收闭环测试脚本
# 测试时间: 2026-05-06

BASE_URL="http://localhost:8123/api"
COOKIES="E:/BankProject/ai-code-mother/ai-code-mother/cookies.txt"
OUTPUT_DIR="E:/BankProject/ai-code-mother/ai-code-mother/testcases"

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

echo "========================================"
echo "自动验收闭环标准化测试"
echo "========================================"
echo "测试时间: $(date)"
echo ""

# 测试环境说明
echo "【测试环境】"
echo "- 服务地址: $BASE_URL"
echo "- Cookie文件: $COOKIES"
echo "- 输出目录: $OUTPUT_DIR"
echo ""

# 检查登录状态
echo "【检查登录状态】"
LOGIN_RESULT=$(curl -s "$BASE_URL/user/get/login" -b "$COOKIES")
echo "$LOGIN_RESULT"
echo ""

# 列出可用应用
echo "【列出可用应用】"
curl -s "$BASE_URL/app/my/list/page" -X POST -H "Content-Type: application/json" -b "$COOKIES" -d '{"current":1,"pageSize":20}' | jq '.data.records[] | {id, appName, codeGenType}'
echo ""

echo "========================================"
echo "测试场景开始"
echo "========================================"