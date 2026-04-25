# dms-masterdata 模块详细设计文档（V1.4）

> 文档版本：V1.4（对应需求与概设 V1.4）
> 模块：煎药室管理系统 — 主数据模块（dms-masterdata）
> 适用范围：医院/客户管理、煎药方案管理、内置方案初始化、温度阈值三层继承

---

## 一、模块概述

### 1.1 职责边界

`dms-masterdata` 是煎药室管理系统的**主数据管理中心**，为生产执行、设备物联等模块提供基础数据支撑：

1. **医院/客户管理**（`md_hospital`）：维护合作医院的基础信息，供处方创建时关联；
2. **煎药方案管理**（`md_decoct_scheme`）：定义不同药材的煎煮参数（压力模式、水量比例、浸泡/加热时间、先煎后下标志、温度告警阈值等），直接影响煎药机运行指令；
3. **药品追溯编码管理**：通过 `trace_code` 字段对接国家药品追溯系统；
4. **温度阈值三层继承**：方案级 `alarm_high_temp` / `alarm_low_temp` 可被设备级和系统级覆盖（优先级：方案级 > 设备级 > 系统默认）。

### 1.2 模块位置

```
openygt-dms/
├── dms-masterdata/
│   ├── src/main/java/cn/org/openygt/masterdata/
│   │   ├── MasterDataModule.java          # 模块常量（表前缀、API前缀）
│   │   ├── controller/
│   │   │   ├── HospitalController.java    # 【修改】补充搜索、校验
│   │   │   └── DecoctSchemeController.java # 【新增】煎药方案 CRUD
│   │   ├── service/
│   │   │   ├── HospitalService.java       # 【修改】补充搜索、校验
│   │   │   ├── HospitalServiceImpl.java   # 【修改】
│   │   │   ├── DecoctSchemeService.java   # 【新增】
│   │   │   └── DecoctSchemeServiceImpl.java # 【新增】
│   │   ├── entity/
│   │   │   ├── Hospital.java              # 【修改】继承 BaseEntity，扩展字段
│   │   │   └── DecoctScheme.java          # 【新增】
│   │   ├── mapper/
│   │   │   ├── HospitalMapper.java        # 已存在（MyBatis-Plus BaseMapper）
│   │   │   └── DecoctSchemeMapper.java    # 【新增】
│   │   └── dto/
│   │       ├── HospitalCreateRequest.java # 【新增】
│   │       ├── HospitalUpdateRequest.java # 【新增】
│   │       ├── HospitalQueryRequest.java  # 【新增】
│   │       ├── SchemeCreateRequest.java   # 【新增】
│   │       ├── SchemeUpdateRequest.java   # 【新增】
│   │       └── SchemeQueryRequest.java    # 【新增】
│   └── src/main/resources/
│       └── db/migration/V5__masterdata_enhance.sql  # 【新增】迁移脚本
└── dms-app/src/main/resources/db/migration/V5__masterdata_enhance.sql  # 【新增】
```

### 1.3 依赖关系

| 依赖模块 | 用途 |
|---------|------|
| `dms-common` | `BaseEntity`、`ApiResponse`、`GlobalExceptionHandler` |
| `spring-boot-starter-validation` | `@NotBlank`、`@Size` 等参数校验（父 POM 已引入） |
| `mybatis-plus-boot-starter` | ORM 与分页（父 POM 已引入） |

### 1.4 设计原则

1. **仅操作 `md_` 前缀表**，禁止访问其他模块表；
2. **所有实体必须继承 `BaseEntity`**，统一软删除、租户字段、审计字段；
3. **`code` 字段全局唯一**，创建/更新时需校验重复；
4. **内置方案保护**：`code` 以 `DEFAULT_` 开头的 6 种内置方案禁止删除，允许修改参数；
5. **单表写操作不加 `@Transactional`**，多表或含校验的写操作需加注解；
6. **所有 API 返回 `ApiResponse<T>`**，HTTP 状态码统一 200，业务错误通过 `code` 区分。

---

## 二、代码现状 vs 目标差距

### 2.1 差距总览表

| # | 功能点 | 现状 | 目标（V1.4） | 优先级 |
|---|--------|------|-------------|--------|
| 1 | `Hospital` 继承 `BaseEntity` | **未继承**，字段重复定义 | 继承 `BaseEntity`，移除重复字段 | P0 |
| 2 | 医院字段完整性 | 缺少 `contact`、`phone`、`address` | 补充 3 个字段，DTO 含校验注解 | P0 |
| 3 | 医院按名称搜索 | `list()` 无 `name` 参数 | 支持 `name` 模糊查询 | P0 |
| 4 | 医院编码唯一校验 | `create`/`update` 无重复校验 | 校验 `code` 唯一性 | P0 |
| 5 | `DecoctScheme` 实体 | **完全未实现** | 新增实体，继承 `BaseEntity` | P0 |
| 6 | `DecoctSchemeController` | **不存在** | 实现完整 CRUD | P0 |
| 7 | `DecoctSchemeService` | **不存在** | 实现完整 CRUD + 内置方案保护 | P0 |
| 8 | `DecoctSchemeMapper` | **不存在** | 继承 `BaseMapper<DecoctScheme>` | P0 |
| 9 | 数据库字段对齐 | 旧字段（`scheme_type`、`pressure` 等）与目标不一致 | V5 迁移：新增字段、映射旧数据、补 `code` | P0 |
| 10 | 温度阈值字段 | `trace_code`、`alarm_high_temp`、`alarm_low_temp` 缺失 | V5 迁移添加 | P0 |
| 11 | 内置方案初始化 | V2 脚本硬编码插入，无保护逻辑 | `CommandLineRunner` 智能初始化（空表才插入） | P1 |
| 12 | 方案级温度阈值继承 | 未实现 | 为 `dms-equipment` 提供查询接口 | P1 |

### 2.2 关键问题分析

#### 问题 A：Hospital 实体未继承 BaseEntity

**现状代码：**
```java
@Data
@TableName("md_hospital")
public class Hospital {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String tenantId;
    @TableLogic
    private Integer deleted;
    private Date createdAt;
    private Date updatedAt;
}
```

