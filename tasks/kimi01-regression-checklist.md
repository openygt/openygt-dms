# kimi01 回归测试清单

> 状态: 等待 kimi02 完成编译修复后执行

## 已完成的测试准备工作

1. ✅ 修复 `TaskControllerTest.java` 第121行: `status=PENDING` → `status=待泡药`
2. ✅ 修复 `FullWorkflowTest.java` 注释中的英文状态引用
3. ✅ 添加 V3 全流程测试方法:
   - `fullWorkflow_v3_fromPrescriptionToHandover` - 完整 V3 流程到已完成
   - `qualityInspect_rework_shouldResetToDecoct` - 质检返工回到待煎药
   - `qualityInspect_scrap_shouldMarkException` - 质检报废标记异常
   - `handover_partial_shouldMarkPartialCompleted` - 部分交接到已部分完成
   - `forceStatus_shouldWork` - 强制操作状态跳转
4. ✅ 添加缺失的 import (`StepLog`, `HandoverDetail`)
5. ✅ 验证 simulator.py 已支持 V3 全流程且使用中文状态名

## 回归测试执行命令

```bash
# 1. 编译
mvn compile -q

# 2. 运行全部测试
mvn test -q

# 3. 单独运行全流程测试
mvn test -Dtest=FullWorkflowTest -q

# 4. 单独运行并发测试
mvn test -Dtest=ConcurrentBindTest -q

# 5. 运行模拟器验证 (需先启动应用)
# mvn spring-boot:run &
# python3 simulator.py
```

## 测试覆盖清单

| 测试项 | 测试类/方法 | 状态 |
|--------|------------|------|
| 创建处方 → 任务状态 "待泡药" | FullWorkflowTest | 待执行 |
| 开始泡药 → "泡药中" | FullWorkflowTest | 待执行 |
| 结束泡药 → "待煎药" | FullWorkflowTest | 待执行 |
| 开始煎药 → "煎药中" | FullWorkflowTest | 待执行 |
| 结束煎药 → "待出液" | FullWorkflowTest | 待执行 |
| 开始出液 → "出液中" | FullWorkflowTest | 待执行 |
| 结束出液 → "待包装" | FullWorkflowTest | 待执行 |
| 开始包装 → "包装中" | FullWorkflowTest | 待执行 |
| 结束包装 → "待贴标" | FullWorkflowTest | 待执行 |
| 贴标确认 → "待质检" | FullWorkflowTest#fullWorkflow_v3_fromPrescriptionToHandover | 待执行 |
| 质检通过 → "待交接" | FullWorkflowTest#fullWorkflow_v3_fromPrescriptionToHandover | 待执行 |
| 质检返工 → "待煎药" | FullWorkflowTest#qualityInspect_rework_shouldResetToDecoct | 待执行 |
| 质检报废 → "已报废" | FullWorkflowTest#qualityInspect_scrap_shouldMarkException | 待执行 |
| 交接(部分) → "已部分完成" | FullWorkflowTest#handover_partial_shouldMarkPartialCompleted | 待执行 |
| 交接(完成) → "已完成" | FullWorkflowTest#fullWorkflow_v3_fromPrescriptionToHandover | 待执行 |
| 强制操作 → 目标状态 | FullWorkflowTest#forceStatus_shouldWork | 待执行 |
| 并发绑定设备 | ConcurrentBindTest | 待执行 |
| MQTT 断连容错 | MqttDisconnectTest | 待执行 |
| 设备状态查询 | TaskControllerTest | 待执行 |
| 打印管理 | 手动验证 | 待执行 |
