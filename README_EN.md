# OpenYGT-DMS Intelligent Decoction Management System

[中文](README.md) | English

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.1.0-green.svg)]()
[![Docker](https://img.shields.io/badge/docker--compose-ready-2496ED.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-6DB33F.svg)]()

A full-process intelligent management system for traditional Chinese medicine (TCM) decoction scenarios, covering the complete chain from prescription receipt to patient medication pickup.

> The system has built-in IoT device integration capabilities, supporting real-time monitoring and intelligent scheduling of decoction machines, packaging machines, label printers, and other equipment.

---

## 🚀 Demo

| Item | Address |
|------|---------|
| 🌐 Demo Site | https://demo.dms.openygt.org.cn |
| 💬 Demo Account | Contact `openygt` on WeChat for credentials |

> Demo data resets automatically at midnight. Contact `openygt` on WeChat to request a trial account.

### Feature Overview

| Module | Description |
|--------|-------------|
| 📋 Prescription Management | Full workflow: prescription entry, receipt, review, and circulation |
| 🏭 Production Command | Production dashboard, task assignment, process tracking, time monitoring |
| 🔧 Equipment Management | Equipment ledger, network monitoring, grouping, alarm center |
| ✅ Quality Inspection | Inspection records, sample retention, pass statistics |
| 📦 Dispensing Management | Temporary storage, dispensing confirmation, patient progress query |
| 🔍 Traceability Query | Prescription trace, batch trace, exception trace |
| 📱 PDA Operations | Mobile decoction operations and equipment identification |
| 🖨️ Print Center | Label printing, work order printing management |
| ⚙️ System Management | Users, roles, menus, permissions, parameter configuration |

---

## 📦 Quick Start

### Option 1: Docker One-Click Deployment (Recommended ⭐)

> For users who want to experience the system fastest, no need to install JDK, Node.js, or MySQL.
>
> Supports: Windows, macOS, Linux

**Prerequisites**
- [Docker](https://www.docker.com/products/docker-desktop) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) 2.0+

**Installation Steps**

```bash
# 1. Clone the repository
git clone https://github.com/openygt/openygt-dms.git
cd openygt-dms

# 2. Set JWT secret (at least 32 random characters, cannot contain "Test")
export JWT_SECRET="YourRandomSecretKeyAtLeast32CharactersLong"

# 3. Start all services (MySQL + Redis + Backend + Frontend)
docker compose up -d

# 4. Wait for services to be ready (first startup takes about 60-90 seconds)
docker compose logs -f backend

# 5. Access the system
# Web Admin: http://localhost
# Default account: admin / admin123
```

> **Windows Users**: Replace `export` with `set JWT_SECRET=your_secret`

**Common Commands**

```bash
# Check service status
docker compose ps

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Stop services
docker compose down

# Full cleanup (including database data)
docker compose down -v
```

---

### Option 2: Source Installation

> For developers, users who need secondary customization, or those who want to deeply understand the system.

#### Prerequisites

| Component | Version | Download |
|-----------|---------|----------|
| JDK | 8+ | [Oracle](https://www.oracle.com/java/technologies/downloads/) / [OpenJDK](https://adoptium.net/) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |
| MySQL | 8.0+ | [mysql.com](https://dev.mysql.com/downloads/) |
| Redis | 7+ | [redis.io](https://redis.io/download/) |
| Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/download.cgi) |

#### Step 1: Configure Backend

```bash
# 1. Clone the repository
git clone https://github.com/openygt/openygt-dms.git
cd openygt-dms

# 2. Copy example configuration file
cp dms-app/src/main/resources/application.yml.example \
   dms-app/src/main/resources/application.yml

# 3. Edit application.yml and modify the following configurations:
#    - spring.datasource.password: your MySQL password
#    - spring.redis.password: your Redis password (leave empty if none)
#    - jwt.secret: at least 32 random characters (cannot contain "Test")
```

#### Step 2: Create Database

```bash
# Create database using MySQL client
mysql -uroot -p -e "CREATE DATABASE openygt_dms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

#### Step 3: Start Backend

```bash
# Compile backend
mvn clean package -DskipTests

# Set environment variable and start
export JWT_SECRET="YourRandomSecretKeyAtLeast32CharactersLong"
java -jar dms-app/target/dms-app-1.0.0.jar --server.port=8080
```

After startup, Flyway will automatically run SQL scripts under `db/migration/` to create table structures.

> Seeing `Started DmsApplication` indicates successful startup.

#### Step 4: Import Demo Data (Optional)

```bash
# Import demo data for a complete experience
mysql -uroot -p openygt_dms < dms-app/src/main/resources/db/demo/init.sql
```

#### Step 5: Start Frontend

```bash
# Open a new terminal window
cd frontend
npm install
npm run dev
```

#### Step 6: Access the System

Open browser and visit: http://localhost:5173

Default account: `admin` / `admin123`

> **Windows Users**: Replace `export` with `set` in commands

---

## 🏗️ Project Structure

```
openygt-dms/
├── dms-analytics/          # Data analytics module
├── dms-app/                # Application entry point
│   └── src/main/resources/
│       ├── db/migration/   # Flyway database migration scripts
│       └── db/demo/        # Demo data
├── dms-common/             # Common module (utils, constants)
├── dms-equipment/          # Equipment management module
├── dms-inventory/          # Inventory management module
├── dms-iot-gateway/        # IoT gateway module
├── dms-masterdata/         # Master data module
├── dms-pda/                # PDA mobile terminal
├── dms-print/              # Print center module
├── dms-production/         # Production management module
├── dms-quality/            # Quality inspection module
├── dms-rbac/               # RBAC permission module
├── dms-system/             # System management module
├── frontend/               # Web frontend (Vue 3 + Vite)
│   └── src/
│       ├── api/            # API interfaces
│       ├── views/          # Page components
│       └── router/         # Route configuration
├── pda-uniapp/             # PDA mobile (uni-app)
├── docker-compose.yml      # Docker one-click deployment
├── Dockerfile.backend      # Backend image build
└── frontend/Dockerfile.frontend  # Frontend image build
```

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Frontend (Web) | Vue 3 + TypeScript + Element Plus | Vue 3.4 |
| Frontend (PDA) | uni-app (Vue 3) | — |
| Backend | Spring Boot + MyBatis-Plus | Spring Boot 2.7.18 |
| Database | MySQL | 8.0+ |
| Cache | Redis | 7+ |
| Messaging | MQTT | — |
| Build | Maven | 3.8+ |
| Deployment | Docker + Docker Compose | 20.10+ |

---

## 📖 Documentation

- **User Manual & Deployment Guide** → [openygt-docs](https://gitee.com/openygt/openygt-docs)
- **Online Reading** → https://openygt.org.cn/docs
- **Architecture Design** → [ARCHITECTURE.md](./ARCHITECTURE.md)

---

## 🤝 Contributing

Issues and Pull Requests are welcome!

- Please read [CONTRIBUTING.md](./CONTRIBUTING.md) before submitting
- PRs require signing [CLA.md](./CLA.md)
- Follow [Conventional Commits](https://www.conventionalcommits.org/) specification

---

## 📜 License

OpenYGT-DMS adopts a **dual-license model**:

| Version | License | Applicable Scenario |
|---------|---------|---------------------|
| Open Source | [AGPL-3.0](LICENSE) | Free to use, SaaS/cloud deployment requires open-sourcing modifications |
| Commercial | [Commercial License](LICENSE.commercial.md) | Hospital closed-source deployment, exempt from open-source obligations |

> For commercial licensing, please contact: luobin@openygt.org.cn

---

## 💬 Community & Support

| Channel | Scenario |
|---------|----------|
| 🐛 [GitHub Issues](https://github.com/openygt/openygt-dms/issues) | Bug reports, feature suggestions |
| 📧 luobin@openygt.org.cn | Business cooperation, device integration |
| 💬 WeChat `openygt` | Daily communication, questions |

---

<p align="center">
  <sub>Copyright © 2026 OpenYGT Contributors & Shanghai Zhoufang Intelligent Technology Co., Ltd.</sub>
</p>