**问题**：`id`、`tenantId`、`deleted`、`createdAt`、`updatedAt` 与 `BaseEntity` 完全重复，且缺少 `contact`、`phone`、`address`。

**修复策略**：改为继承 `BaseEntity`，子类仅保留业务字段。

#### 问题 B：DecoctScheme 全量缺失

当前代码库中无任何 `DecoctScheme` 相关的 Java 类。数据库层面，`md_decoct_scheme` 表仍保留 V2 旧字段（`scheme_type`、`decoct_times`、`pressure`、`upper_water`、`heating_time`、`pre_heating_time`、`post_heating_time`），与 V1.4 目标字段差距大。

**修复策略**：
1. 编写 V5 迁移脚本，新增目标字段并做旧数据映射；
2. 保留旧字段（不删除），确保回滚安全；
3. 全新开发 Java 层代码（实体、Mapper、Service、Controller、DTO）。

#### 问题 C：code 唯一性无校验

`HospitalServiceImpl.create()` 直接 `insert`，未检查 `code` 是否已存在。若违反数据库唯一约束会抛出底层 `DuplicateKeyException`，用户体验差。

**修复策略**：Service 层显式预查询，重复时抛出 `IllegalStateException`（由 `GlobalExceptionHandler` 映射为 409）。

---

## 三、数据库详细设计

### 3.1 DDL —— SQLite 版本（开发/测试环境）

```sql
-- ============================================================
-- 医院/客户表
-- ============================================================
CREATE TABLE IF NOT EXISTS md_hospital (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id   VARCHAR(32) DEFAULT 'default',
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50) UNIQUE,
    contact     VARCHAR(50),
    phone       VARCHAR(20),
    address     VARCHAR(200),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     INTEGER DEFAULT 0
);

-- ============================================================
-- 煎药方案表
-- ============================================================
CREATE TABLE IF NOT EXISTS md_decoct_scheme (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id           VARCHAR(32) DEFAULT 'default',
    name                VARCHAR(100) NOT NULL,
    code                VARCHAR(50) UNIQUE,
    trace_code          VARCHAR(100),
    pressure_mode       VARCHAR(20),
    water_ratio         VARCHAR(20),
    soak_time           INTEGER,
    heat_time           INTEGER,
    decoct_count        INTEGER DEFAULT 1,
    pre_decoct          INTEGER DEFAULT 0,
    post_decoct         INTEGER DEFAULT 0,
    pre_decoct_time     INTEGER,
    post_decoct_time    INTEGER,
    alarm_high_temp     DECIMAL(5,2),
    alarm_low_temp      DECIMAL(5,2),
    description         TEXT,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER DEFAULT 0
);
```

### 3.2 DDL —— MySQL 版本（生产环境）

```sql
-- ============================================================
-- 医院/客户表
-- ============================================================
CREATE TABLE md_hospital (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id   VARCHAR(32) NOT NULL DEFAULT 'default',
    name        VARCHAR(100) NOT NULL COMMENT '医院名称',
    code        VARCHAR(50) UNIQUE COMMENT '医院编码（内部使用）',
    contact     VARCHAR(50) COMMENT '联系人',
    phone       VARCHAR(20) COMMENT '联系电话',
    address     VARCHAR(200) COMMENT '地址',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    KEY idx_md_hospital_tenant (tenant_id),
    KEY idx_md_hospital_name (tenant_id, name),
    KEY idx_md_hospital_code (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院/客户表';

-- ============================================================
-- 煎药方案表
-- ============================================================
CREATE TABLE md_decoct_scheme (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           VARCHAR(32) NOT NULL DEFAULT 'default',
    name                VARCHAR(100) NOT NULL COMMENT '方案名称',
    code                VARCHAR(50) UNIQUE COMMENT '方案编码',
    trace_code          VARCHAR(100) COMMENT '药品追溯编码（对接国家药品追溯系统）',
    pressure_mode       VARCHAR(20) COMMENT '压力模式：常压 / 微压密闭',
    water_ratio         VARCHAR(20) COMMENT '加水比例（如 1:10）',
    soak_time           INTEGER COMMENT '泡药时间（分钟）',
    heat_time           INTEGER COMMENT '加热时间（分钟）',
    decoct_count        INTEGER DEFAULT 1 COMMENT '煎煮次数',
    pre_decoct          INTEGER DEFAULT 0 COMMENT '是否先煎（0/1）',
    post_decoct         INTEGER DEFAULT 0 COMMENT '是否后下（0/1）',
    pre_decoct_time     INTEGER COMMENT '先煎时间（分钟）',
    post_decoct_time    INTEGER COMMENT '后下时间（分钟）',
    alarm_high_temp     DECIMAL(5,2) COMMENT '方案级高温告警阈值（℃）',
    alarm_low_temp      DECIMAL(5,2) COMMENT '方案级低温告警阈值（℃）',
    description         TEXT COMMENT '说明',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT NOT NULL DEFAULT 0,
    KEY idx_md_scheme_tenant (tenant_id),
    KEY idx_md_scheme_name (tenant_id, name),
    KEY idx_md_scheme_code (tenant_id, code),
    KEY idx_md_scheme_trace (tenant_id, trace_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='煎药方案表';
```

### 3.3 字段说明

#### md_hospital

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT / INTEGER | 是 | 主键，自增 |
| tenant_id | VARCHAR(32) | 是 | 租户ID，默认 `'default'` |
| name | VARCHAR(100) | 是 | 医院名称 |
| code | VARCHAR(50) | 否 | 医院编码，全局唯一 |
| contact | VARCHAR(50) | 否 | 联系人 |
| phone | VARCHAR(20) | 否 | 联系电话 |
| address | VARCHAR(200) | 否 | 地址 |
| created_at / updated_at | DATETIME | 是 | 审计字段 |
| deleted | TINYINT / INTEGER | 是 | 逻辑删除标志（0=正常，1=已删除） |

