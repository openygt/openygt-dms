# kimi03 任务单: 前端状态名称同步 + simulator.py 修复

> 优先级: P2
> 截止: kimi02 完成后
> 依赖: kimi02 完成编译修复

## 任务概述

前端 HTML 文件和 Python 模拟器中可能存在英文状态名引用，需要与后端统一为中文。

## 任务 1: 检查并修复 simulator.py

检查 `simulator.py` 中的状态断言和请求参数:

```bash
grep -n "PENDING\|SOAKING\|SOAKED\|PROCESSING\|DECOCTED\|POURING\|WRAPPING\|COMPLETED\|BOUND" simulator.py
```

将所有英文状态名替换为中文。

## 任务 2: 检查并修复前端 HTML

需要检查的文件:
1. `src/main/resources/static/dashboard-v2.html`
2. `src/main/resources/static/decoction-simulator.html`
3. `src/main/resources/static/pda.html`
4. `src/main/resources/static/dashboard.html`

在这些文件中搜索英文状态名并替换:
- 搜索: `PENDING`, `SOAKING`, `SOAKED`, `PROCESSING`, `DECOCTED`, `POURING`, `WRAPPING`, `COMPLETED`, `BOUND`
- 替换为对应中文

注意: 前端可能有状态颜色映射、状态显示文本等，都需要更新。

## 任务 3: 检查 PdaController.java

文件: `src/main/java/com/decoction/controller/PdaController.java`
第44行的状态列表需要改为中文:

```java
// 改前:
wrapper.in(Task::getStatus, "PENDING", "SOAKING", "SOAKED", "BOUND", "PROCESSING", "DECOCTED", "POURING", "WRAPPING");

// 改后:
wrapper.in(Task::getStatus, "待泡药", "泡药中", "待煎药", "煎药中", "待出液", "出液中", "待包装", "包装中", "待贴标", "待质检", "待交接");
```

## 任务 4: 检查 SoakTimeoutScheduler.java

文件: `src/main/java/com/decoction/scheduler/SoakTimeoutScheduler.java`
第33行:

```java
// 改前:
wrapper.eq(Task::getStatus, "SOAKING");

// 改后:
wrapper.eq(Task::getStatus, "泡药中");
```

## 验收标准

1. `grep -r "PENDING\|SOAKING\|SOAKED\|PROCESSING\|DECOCTED\|POURING\|WRAPPING\|BOUND" src/` 无结果 (排除注释)
2. simulator.py 能跑通完整流程
3. 前端页面状态显示正确
