# kimi01 任务单: 测试验证

> 优先级: P0 (与 kimi02 并行)
> 截止: kimi02 完成后立即验证
> 依赖: kimi02 完成编译修复后执行验证

## 任务概述

验证重构后的代码是否正确实现了 openygt-dms 的完整流程。

## 任务 1: 验证现有测试用例的正确性 (kimi02 修复前)

检查以下测试文件中的状态断言是否与中文状态名一致:

### FullWorkflowTest.java
- 第69行: `jsonPath("$.data.status").value("泡药中")` ✅ 正确
- 第78行: `value("待煎药")` ✅ 正确
- 第95行: `value("煎药中")` ✅ 正确
- 第102行: `value("待出液")` ✅ 正确
- 第109行: `value("出液中")` ✅ 正确
- 第116行: `value("待包装")` ✅ 正确
- 第133行: `value("包装中")` ✅ 正确
- 第140行: `value("待贴标")` ✅ 正确

结论: 测试用例的断言已经是中文状态名，说明测试是正确的，是实现有问题。

### TaskControllerTest.java
- 第51行: `value("待煎药")` — 需确认 bindDevice 后应该是什么状态
- 第121行: `status=PENDING` — ❌ 需改为 `status=待泡药`

## 任务 2: 补充 V3 功能测试用例 (kimi02 完成后)

在 `FullWorkflowTest.java` 中扩展全流程测试，添加贴标→质检→交接:

```java
@Test
void fullWorkflow_v3_fromPrescriptionToHandover() throws Exception {
    // ... 复用现有流程到 "待贴标" ...

    // 12. 贴标确认
    mockMvc.perform(post("/api/tasks/" + taskId + "/label/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(stageReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("待质检"));

    // 13. 质检通过
    QualityInspectRequest qualityReq = new QualityInspectRequest();
    qualityReq.setResult("通过");
    qualityReq.setOperatorId("OP001");
    mockMvc.perform(post("/api/tasks/" + taskId + "/quality")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(qualityReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("待交接"));

    // 14. 交接完成
    HandoverRequest handoverReq = new HandoverRequest();
    handoverReq.setBagCount(7);
    handoverReq.setHandoverType("自取");
    handoverReq.setHandoverUser("患者家属");
    handoverReq.setIsFinal(true);
    mockMvc.perform(post("/api/tasks/" + taskId + "/handover")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(handoverReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("已完成"));

    // 验证最终状态
    Task finalTask = taskService.getById(taskId);
    assertEquals("已完成", finalTask.getStatus());
    assertNotNull(finalTask.getCompleteTime());
    assertNotNull(finalTask.getHandoverTime());

    // 验证 StepLog 记录
    List<StepLog> steps = taskService.queryStepLogs(taskId);
    assertTrue(steps.size() >= 7, "应有至少7条工序记录 (SOAK/DECOCT/POUR/WRAP/LABEL/INSPECT/HANDOVER)");

    // 验证交接明细
    List<HandoverDetail> details = taskService.queryHandoverDetails(taskId);
    assertFalse(details.isEmpty());
    assertEquals(7, details.get(0).getBagCount());
}
```

### 补充质检异常路径测试:

```java
@Test
void qualityInspect_rework_shouldResetToDecoct() throws Exception {
    // 走到待质检状态...
    // 质检返工
    QualityInspectRequest req = new QualityInspectRequest();
    req.setResult("返工");
    req.setOperatorId("OP001");
    req.setRemark("药液浓度不足");
    // 断言状态回到 "待煎药"
}

@Test
void qualityInspect_scrap_shouldMarkException() throws Exception {
    // 走到待质检状态...
    // 质检报废
    QualityInspectRequest req = new QualityInspectRequest();
    req.setResult("报废");
    req.setOperatorId("OP001");
    req.setRemark("药液变质");
    // 断言状态为 "已报废"，isException=1
}
```

## 任务 3: 验证 simulator.py 全流程 (kimi02 完成后)

1. 启动应用: `mvn spring-boot:run`
2. 运行模拟器: `python3 simulator.py`
3. 检查输出中每一步的 success 是否为 True
4. 检查最终状态是否为 "已完成"

注意: simulator.py 可能也需要更新状态名称，检查其中的断言。

## 任务 4: 回归测试清单

| 测试项 | 预期结果 | 实际结果 |
|--------|---------|---------|
| `mvn compile` | 成功 | |
| `mvn test` | 全部通过 | |
| 创建处方 → 任务状态 "待泡药" | ✅ | |
| 开始泡药 → "泡药中" | ✅ | |
| 结束泡药 → "待煎药" | ✅ | |
| 开始煎药 → "煎药中" | ✅ | |
| 结束煎药 → "待出液" | ✅ | |
| 开始出液 → "出液中" | ✅ | |
| 结束出液 → "待包装" | ✅ | |
| 开始包装 → "包装中" | ✅ | |
| 结束包装 → "待贴标" | ✅ | |
| 贴标确认 → "待质检" | ✅ | |
| 质检通过 → "待交接" | ✅ | |
| 质检返工 → "待煎药" | ✅ | |
| 质检报废 → "已报废" | ✅ | |
| 交接(部分) → "已部分完成" | ✅ | |
| 交接(完成) → "已完成" | ✅ | |
| 强制操作 → 目标状态 | ✅ | |
| StepLog 每阶段有记录 | ✅ | |
| 温度上报 → 自动推进 | ✅ | |
| 设备告警 → 正常触发 | ✅ | |
| 打印管理 → 正常工作 | ✅ | |