#### md_decoct_scheme

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT / INTEGER | 是 | 主键，自增 |
| tenant_id | VARCHAR(32) | 是 | 租户ID |
| name | VARCHAR(100) | 是 | 方案名称 |
| code | VARCHAR(50) | 否 | 方案编码，全局唯一 |
| trace_code | VARCHAR(100) | 否 | 药品追溯编码 |
| pressure_mode | VARCHAR(20) | 否 | 压力模式：`常压`、`微压密闭` |
| water_ratio | VARCHAR(20) | 否 | 加水比例，示例：`1:10` |
| soak_time | INTEGER | 否 | 泡药时间（分钟） |
| heat_time | INTEGER | 否 | 加热时间（分钟） |
| decoct_count | INTEGER | 否 | 煎煮次数，默认 1 |
| pre_decoct | INTEGER | 否 | 是否先煎：`0`=否，`1`=是 |
| post_decoct | INTEGER | 否 | 是否后下：`0`=否，`1`=是 |
| pre_decoct_time | INTEGER | 否 | 先煎时间（分钟） |
| post_decoct_time | INTEGER | 否 | 后下时间（分钟） |
| alarm_high_temp | DECIMAL(5,2) | 否 | 方案级高温告警阈值（℃） |
| alarm_low_temp | DECIMAL(5,2) | 否 | 方案级低温告警阈值（℃） |
| description | TEXT | 否 | 方案说明 |
| created_at / updated_at | DATETIME | 是 | 审计字段 |
| deleted | TINYINT / INTEGER | 是 | 逻辑删除标志 |

### 3.4 索引设计说明

1. **`uk_md_hospital_code`** / **`uk_md_decoct_scheme_code`**：`code` 全局唯一，支持按编码精确查找；
2. **`idx_md_hospital_name`** / **`idx_md_scheme_name`**：支持按名称模糊搜索；
3. **`idx_md_scheme_trace`**：`trace_code` 索引，支持药品追溯编码反查；
4. 所有二级索引均带 `tenant_id` 前缀，为后续多租户分片预留。

### 3.5 V5 迁移脚本（dms-app/src/main/resources/db/migration/V5__masterdata_enhance.sql）

```sql
-- ============================================================
-- V5__masterdata_enhance.sql
-- 主数据模块 V1.4 字段增强与旧数据迁移
-- 目标环境：SQLite（开发/测试）与 MySQL（生产）
-- ============================================================

-- ========== 1. md_hospital 扩展 ==========
ALTER TABLE md_hospital ADD COLUMN contact VARCHAR(50);
ALTER TABLE md_hospital ADD COLUMN phone   VARCHAR(20);
ALTER TABLE md_hospital ADD COLUMN address VARCHAR(200);

-- ========== 2. md_decoct_scheme 扩展 ==========
-- 2.1 新增目标字段
ALTER TABLE md_decoct_scheme ADD COLUMN code              VARCHAR(50);
ALTER TABLE md_decoct_scheme ADD COLUMN trace_code        VARCHAR(100);
ALTER TABLE md_decoct_scheme ADD COLUMN pressure_mode     VARCHAR(20);
ALTER TABLE md_decoct_scheme ADD COLUMN water_ratio       VARCHAR(20);
ALTER TABLE md_decoct_scheme ADD COLUMN soak_time         INTEGER;
ALTER TABLE md_decoct_scheme ADD COLUMN heat_time         INTEGER;
ALTER TABLE md_decoct_scheme ADD COLUMN decoct_count      INTEGER DEFAULT 1;
ALTER TABLE md_decoct_scheme ADD COLUMN pre_decoct        INTEGER DEFAULT 0;
ALTER TABLE md_decoct_scheme ADD COLUMN post_decoct       INTEGER DEFAULT 0;
ALTER TABLE md_decoct_scheme ADD COLUMN pre_decoct_time   INTEGER;
ALTER TABLE md_decoct_scheme ADD COLUMN post_decoct_time  INTEGER;
ALTER TABLE md_decoct_scheme ADD COLUMN alarm_high_temp   DECIMAL(5,2);
ALTER TABLE md_decoct_scheme ADD COLUMN alarm_low_temp    DECIMAL(5,2);

-- 2.2 旧数据映射（SQLite 语法；MySQL 下将 || 替换为 CONCAT，并去掉 transaction）
-- 说明：
--   scheme_type -> pre_decoct / post_decoct
--   pressure    -> pressure_mode
--   decoct_times-> decoct_count（0 视为 1）
--   upper_water -> water_ratio（>0 保留文本，否则 NULL）
--   heating_time-> heat_time
--   pre_heating_time -> pre_decoct_time
--   post_heating_time-> post_decoct_time

UPDATE md_decoct_scheme SET
    pressure_mode = CASE pressure
        WHEN 1 THEN '常压'
        WHEN 0 THEN '微压密闭'
        ELSE NULL
    END,
    pre_decoct = CASE scheme_type
        WHEN 1 THEN 1
        WHEN 3 THEN 1
        ELSE 0
    END,
    post_decoct = CASE scheme_type
        WHEN 2 THEN 1
        WHEN 3 THEN 1
        ELSE 0
    END,
    decoct_count = CASE WHEN decoct_times IS NULL OR decoct_times = 0 THEN 1 ELSE decoct_times END,
    water_ratio = CASE WHEN upper_water > 0 THEN CAST(upper_water AS TEXT) ELSE NULL END,
    heat_time = heating_time,
    pre_decoct_time = pre_heating_time,
    post_decoct_time = post_heating_time;

-- 2.3 为内置方案设置固定 code（以 DEFAULT_ 开头，受保护）
UPDATE md_decoct_scheme SET code = 'DEFAULT_NORMAL'     WHERE name = '常压汤药';
UPDATE md_decoct_scheme SET code = 'DEFAULT_TONIC'      WHERE name = '常压补药';
UPDATE md_decoct_scheme SET code = 'DEFAULT_PRESSURE'   WHERE name = '微压(密闭)汤药';
UPDATE md_decoct_scheme SET code = 'DEFAULT_PRE_DECOCT' WHERE name = '先煎汤药';
UPDATE md_decoct_scheme SET code = 'DEFAULT_POST_DECOCT'WHERE name = '后下汤药';
UPDATE md_decoct_scheme SET code = 'DEFAULT_PRE_POST'   WHERE name = '先煎后下汤药';

-- 2.4 为用户自定义方案生成迁移 code（避免 NULL 导致唯一索引问题）
UPDATE md_decoct_scheme SET code = 'MIGRATED_' || id WHERE code IS NULL;

-- 2.5 设置内置方案默认温度阈值（℃）
UPDATE md_decoct_scheme SET alarm_high_temp = 120, alarm_low_temp = 50 WHERE code = 'DEFAULT_NORMAL';
UPDATE md_decoct_scheme SET alarm_high_temp = 120, alarm_low_temp = 50 WHERE code = 'DEFAULT_TONIC';
UPDATE md_decoct_scheme SET alarm_high_temp = 130, alarm_low_temp = 60 WHERE code = 'DEFAULT_PRESSURE';
UPDATE md_decoct_scheme SET alarm_high_temp = 120, alarm_low_temp = 50 WHERE code = 'DEFAULT_PRE_DECOCT';
UPDATE md_decoct_scheme SET alarm_high_temp = 120, alarm_low_temp = 50 WHERE code = 'DEFAULT_POST_DECOCT';
UPDATE md_decoct_scheme SET alarm_high_temp = 120, alarm_low_temp = 50 WHERE code = 'DEFAULT_PRE_POST';

-- 2.6 创建唯一索引（SQLite 在 ALTER 后单独创建；MySQL 可直接在 DDL 中指定）
-- 注意：如果存在重复 code，需先清理数据再执行以下语句
CREATE UNIQUE INDEX IF NOT EXISTS uk_md_decoct_scheme_code ON md_decoct_scheme(code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_md_hospital_code ON md_hospital(code);
```

