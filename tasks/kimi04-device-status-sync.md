# kimi04 任务单: 设备状态名称统一 + DeviceController 修复

> 优先级: P2
> 截止: kimi02 完成后
> 依赖: kimi02 完成编译修复

## 任务概述

设备状态名称也存在不一致问题。openygt-dms 使用小写英文 (`idle`, `running`, `fault`, `offline`)，
而 decoction 项目使用大写 (`IDLE`, `BUSY`, `FAULT`, `OFFLINE`)。需要统一。

## 任务 1: 确认设备状态名称标准

检查 openygt-dms 中的设备状态:

| openygt-dms | decoction (当前) | 决策 |
|-------------|-----------------|------|
| idle | IDLE | 统一为 IDLE |
| running | BUSY | 统一为 BUSY |
| fault | FAULT | 统一为 FAULT |
| offline | OFFLINE | 统一为 OFFLINE |

建议: 保持 decoction 当前的大写风格 (IDLE/BUSY/FAULT/OFFLINE)，因为这是更常见的枚举风格。

## 任务 2: 检查 DeviceController 中的状态不一致

文件: `src/main/java/com/decoction/controller/DeviceController.java`

第92行: `device.setStatus("fault")` — 应改为 `"FAULT"`
第113行: `device.setStatus("idle")` — 应改为 `"IDLE"`

## 任务 3: 检查 StatisticsServiceImpl 中的设备状态

文件: `src/main/java/com/decoction/service/impl/StatisticsServiceImpl.java`

第129-131行的设备状态过滤已经使用大写，确认无需修改:
```java
long busy = devices.stream().filter(d -> "BUSY".equals(d.getStatus())).count();
long fault = devices.stream().filter(d -> "FAULT".equals(d.getStatus())).count();
long offline = devices.stream().filter(d -> "OFFLINE".equals(d.getStatus())).count();
```

## 任务 4: 检查前端中的设备状态引用

在 HTML 文件中搜索小写设备状态名:
```bash
grep -rn '"idle"\|"running"\|"fault"\|"offline"' src/main/resources/static/
```

统一为大写。

## 验收标准

1. 设备状态全部使用大写: IDLE, BUSY, FAULT, OFFLINE
2. DeviceController 中的 reportFault 和 clearFault 使用正确的状态名
3. 前端设备状态显示正确
