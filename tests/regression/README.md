# REVIEW-2026-04-28 修复项 回归测试套件

## 目录说明

本目录下所有脚本/配置仅用于验证 `docs/20260428/REVIEW-2026-04-28.md` 中 P0/P1 修复项，**不修改任何源码**。

## 文件清单

| 文件 | 用途 |
|---|---|
| `REVIEW-2026-04-28-test-plan.md` | 测试计划与用例清单（主文档） |
| `test-env.yml` | 测试环境专用 Spring 配置（SQLite 内存库、测试 JWT 密钥） |
| `run_all_regression.sh` | 总入口脚本：编译→启动→扫描→汇总 |
| `review_fixes_regression.py` | API 回归测试（登录路径、鉴权覆盖、库存路径迁移） |
| `jwt_security_test.py` | JWT 安全专项（Token 结构、篡改拦截、过期验证） |
| `api_path_consistency_test.py` | 白盒静态扫描（Controller 常量引用率、旧路径清理） |
| `mqtt_topic_test.py` | MQTT Topic 配置白盒验证 |
| `startup_log_scan.py` | 启动日志断言（弱密钥告警、SecurityFilterChain 加载） |

## 快速开始

```bash
cd /data2/docker/decoction/openygt-dms-01/openygt-dms

# 方式1：一键全量（推荐开发完成后执行）
bash tests/regression/run_all_regression.sh http://localhost:8080

# 方式2：分步执行（开发联调时按需使用）

# 2a. 静态扫描（不依赖服务启动）
python3 tests/regression/api_path_consistency_test.py .
python3 tests/regression/mqtt_topic_test.py .

# 2b. 单元测试
mvn clean test

# 2c. 使用测试配置启动应用
cp tests/regression/test-env.yml dms-app/src/main/resources/application.yml
mvn spring-boot:run -pl dms-app

# 2d. API 回归（另一个终端）
python3 tests/regression/review_fixes_regression.py
python3 tests/regression/jwt_security_test.py

# 2e. 日志扫描
python3 tests/regression/startup_log_scan.py app.log
```

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DMS_BASE_URL` | 被测服务地址 | `http://localhost:8080` |
| `DMS_USERNAME` | 测试账号 | `admin` |
| `DMS_PASSWORD` | 测试密码 | `admin123` |
| `JWT_SECRET` | JWT 密钥（启动时注入） | 使用 test-env.yml 中的值 |

## 报告输出

所有脚本执行后会在 `tests/reports/` 生成 JSON 报告：
- `review-fixes-report-{ts}.json`
- `jwt-security-report-{ts}.json`
- `api-path-scan-{ts}.json`
- `mqtt-topic-report-{ts}.json`
- `startup-log-report-{ts}.json`
- `regression-summary-{ts}.json`（汇总）

## 通过标准

1. `mvn test` 全量通过
2. `review_fixes_regression.py` 无 FAIL
3. `api_path_consistency_test.py` 无 HIGH 告警
4. 应用使用弱密钥启动时，日志中必现 WARN