> **MySQL 适配说明**：
> - 将 `||` 字符串连接替换为 `CONCAT()`；
> - 将 `CAST(x AS TEXT)` 替换为 `CAST(x AS CHAR)`；
> - `ALTER TABLE ... ADD COLUMN` 语法兼容；
> - 唯一约束建议在确认数据无重复后通过 `ADD UNIQUE INDEX` 创建。



---

## 四、实体类详细设计

### 4.1 包结构

```
cn.org.openygt.masterdata
├── entity
│   ├── Hospital.java
│   └── DecoctScheme.java
├── dto
│   ├── HospitalCreateRequest.java
│   ├── HospitalUpdateRequest.java
│   ├── HospitalQueryRequest.java
│   ├── SchemeCreateRequest.java
│   ├── SchemeUpdateRequest.java
│   └── SchemeQueryRequest.java
├── mapper
│   ├── HospitalMapper.java
│   └── DecoctSchemeMapper.java
├── service
│   ├── HospitalService.java
│   ├── HospitalServiceImpl.java
│   ├── DecoctSchemeService.java
│   └── DecoctSchemeServiceImpl.java
└── controller
    ├── HospitalController.java
    └── DecoctSchemeController.java
```

### 4.2 Hospital（修改）

**目标代码**：

```java
package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_hospital")
public class Hospital extends BaseEntity {

    private String name;
    private String code;
    private String contact;
    private String phone;
    private String address;
}
```

**变更点说明**：

| 变更 | 说明 |
|------|------|
| 继承 `BaseEntity` | 移除 `id`、`tenantId`、`deleted`、`createdAt`、`updatedAt`，统一由父类管理 |
| 新增 `contact` | 联系人，VARCHAR(50) |
| 新增 `phone` | 联系电话，VARCHAR(20) |
| 新增 `address` | 地址，VARCHAR(200) |
| `@EqualsAndHashCode(callSuper = true)` | 确保 Lombok 生成方法时包含父类字段 |

### 4.3 DecoctScheme（新增）

```java
package cn.org.openygt.masterdata.entity;

import cn.org.openygt.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_decoct_scheme")
public class DecoctScheme extends BaseEntity {

    private String name;
    private String code;
    private String traceCode;
    private String pressureMode;
    private String waterRatio;
    private Integer soakTime;
    private Integer heatTime;
    private Integer decoctCount;
    private Integer preDecoct;
    private Integer postDecoct;
    private Integer preDecoctTime;
    private Integer postDecoctTime;
    private BigDecimal alarmHighTemp;
    private BigDecimal alarmLowTemp;
    private String description;
}
```

### 4.4 DTO 详细设计

#### 4.4.1 医院 DTO

```java
// HospitalCreateRequest.java
package cn.org.openygt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HospitalCreateRequest {

    @NotBlank(message = "医院名称不能为空")
    @Size(max = 100, message = "医院名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "医院编码长度不能超过50")
    private String code;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contact;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;

    @Size(max = 200, message = "地址长度不能超过200")
    private String address;
}

// HospitalUpdateRequest.java
package cn.org.openygt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HospitalUpdateRequest {

    @NotBlank(message = "医院名称不能为空")
    @Size(max = 100, message = "医院名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "医院编码长度不能超过50")
    private String code;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contact;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;

    @Size(max = 200, message = "地址长度不能超过200")
    private String address;
}

// HospitalQueryRequest.java（用于列表查询参数封装，可选）
package cn.org.openygt.masterdata.dto;

import lombok.Data;

@Data
public class HospitalQueryRequest {
    private String name;   // 模糊匹配
    private Integer page = 1;
    private Integer size = 20;
}
```

#### 4.4.2 煎药方案 DTO

```java
// SchemeCreateRequest.java
package cn.org.openygt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SchemeCreateRequest {

    @NotBlank(message = "方案名称不能为空")
    @Size(max = 100, message = "方案名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "方案编码长度不能超过50")
    private String code;

    @Size(max = 100, message = "追溯编码长度不能超过100")
    private String traceCode;

    @Size(max = 20, message = "压力模式长度不能超过20")
    private String pressureMode;

    @Size(max = 20, message = "加水比例长度不能超过20")
    private String waterRatio;

    private Integer soakTime;
    private Integer heatTime;
    private Integer decoctCount;
    private Integer preDecoct;
    private Integer postDecoct;
    private Integer preDecoctTime;
    private Integer postDecoctTime;
    private BigDecimal alarmHighTemp;
    private BigDecimal alarmLowTemp;

    @Size(max = 500, message = "说明长度不能超过500")
    private String description;
}

// SchemeUpdateRequest.java
package cn.org.openygt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SchemeUpdateRequest {

    @NotBlank(message = "方案名称不能为空")
    @Size(max = 100, message = "方案名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "方案编码长度不能超过50")
    private String code;

    @Size(max = 100, message = "追溯编码长度不能超过100")
    private String traceCode;

    @Size(max = 20, message = "压力模式长度不能超过20")
    private String pressureMode;

    @Size(max = 20, message = "加水比例长度不能超过20")
    private String waterRatio;

    private Integer soakTime;
    private Integer heatTime;
    private Integer decoctCount;
    private Integer preDecoct;
    private Integer postDecoct;
    private Integer preDecoctTime;
    private Integer postDecoctTime;
    private BigDecimal alarmHighTemp;
    private BigDecimal alarmLowTemp;

    @Size(max = 500, message = "说明长度不能超过500")
    private String description;
}

// SchemeQueryRequest.java
package cn.org.openygt.masterdata.dto;

import lombok.Data;

@Data
public class SchemeQueryRequest {
    private String name;   // 模糊匹配
    private Integer page = 1;
    private Integer size = 20;
}
```

---

## 五、REST API 详细设计

### 5.1 接口清单

#### 医院管理

| 方法 | 路径 | 说明 | 请求体 / 查询参数 | 响应 |
|------|------|------|------------------|------|
| POST | `/api/v1/md/hospitals` | 创建医院 | `HospitalCreateRequest` | `ApiResponse<Hospital>` |
| PUT | `/api/v1/md/hospitals/{id}` | 更新医院 | `HospitalUpdateRequest` | `ApiResponse<Hospital>` |
| GET | `/api/v1/md/hospitals/{id}` | 查询医院详情 | path: `id` | `ApiResponse<Hospital>` |
| DELETE | `/api/v1/md/hospitals/{id}` | 删除医院 | path: `id` | `ApiResponse<Void>` |
| GET | `/api/v1/md/hospitals` | 分页查询 | query: `name`(模糊), `page`, `size` | `ApiResponse<IPage<Hospital>>` |

#### 煎药方案管理

| 方法 | 路径 | 说明 | 请求体 / 查询参数 | 响应 |
|------|------|------|------------------|------|
| POST | `/api/v1/md/schemes` | 创建方案 | `SchemeCreateRequest` | `ApiResponse<DecoctScheme>` |
| PUT | `/api/v1/md/schemes/{id}` | 更新方案 | `SchemeUpdateRequest` | `ApiResponse<DecoctScheme>` |
| GET | `/api/v1/md/schemes/{id}` | 查询方案详情 | path: `id` | `ApiResponse<DecoctScheme>` |
| DELETE | `/api/v1/md/schemes/{id}` | 删除方案 | path: `id` | `ApiResponse<Void>` |
| GET | `/api/v1/md/schemes` | 分页查询 | query: `name`(模糊), `page`, `size` | `ApiResponse<IPage<DecoctScheme>>` |

### 5.2 Controller 完整代码

#### HospitalController（修改后）

```java
package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.dto.HospitalCreateRequest;
import cn.org.openygt.masterdata.dto.HospitalUpdateRequest;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/md/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    public ApiResponse<Hospital> create(@RequestBody @Valid HospitalCreateRequest request) {
        return ApiResponse.success(hospitalService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Hospital> update(@PathVariable Long id,
                                        @RequestBody @Valid HospitalUpdateRequest request) {
        return ApiResponse.success(hospitalService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Hospital> getById(@PathVariable Long id) {
        return ApiResponse.success(hospitalService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<Hospital>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(hospitalService.list(name, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hospitalService.delete(id);
        return ApiResponse.success();
    }
}
```

#### DecoctSchemeController（新增）

```java
package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/md/schemes")
public class DecoctSchemeController {

    private final DecoctSchemeService decoctSchemeService;

    public DecoctSchemeController(DecoctSchemeService decoctSchemeService) {
        this.decoctSchemeService = decoctSchemeService;
    }

    @PostMapping
    public ApiResponse<DecoctScheme> create(@RequestBody @Valid SchemeCreateRequest request) {
        return ApiResponse.success(decoctSchemeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DecoctScheme> update(@PathVariable Long id,
                                            @RequestBody @Valid SchemeUpdateRequest request) {
        return ApiResponse.success(decoctSchemeService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DecoctScheme> getById(@PathVariable Long id) {
        return ApiResponse.success(decoctSchemeService.getById(id));
    }

    @GetMapping
    public ApiResponse<IPage<DecoctScheme>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(decoctSchemeService.list(name, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        decoctSchemeService.delete(id);
        return ApiResponse.success();
    }
}
```

### 5.3 Service 完整代码

#### HospitalService / HospitalServiceImpl（修改后）

```java
// HospitalService.java
package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.dto.HospitalCreateRequest;
import cn.org.openygt.masterdata.dto.HospitalUpdateRequest;
import cn.org.openygt.masterdata.entity.Hospital;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface HospitalService {
    Hospital create(HospitalCreateRequest request);
    Hospital update(Long id, HospitalUpdateRequest request);
    Hospital getById(Long id);
    IPage<Hospital> list(String name, int page, int size);
    void delete(Long id);
}

// HospitalServiceImpl.java
package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.dto.HospitalCreateRequest;
import cn.org.openygt.masterdata.dto.HospitalUpdateRequest;
import cn.org.openygt.masterdata.entity.Hospital;
import cn.org.openygt.masterdata.mapper.HospitalMapper;
import cn.org.openygt.masterdata.service.HospitalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HospitalServiceImpl implements HospitalService {

    private final HospitalMapper hospitalMapper;

    public HospitalServiceImpl(HospitalMapper hospitalMapper) {
        this.hospitalMapper = hospitalMapper;
    }

    @Override
    @Transactional
    public Hospital create(HospitalCreateRequest request) {
        if (StringUtils.hasText(request.getCode())) {
            ensureCodeUnique(request.getCode(), null);
        }
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(request, hospital);
        hospitalMapper.insert(hospital);
        return hospital;
    }

    @Override
    @Transactional
    public Hospital update(Long id, HospitalUpdateRequest request) {
        Hospital existing = hospitalMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(existing.getCode())) {
            ensureCodeUnique(request.getCode(), id);
        }
        BeanUtils.copyProperties(request, existing);
        existing.setId(id);
        hospitalMapper.updateById(existing);
        return hospitalMapper.selectById(id);
    }

    @Override
    public Hospital getById(Long id) {
        Hospital hospital = hospitalMapper.selectById(id);
        if (hospital == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        return hospital;
    }

    @Override
    public IPage<Hospital> list(String name, int page, int size) {
        LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Hospital::getName, name);
        }
        wrapper.orderByDesc(Hospital::getCreatedAt);
        return hospitalMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Hospital existing = hospitalMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("医院不存在: " + id);
        }
        hospitalMapper.deleteById(id);
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Hospital> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hospital::getCode, code);
        if (excludeId != null) {
            wrapper.ne(Hospital::getId, excludeId);
        }
        Long count = hospitalMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalStateException("医院编码已存在: " + code);
        }
    }
}
```

#### DecoctSchemeService / DecoctSchemeServiceImpl（新增）

```java
// DecoctSchemeService.java
package cn.org.openygt.masterdata.service;

import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DecoctSchemeService {
    DecoctScheme create(SchemeCreateRequest request);
    DecoctScheme update(Long id, SchemeUpdateRequest request);
    DecoctScheme getById(Long id);
    IPage<DecoctScheme> list(String name, int page, int size);
    void delete(Long id);
}

// DecoctSchemeServiceImpl.java
package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.dto.SchemeCreateRequest;
import cn.org.openygt.masterdata.dto.SchemeUpdateRequest;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DecoctSchemeServiceImpl implements DecoctSchemeService {

    private final DecoctSchemeMapper decoctSchemeMapper;

    public DecoctSchemeServiceImpl(DecoctSchemeMapper decoctSchemeMapper) {
        this.decoctSchemeMapper = decoctSchemeMapper;
    }

    @Override
    @Transactional
    public DecoctScheme create(SchemeCreateRequest request) {
        if (StringUtils.hasText(request.getCode())) {
            ensureCodeUnique(request.getCode(), null);
        }
        DecoctScheme scheme = new DecoctScheme();
        BeanUtils.copyProperties(request, scheme);
        normalizeDefaults(scheme);
        decoctSchemeMapper.insert(scheme);
        return scheme;
    }

    @Override
    @Transactional
    public DecoctScheme update(Long id, SchemeUpdateRequest request) {
        DecoctScheme existing = decoctSchemeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(existing.getCode())) {
            ensureCodeUnique(request.getCode(), id);
        }
        // 内置方案禁止修改 code
        if (isBuiltIn(existing) && StringUtils.hasText(request.getCode())
                && !request.getCode().equals(existing.getCode())) {
            throw new IllegalStateException("内置方案不允许修改编码");
        }
        BeanUtils.copyProperties(request, existing);
        existing.setId(id);
        normalizeDefaults(existing);
        decoctSchemeMapper.updateById(existing);
        return decoctSchemeMapper.selectById(id);
    }

    @Override
    public DecoctScheme getById(Long id) {
        DecoctScheme scheme = decoctSchemeMapper.selectById(id);
        if (scheme == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        return scheme;
    }

    @Override
    public IPage<DecoctScheme> list(String name, int page, int size) {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(DecoctScheme::getName, name);
        }
        wrapper.orderByDesc(DecoctScheme::getCreatedAt);
        return decoctSchemeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DecoctScheme existing = decoctSchemeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("煎药方案不存在: " + id);
        }
        if (isBuiltIn(existing)) {
            throw new IllegalStateException("内置方案不允许删除: " + existing.getName());
        }
        decoctSchemeMapper.deleteById(id);
    }

    private boolean isBuiltIn(DecoctScheme scheme) {
        return StringUtils.hasText(scheme.getCode()) && scheme.getCode().startsWith("DEFAULT_");
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DecoctScheme::getCode, code);
        if (excludeId != null) {
            wrapper.ne(DecoctScheme::getId, excludeId);
        }
        Long count = decoctSchemeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalStateException("方案编码已存在: " + code);
        }
    }

    private void normalizeDefaults(DecoctScheme scheme) {
        if (scheme.getDecoctCount() == null) {
            scheme.setDecoctCount(1);
        }
        if (scheme.getPreDecoct() == null) {
            scheme.setPreDecoct(0);
        }
        if (scheme.getPostDecoct() == null) {
            scheme.setPostDecoct(0);
        }
    }
}
```

### 5.4 异常码与幂等性设计

#### 异常码表

| 业务场景 | 异常类型 | HTTP 状态码 | ApiResponse.code | 错误消息示例 |
|---------|---------|------------|------------------|-------------|
| 参数校验失败 | `MethodArgumentNotValidException` | 400 | 400 | `医院名称不能为空` |
| 医院/方案不存在 | `IllegalArgumentException` | 400 | 400 | `医院不存在: 123` |
| 编码重复 | `IllegalStateException` | 409 | 409 | `医院编码已存在: H001` |
| 删除内置方案 | `IllegalStateException` | 409 | 409 | `内置方案不允许删除: 常压汤药` |
| 修改内置方案编码 | `IllegalStateException` | 409 | 409 | `内置方案不允许修改编码` |
| 通用服务器错误 | `Exception` | 500 | 500 | `服务器内部错误` |

> 说明：当前 `GlobalExceptionHandler` 已覆盖上述异常类型，无需新增 Handler。

#### 幂等性设计

- **创建接口**：客户端可通过传入唯一业务键 `code` 实现业务幂等。若 `code` 重复，服务端返回 409，客户端可据此判定为"已创建"；
- **更新/删除接口**：基于资源 ID 操作，天然幂等。重复提交返回相同结果（更新成功或 400 资源不存在）；
- **无自动重试机制**：当前版本不引入 Token 或乐观锁，依赖前端防抖与异常提示。

---

## 六、内置方案初始化设计（CommandLineRunner）

### 6.1 设计目标

应用启动时，若 `md_decoct_scheme` 表为空（或不存在任何 `code` 以 `DEFAULT_` 开头的记录），则自动插入 6 种内置煎药方案。保证新环境开箱即用，同时避免重复插入。

### 6.2 实现代码

```java
package cn.org.openygt.masterdata.initializer;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecoctSchemeInitializer implements CommandLineRunner {

    private final DecoctSchemeMapper decoctSchemeMapper;

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DecoctScheme::getCode, "DEFAULT_");
        long builtInCount = decoctSchemeMapper.selectCount(wrapper);

        if (builtInCount > 0) {
            log.info("内置煎药方案已存在，跳过初始化。count={}", builtInCount);
            return;
        }

        List<DecoctScheme> schemes = Arrays.asList(
            buildScheme("常压汤药", "DEFAULT_NORMAL", "常压", 120, 50),
            buildScheme("常压补药", "DEFAULT_TONIC", "常压", 120, 50),
            buildScheme("微压密闭", "DEFAULT_PRESSURE", "微压密闭", 130, 60),
            buildScheme("先煎", "DEFAULT_PRE_DECOCT", "常压", 120, 50),
            buildScheme("后下", "DEFAULT_POST_DECOCT", "常压", 120, 50),
            buildScheme("先煎后下", "DEFAULT_PRE_POST", "常压", 120, 50)
        );

        for (DecoctScheme scheme : schemes) {
            decoctSchemeMapper.insert(scheme);
        }
        log.info("内置煎药方案初始化完成，共插入 {} 条", schemes.size());
    }

    private DecoctScheme buildScheme(String name, String code, String pressureMode,
                                     int highTemp, int lowTemp) {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setName(name);
        scheme.setCode(code);
        scheme.setPressureMode(pressureMode);
        scheme.setDecoctCount(1);
        scheme.setPreDecoct(0);
        scheme.setPostDecoct(0);
        scheme.setAlarmHighTemp(new BigDecimal(highTemp));
        scheme.setAlarmLowTemp(new BigDecimal(lowTemp));
        return scheme;
    }
}
```

### 6.3 设计要点

1. **幂等检查**：以 `code LIKE 'DEFAULT_%'` 为判断条件，而非全表空判断，防止误删用户数据后重复插入；
2. **顺序无关**：6 条记录无外键依赖，可并发插入；
3. **租户隔离**：当前单租户默认 `tenant_id = 'default'`，后续多租户扩展时，此 Runner 需按租户维度循环执行；
4. **失败策略**：单条插入异常会抛出并由 Spring Boot 启动流程捕获，导致启动失败，确保问题显性化。

---

## 七、异常处理策略

### 7.1 现状与复用

`dms-common` 已提供 `GlobalExceptionHandler`（`@RestControllerAdvice`），覆盖：

- `MethodArgumentNotValidException` → 400
- `BindException` → 400
- `IllegalArgumentException` → 400
- `IllegalStateException` → 409
- `Exception` → 500

### 7.2 本模块异常规范

| 层级 | 策略 |
|------|------|
| **Controller** | 不捕获异常，统一抛给 `GlobalExceptionHandler`；所有入参必须加 `@Valid` |
| **Service** | 业务规则校验失败抛 `IllegalArgumentException`（客户端错误）或 `IllegalStateException`（冲突）；禁止直接返回 `null` 表示失败 |
| **Mapper** | 不处理异常，由 MyBatis-Plus 统一转换 |
| **数据库唯一冲突** | 尽量在 Service 层预校验，避免直接暴露底层 `DuplicateKeyException`。若因并发导致漏检，底层异常会穿透为 500，需在日志中监控 |

### 7.3 温度阈值查询接口（供 dms-equipment 调用）

`dms-equipment` 在运行时需获取"当前生效的温度阈值"。本模块提供只读查询：

```java
// DecoctSchemeService.java 中扩展
BigDecimal getAlarmHighTemp(Long schemeId);
BigDecimal getAlarmLowTemp(Long schemeId);
```

实现逻辑：

```java
@Override
public BigDecimal getAlarmHighTemp(Long schemeId) {
    DecoctScheme scheme = getById(schemeId);
    return scheme != null ? scheme.getAlarmHighTemp() : null;
}
```

> 说明：完整的"三层继承"决策逻辑（系统默认 → 设备级 → 方案级）由 `dms-equipment` 的 `EquipmentServiceImpl` 实现，本模块仅提供方案级原始值查询，保持职责单一。

---

## 八、单元测试策略

### 8.1 测试范围与目标

| 测试对象 | 目标 | 覆盖率要求 |
|---------|------|-----------|
| `HospitalServiceImpl` | CRUD、搜索、code 唯一校验、删除 | 行覆盖率 ≥ 80% |
| `DecoctSchemeServiceImpl` | CRUD、搜索、内置方案保护、code 唯一校验 | 行覆盖率 ≥ 80% |
| `DecoctSchemeInitializer` | 空表时插入、非空时跳过 | 至少覆盖 2 个分支 |
| `HospitalController` / `DecoctSchemeController` | 参数校验、HTTP 映射、异常转换 | 集成测试覆盖 |

### 8.2 测试依赖与工具

- **框架**：JUnit 5（`spring-boot-starter-test` 已含）
- **DB**：H2 内存数据库（兼容 MyBatis-Plus 与 SQLite 语法）
- **Mock**：Mockito（Service 层测试可 Mock Mapper）
- **断言**：AssertJ

### 8.3 关键测试用例示例

#### HospitalServiceImplTest

```java
@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    @Mock
    private HospitalMapper hospitalMapper;

    @InjectMocks
    private HospitalServiceImpl hospitalService;

    @Test
    void create_shouldSucceed_whenCodeIsUnique() {
        HospitalCreateRequest request = new HospitalCreateRequest();
        request.setName("测试医院");
        request.setCode("H001");

        when(hospitalMapper.selectCount(any())).thenReturn(0L);
        when(hospitalMapper.insert(any(Hospital.class))).thenAnswer(inv -> {
            Hospital h = inv.getArgument(0);
            h.setId(1L);
            return 1;
        });

        Hospital result = hospitalService.create(request);

        assertThat(result.getName()).isEqualTo("测试医院");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void create_shouldThrow_whenCodeDuplicated() {
        HospitalCreateRequest request = new HospitalCreateRequest();
        request.setCode("H001");

        when(hospitalMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> hospitalService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("医院编码已存在");
    }

    @Test
    void list_shouldApplyNameFilter() {
        hospitalService.list("人民", 1, 10);

        ArgumentCaptor<Page<Hospital>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<Hospital>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(hospitalMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());

        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10);
    }
}
```

#### DecoctSchemeServiceImplTest（内置方案保护）

```java
@ExtendWith(MockitoExtension.class)
class DecoctSchemeServiceImplTest {

    @Mock
    private DecoctSchemeMapper decoctSchemeMapper;

    @InjectMocks
    private DecoctSchemeServiceImpl decoctSchemeService;

    @Test
    void delete_shouldThrow_whenDeletingBuiltInScheme() {
        DecoctScheme builtIn = new DecoctScheme();
        builtIn.setId(1L);
        builtIn.setCode("DEFAULT_NORMAL");
        builtIn.setName("常压汤药");

        when(decoctSchemeMapper.selectById(1L)).thenReturn(builtIn);

        assertThatThrownBy(() -> decoctSchemeService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("内置方案不允许删除");
    }

    @Test
    void update_shouldThrow_whenChangingBuiltInCode() {
        DecoctScheme builtIn = new DecoctScheme();
        builtIn.setId(1L);
        builtIn.setCode("DEFAULT_NORMAL");

        SchemeUpdateRequest request = new SchemeUpdateRequest();
        request.setName("常压汤药-改");
        request.setCode("DEFAULT_CHANGED");

        when(decoctSchemeMapper.selectById(1L)).thenReturn(builtIn);

        assertThatThrownBy(() -> decoctSchemeService.update(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("内置方案不允许修改编码");
    }
}
```

#### DecoctSchemeInitializerTest

```java
@SpringBootTest
@AutoConfigureMockMvc
class DecoctSchemeInitializerTest {

    @Autowired
    private DecoctSchemeMapper decoctSchemeMapper;

    @Test
    void contextLoads_andBuiltInSchemesExist() {
        LambdaQueryWrapper<DecoctScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DecoctScheme::getCode, "DEFAULT_");
        long count = decoctSchemeMapper.selectCount(wrapper);
        assertThat(count).isEqualTo(6);
    }
}
```

### 8.4 测试数据管理

- 使用 `@Sql("/schema-test.sql")` 或 Flyway 测试迁移加载表结构；
- 每个测试方法结束后回滚事务（`@Transactional`），避免数据污染；
- 内置方案初始化测试需在空库上运行，可通过 `@TestPropertySource` 指定独立 H2 数据源。

---

## 九、迁移路径

### 9.1 迁移步骤总览

```
Step 1: 编写并放置 V5 SQL 脚本
    └── dms-app/src/main/resources/db/migration/V5__masterdata_enhance.sql

Step 2: 启动应用验证 Flyway 迁移
    └── 检查日志："Flyway validated X migrations"
    └── 检查表结构：md_hospital 有 contact/phone/address；md_decoct_scheme 有新增字段

Step 3: 修改 Hospital 实体（继承 BaseEntity，扩展字段）
    └── 同步修改 HospitalService/HospitalController，引入 DTO 与校验

Step 4: 新增 DecoctScheme 全量代码（entity/mapper/service/controller/dto）
    └── 注意 MyBatis-Plus 的自动扫描（Mapper 包需在启动类或配置中指定）

Step 5: 新增 DecoctSchemeInitializer（CommandLineRunner）
    └── 首次启动时自动插入 6 种内置方案

Step 6: 运行单元测试
    └── mvn test -pl dms-masterdata
    └── 确保 Hospital/Scheme Service 测试通过

Step 7: 集成验证
    └── 调用 POST /api/v1/md/hospitals 创建医院（含新字段）
    └── 调用 GET /api/v1/md/hospitals?name=xxx 搜索
    └── 调用 POST /api/v1/md/schemes 创建方案
    └── 调用 DELETE /api/v1/md/schemes/{内置ID} 验证保护逻辑（应 409）
```

### 9.2 回滚策略

| 场景 | 回滚操作 |
|------|---------|
| V5 迁移失败 | Flyway 自动阻止应用启动，修复 SQL 后重启 |
| 新代码异常 | 回退到上一版本 Git 标签，旧代码兼容旧表结构（旧字段仍保留） |
| 内置方案误删 | 重启应用，`DecoctSchemeInitializer` 会重新检测并补充缺失的内置方案 |
| 数据不一致 | 旧字段（`scheme_type`、`pressure` 等）在 V5 中未删除，可作为对照手工修复 |

### 9.3 兼容性说明

- **API 兼容**：`HospitalController` 的 `list` 接口新增可选 `name` 参数，旧客户端不传不影响；
- **数据库兼容**：V5 仅新增字段和索引，不删除旧字段，旧版本代码仍可读取；
- **字段映射**：V5 脚本已将旧数据映射至新字段，旧客户端直接查询数据库时新旧字段并存；
- **多租户**：当前默认 `tenant_id = 'default'`，所有唯一索引未强制联合 `tenant_id`，后续多租户改造时需将唯一索引改为 `(tenant_id, code)`。

### 9.4 检查清单（Checklist）

- [ ] `V5__masterdata_enhance.sql` 已放置于 `dms-app/src/main/resources/db/migration/`
- [ ] `Hospital` 继承 `BaseEntity`，且包含 `contact`、`phone`、`address`
- [ ] `DecoctScheme` 实体、Mapper、Service、Controller、DTO 全部新增完毕
- [ ] `HospitalServiceImpl` 中 `ensureCodeUnique` 逻辑已生效
- [ ] `DecoctSchemeServiceImpl` 中内置方案保护逻辑（禁止删除、禁止改编码）已生效
- [ ] `DecoctSchemeInitializer` 在空库下能正确插入 6 条内置方案
- [ ] 所有 Controller 入参均标注 `@Valid`
- [ ] 所有 API 返回类型为 `ApiResponse<T>`
- [ ] `mvn test -pl dms-masterdata` 通过且覆盖率 ≥ 80%
- [ ] 集成测试：医院按名称搜索、方案 CRUD、内置方案保护均验证通过

---

> **文档结束**
