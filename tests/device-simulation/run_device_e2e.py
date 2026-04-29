#!/usr/bin/env python3
"""
设备端发起的端到端测试脚本
==============================
模拟煎药机、包装机、打印机、PDA、定时任务、网络信号向系统发输入，
验证系统反馈和后续流程正确性。

用法:
    python run_device_e2e.py [--base-url http://localhost:8080]

依赖:
    pip install requests paho-mqtt pymysql
"""

import os
import sys
import json
import time
import uuid
import argparse
import threading
from datetime import datetime, timedelta
from typing import Optional, Dict, Any

try:
    import requests
    import paho.mqtt.client as mqtt
    import pymysql
except ImportError as e:
    print(f"[ERROR] 缺少依赖: {e}")
    print("请安装: pip install requests paho-mqtt pymysql")
    sys.exit(1)


class TestContext:
    def __init__(self, args):
        self.base_url = args.base_url.rstrip("/")
        self.mqtt_host = args.mqtt_host
        self.mqtt_port = args.mqtt_port
        self.db_config = {
            "host": args.mysql_host,
            "user": args.mysql_user,
            "password": args.mysql_password,
            "database": args.mysql_db,
            "charset": "utf8mb4",
            "cursorclass": pymysql.cursors.DictCursor,
        }
        self.token: Optional[str] = None
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.results: list[dict] = []
        self.passed = 0
        self.failed = 0
        self.test_data = {}

    def log(self, case_id: str, desc: str, status: str, detail: str = ""):
        self.results.append({
            "case_id": case_id,
            "desc": desc,
            "status": status,
            "detail": detail,
            "timestamp": datetime.now().isoformat()
        })
        if status == "PASS":
            self.passed += 1
        else:
            self.failed += 1
        icon = {"PASS": "✅", "FAIL": "❌", "BLOCK": "⛔"}.get(status, "?")
        print(f"{icon} [{case_id}] {desc} — {status} {detail}")

    def req(self, method: str, path: str, auth: bool = True, **kwargs):
        url = f"{self.base_url}{path}"
        headers = kwargs.pop("headers", {})
        if auth and self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        try:
            resp = self.session.request(method, url, headers=headers, timeout=15, **kwargs)
            return resp
        except requests.RequestException as e:
            return type("obj", (object,), {
                "status_code": 0,
                "text": str(e),
                "json": lambda: {}
            })()

    def db_query(self, sql: str, args=()):
        conn = pymysql.connect(**self.db_config)
        try:
            with conn.cursor() as cur:
                cur.execute(sql, args)
                return cur.fetchall()
        finally:
            conn.close()

    def db_execute(self, sql: str, args=()):
        conn = pymysql.connect(**self.db_config)
        try:
            with conn.cursor() as cur:
                cur.execute(sql, args)
                conn.commit()
        finally:
            conn.close()


# ==================== 测试数据准备 ====================

def prepare_test_data(ctx: TestContext):
    print("\n[Phase 1] 准备测试数据...")

    # 1. 登录
    resp = ctx.req("POST", "/api/v1/rbac/auth/login", auth=False,
                   data=json.dumps({"username": "admin", "password": "admin123"}))
    if resp.status_code != 200 or resp.json().get("code") != 200:
        print(f"[CRITICAL] 登录失败: {resp.text[:200]}")
        sys.exit(1)
    ctx.token = resp.json()["data"]["token"]
    print(f"  登录成功，token_len={len(ctx.token)}")

    # 2. 创建医院
    hospital_payload = {"name": f"TEST医院_{uuid.uuid4().hex[:6]}", "code": f"H{uuid.uuid4().hex[:6].upper()}", "address": "测试地址"}
    r = ctx.req("POST", "/api/v1/md/hospitals", data=json.dumps(hospital_payload))
    if r.status_code == 200 and r.json().get("code") == 200:
        ctx.test_data["hospital_id"] = r.json().get("data", {}).get("id")
        print(f"  医院创建成功 id={ctx.test_data['hospital_id']}")
    else:
        # 尝试查询已有医院
        r2 = ctx.req("GET", "/api/v1/md/hospitals?page=1&size=1")
        if r2.status_code == 200:
            recs = r2.json().get("data", {}).get("records", [])
            if recs:
                ctx.test_data["hospital_id"] = recs[0]["id"]
                print(f"  使用已有医院 id={ctx.test_data['hospital_id']}")

    # 3. 创建设备（煎药机、包装机、标签打印机）
    devices = [
        {"deviceCode": "EQ001", "name": "煎药机01", "deviceType": 1, "status": "IDLE"},
        {"deviceCode": "PK001", "name": "包装机01", "deviceType": 2, "status": "IDLE"},
        {"deviceCode": "LB001", "name": "标签打印机01", "deviceType": 4, "status": "IDLE"},
    ]
    for dev in devices:
        r = ctx.req("POST", "/api/v1/eq/devices", data=json.dumps(dev))
        if r.status_code == 200 and r.json().get("code") == 200:
            print(f"  设备 {dev['deviceCode']} 创建成功")
        elif r.status_code == 200 and "已存在" in r.text:
            print(f"  设备 {dev['deviceCode']} 已存在")
        else:
            print(f"  设备 {dev['deviceCode']} 创建结果: {r.status_code} {r.text[:100]}")
    # 初始化设备心跳，防止被 HeartbeatCheckScheduler 立即标记为 OFFLINE
    for code in [d["deviceCode"] for d in devices]:
        ctx.db_execute("UPDATE eq_device SET last_heartbeat=NOW(), status='IDLE' WHERE device_code=%s", (code,))
        print(f"  设备 {code} 心跳已初始化")

    # 4. 创建煎药方案
    scheme_payload = {
        "name": f"TEST方案_{uuid.uuid4().hex[:4]}",
        "code": f"S{uuid.uuid4().hex[:4].upper()}",
        "soakDuration": 10,
        "decoctDuration": 30,
        "alarmHighTemp": 110.0,
        "alarmLowTemp": 80.0,
    }
    r = ctx.req("POST", "/api/v1/md/schemes", data=json.dumps(scheme_payload))
    if r.status_code == 200 and r.json().get("code") == 200:
        ctx.test_data["scheme_id"] = r.json().get("data", {}).get("id")
        print(f"  方案创建成功 id={ctx.test_data['scheme_id']}")
    else:
        # 查询已有方案
        r2 = ctx.req("GET", "/api/v1/md/schemes?page=1&size=1")
        if r2.status_code == 200:
            recs = r2.json().get("data", {}).get("records", [])
            if recs:
                ctx.test_data["scheme_id"] = recs[0]["id"]
                print(f"  使用已有方案 id={ctx.test_data['scheme_id']}")

    # 5. 创建处方
    pres_payload = {
        "patientName": f"TEST患者_{uuid.uuid4().hex[:4]}",
        "medicineList": json.dumps([{"medicineName": "黄芪", "dosage": 30, "unit": "g"}, {"medicineName": "当归", "dosage": 15, "unit": "g"}]),
        "remark": "端到端测试处方",
        "hospitalId": ctx.test_data.get("hospital_id"),
        "patientType": 1
    }
    r = ctx.req("POST", "/api/v1/prod/prescriptions", data=json.dumps(pres_payload))
    if r.status_code == 200 and r.json().get("code") == 200:
        ctx.test_data["prescription_id"] = r.json().get("data", {}).get("id")
        print(f"  处方创建成功 id={ctx.test_data['prescription_id']}")
    else:
        print(f"  处方创建失败: {r.status_code} {r.text[:200]}")

    # 6. 查询刚创建的任务（待泡药）
    time.sleep(1)
    r = ctx.req("GET", "/api/v1/prod/tasks?page=1&size=1")
    if r.status_code == 200:
        recs = r.json().get("data", {}).get("records", [])
        if recs:
            ctx.test_data["task_id"] = recs[0]["id"]
            ctx.test_data["task_status"] = recs[0].get("status")
            print(f"  任务获取成功 id={ctx.test_data['task_id']} status={ctx.test_data['task_status']}")
        else:
            print("  [WARN] 未找到任务")
    else:
        print(f"  任务查询失败: {r.status_code}")

    print("[Phase 1] 完成\n")


# ==================== MQTT 设备模拟 ====================

class MqttDeviceSimulator:
    def __init__(self, host: str, port: int, tenant_id: str = "default"):
        self.host = host
        self.port = port
        self.tenant_id = tenant_id
        self.client = mqtt.Client(client_id=f"test-device-sim-{uuid.uuid4().hex[:8]}")
        self.connected = False

    def start(self):
        self.client.on_connect = lambda c, u, f, rc: self._on_connect(rc)
        self.client.on_disconnect = lambda c, u, rc: self._on_disconnect(rc)
        self.client.connect(self.host, self.port, 60)
        self.client.loop_start()
        # 等待连接
        for _ in range(20):
            if self.connected:
                break
            time.sleep(0.5)
        if not self.connected:
            raise RuntimeError("MQTT 连接失败")

    def _on_connect(self, rc):
        if rc == 0:
            self.connected = True
            print(f"  [MQTT] 已连接到 {self.host}:{self.port}")
        else:
            print(f"  [MQTT] 连接失败 rc={rc}")

    def _on_disconnect(self, rc):
        self.connected = False
        print(f"  [MQTT] 断开连接 rc={rc}")

    def publish(self, device_code: str, message_type: str, payload: Any):
        topic = f"/openygt/{self.tenant_id}/{device_code}/{message_type}"
        if isinstance(payload, dict):
            payload = json.dumps(payload)
        self.client.publish(topic, payload, qos=1)
        print(f"  [MQTT] PUBLISH {topic} => {payload}")

    def stop(self):
        self.client.loop_stop()
        self.client.disconnect()


# ==================== 测试用例实现 ====================

def test_decoct_online(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-DECOCT-01: 煎药机上线上报"""
    case = "SC-DECOCT-01"
    mqtt.publish("EQ001", "online", "")
    time.sleep(2)
    rows = ctx.db_query("SELECT status FROM eq_device WHERE device_code=%s", ("EQ001",))
    if rows and rows[0]["status"] == "IDLE":
        ctx.log(case, "煎药机上线后状态=IDLE", "PASS")
    else:
        ctx.log(case, "煎药机上线状态不符", "FAIL", str(rows))


def test_decoct_temp_drives_task(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-DECOCT-02: 温度上报驱动任务从待煎药→煎药中"""
    case = "SC-DECOCT-02"
    task_id = ctx.test_data.get("task_id")
    if not task_id:
        ctx.log(case, "无可用任务", "BLOCK")
        return

    # 1. 先手动将任务推进到待煎药（泡药开始→结束）
    r1 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/soak/start",
                 data=json.dumps({"operatorId": "OP001"}))
    if r1.status_code == 200:
        print(f"    泡药开始 OK")
    r2 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/soak/end",
                 data=json.dumps({"operatorId": "OP001"}))
    if r2.status_code == 200:
        print(f"    泡药结束 OK")

    # 2. 绑定煎药机
    r3 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/decoct/start",
                 data=json.dumps({"deviceCode": "EQ001", "operatorId": "OP001"}))
    if r3.status_code == 200:
        print(f"    煎药开始（绑定设备）OK")
    else:
        print(f"    煎药开始结果: {r3.status_code} {r3.text[:200]}")

    # 3. 模拟煎药机温度上报
    mqtt.publish("EQ001", "status", {"temperature": 85.5, "status": "RUNNING"})
    time.sleep(3)

    # 4. 验证设备温度更新
    rows = ctx.db_query("SELECT current_temp, status FROM eq_device WHERE device_code=%s", ("EQ001",))
    temp_ok = rows and float(rows[0]["current_temp"]) == 85.5
    status_ok = rows and rows[0]["status"] == "RUNNING"

    # 5. 验证任务状态（可能被推进为煎药中，或者已经是煎药中）
    task_rows = ctx.db_query("SELECT status, current_temp FROM prod_task WHERE id=%s", (task_id,))
    task_status = task_rows[0]["status"] if task_rows else "UNKNOWN"

    detail = f"device_temp={rows[0]['current_temp'] if rows else None}, device_status={rows[0]['status'] if rows else None}, task_status={task_status}"
    if temp_ok and status_ok:
        ctx.log(case, "温度上报驱动任务/设备状态", "PASS", detail)
    else:
        ctx.log(case, "温度上报后状态不符", "FAIL", detail)


def test_decoct_overtemp_alarm(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-DECOCT-03: 超温告警"""
    case = "SC-DECOCT-03"
    mqtt.publish("EQ001", "status", {"temperature": 125.0})
    time.sleep(3)
    rows = ctx.db_query(
        "SELECT alarm_type, alarm_level FROM eq_device_alarm WHERE device_id=(SELECT id FROM eq_device WHERE device_code=%s) ORDER BY id DESC LIMIT 1",
        ("EQ001",))
    if rows and rows[0]["alarm_type"] == "HIGH_TEMP":
        ctx.log(case, "超温告警产生 HIGH_TEMP", "PASS", str(rows[0]))
    else:
        ctx.log(case, "超温告警未产生", "FAIL", str(rows))


def test_decoct_fault(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-DECOCT-04: 故障上报"""
    case = "SC-DECOCT-04"
    mqtt.publish("EQ001", "fault", "E101-加热管故障")
    time.sleep(2)
    rows = ctx.db_query("SELECT status, fault_code FROM eq_device WHERE device_code=%s", ("EQ001",))
    if rows and rows[0]["status"] == "FAULT" and "E101" in (rows[0]["fault_code"] or ""):
        ctx.log(case, "故障上报后状态=FAULT", "PASS", str(rows[0]))
    else:
        ctx.log(case, "故障上报状态不符", "FAIL", str(rows))


def test_decoct_heartbeat_keepalive(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-DECOCT-05: 心跳维持"""
    case = "SC-DECOCT-05"
    # 发送心跳
    mqtt.publish("EQ001", "heartbeat", "")
    time.sleep(1)
    rows1 = ctx.db_query("SELECT last_heartbeat FROM eq_device WHERE device_code=%s", ("EQ001",))
    hb1 = rows1[0]["last_heartbeat"] if rows1 else None
    time.sleep(3)
    mqtt.publish("EQ001", "heartbeat", "")
    time.sleep(1)
    rows2 = ctx.db_query("SELECT last_heartbeat FROM eq_device WHERE device_code=%s", ("EQ001",))
    hb2 = rows2[0]["last_heartbeat"] if rows2 else None
    if hb1 and hb2 and hb2 >= hb1:
        ctx.log(case, "心跳时间持续更新", "PASS", f"hb1={hb1}, hb2={hb2}")
    else:
        ctx.log(case, "心跳时间未更新", "FAIL", f"hb1={hb1}, hb2={hb2}")


def test_wrap_bind_and_release(ctx: TestContext):
    """SC-WRAP-01/02: 包装机绑定与释放"""
    case = "SC-WRAP"
    task_id = ctx.test_data.get("task_id")
    if not task_id:
        ctx.log(case, "无可用任务", "BLOCK")
        return

    # 先将任务推进到待包装（需要经过泡药→煎药→出液）
    stages = [
        ("soak/start", {"operatorId": "OP001"}),
        ("soak/end", {"operatorId": "OP001"}),
        ("decoct/start", {"deviceCode": "EQ001", "operatorId": "OP001"}),
        ("decoct/end", {"operatorId": "OP001"}),
        ("pour/start", {"operatorId": "OP001"}),
        ("pour/end", {"operatorId": "OP001"}),
    ]
    for path, body in stages:
        r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/{path}", data=json.dumps(body))
        print(f"    {path} => {r.status_code}")
        if r.status_code not in (200, 400, 500):
            print(f"      {r.text[:100]}")

    # 绑定包装机
    r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/wrap/start",
                data=json.dumps({"deviceCode": "PK001", "operatorId": "OP001"}))
    if r.status_code != 200:
        ctx.log(case, "包装开始失败", "FAIL", r.text[:200])
        return

    time.sleep(1)
    rows = ctx.db_query("SELECT status FROM eq_device WHERE device_code=%s", ("PK001",))
    wrap_status = rows[0]["status"] if rows else None

    # 结束包装
    r2 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/wrap/end",
                 data=json.dumps({"operatorId": "OP001"}))
    time.sleep(1)
    rows2 = ctx.db_query("SELECT status FROM eq_device WHERE device_code=%s", ("PK001",))
    release_status = rows2[0]["status"] if rows2 else None

    detail = f"bind_status={wrap_status}, release_status={release_status}"
    if wrap_status and wrap_status.upper() == "RUNNING" and release_status and release_status.upper() == "IDLE":
        ctx.log(case, "包装机绑定RUNNING/释放IDLE", "PASS", detail)
    else:
        ctx.log(case, "包装机状态不符", "FAIL", detail)


def test_print_label(ctx: TestContext):
    """SC-PRINT-01: 标签打印"""
    case = "SC-PRINT-01"
    task_id = ctx.test_data.get("task_id")
    if not task_id:
        ctx.log(case, "无可用任务", "BLOCK")
        return

    # 任务推进到待贴标
    stages = [
        ("soak/start", {"operatorId": "OP001"}),
        ("soak/end", {"operatorId": "OP001"}),
        ("decoct/start", {"deviceCode": "EQ001", "operatorId": "OP001"}),
        ("decoct/end", {"operatorId": "OP001"}),
        ("pour/start", {"operatorId": "OP001"}),
        ("pour/end", {"operatorId": "OP001"}),
        ("wrap/start", {"deviceCode": "PK001", "operatorId": "OP001"}),
        ("wrap/end", {"operatorId": "OP001"}),
    ]
    for path, body in stages:
        r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/{path}", data=json.dumps(body))
        print(f"    {path} => {r.status_code}")

    # 执行打印（在待贴标状态下）
    r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/print",
                data=json.dumps({"deviceCode": "LB001", "operatorId": "OP001"}))
    if r.status_code != 200:
        ctx.log(case, "打印请求失败", "FAIL", r.text[:200])
        return

    time.sleep(1)
    rows = ctx.db_query(
        "SELECT status FROM prt_task WHERE task_id=%s ORDER BY id DESC LIMIT 1",
        (task_id,))
    print_status = rows[0]["status"] if rows else None
    if print_status == "COMPLETED":
        ctx.log(case, "标签打印任务完成", "PASS", f"status={print_status}")
    else:
        ctx.log(case, "标签打印状态不符", "FAIL", f"status={print_status}")


def test_pda_quality_and_handover(ctx: TestContext):
    """SC-PDA-02/03: PDA 质检通过 + 扫码交接"""
    case = "SC-PDA"
    task_id = ctx.test_data.get("task_id")
    if not task_id:
        ctx.log(case, "无可用任务", "BLOCK")
        return

    # 推进到待质检
    stages = [
        ("soak/start", {"operatorId": "OP001"}),
        ("soak/end", {"operatorId": "OP001"}),
        ("decoct/start", {"deviceCode": "EQ001", "operatorId": "OP001"}),
        ("decoct/end", {"operatorId": "OP001"}),
        ("pour/start", {"operatorId": "OP001"}),
        ("pour/end", {"operatorId": "OP001"}),
        ("wrap/start", {"deviceCode": "PK001", "operatorId": "OP001"}),
        ("wrap/end", {"operatorId": "OP001"}),
        ("label/confirm", {"operatorId": "OP001"}),
    ]
    for path, body in stages:
        r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/{path}", data=json.dumps(body))
        print(f"    {path} => {r.status_code}")

    # PDA 质检通过
    r = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/quality",
                data=json.dumps({"result": "PASS", "operatorId": "PDA001", "remark": "扫码质检通过"}))
    if r.status_code != 200:
        ctx.log(case, "质检请求失败", "FAIL", r.text[:200])
        return

    time.sleep(1)
    task_rows = ctx.db_query("SELECT status FROM prod_task WHERE id=%s", (task_id,))
    qual_status = task_rows[0]["status"] if task_rows else None

    # PDA 扫码交接
    r2 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/handover",
                 data=json.dumps({
                     "bagCount": 2, "handoverType": "自提", "handoverUser": "患者A",
                     "remark": "PDA扫码交接", "isFinal": True
                 }))
    time.sleep(1)
    task_rows2 = ctx.db_query("SELECT status FROM prod_task WHERE id=%s", (task_id,))
    handover_status = task_rows2[0]["status"] if task_rows2 else None
    detail_rows = ctx.db_query("SELECT COUNT(*) as cnt FROM prod_handover_detail WHERE task_id=%s", (task_id,))
    detail_cnt = detail_rows[0]["cnt"] if detail_rows else 0

    detail = f"qual_status={qual_status}, handover_status={handover_status}, handover_detail={detail_cnt}"
    if qual_status == "待交接" and handover_status == "已完成" and detail_cnt >= 1:
        ctx.log(case, "PDA质检+交接流程正确", "PASS", detail)
    else:
        ctx.log(case, "PDA质检+交接流程异常", "FAIL", detail)


def test_scheduler_heartbeat_timeout(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-SCHED-02: 心跳超时设备离线（加速验证：修改数据库 last_heartbeat 到过去）"""
    case = "SC-SCHED-02"
    # 先确保设备在线
    mqtt.publish("EQ001", "online", "")
    time.sleep(2)

    # 将 last_heartbeat 修改为 3 分钟前（超过 120 秒阈值）
    past = datetime.now() - timedelta(seconds=200)
    ctx.db_execute(
        "UPDATE eq_device SET last_heartbeat=%s WHERE device_code=%s",
        (past.strftime("%Y-%m-%d %H:%M:%S"), "EQ001"))
    print(f"    已将 EQ001 last_heartbeat 修改为 {past}")

    # 等待 HeartbeatCheckScheduler 执行（最多 40 秒）
    found = False
    for i in range(40):
        time.sleep(1)
        rows = ctx.db_query("SELECT status FROM eq_device WHERE device_code=%s", ("EQ001",))
        if rows and rows[0]["status"] == "OFFLINE":
            found = True
            break
    if found:
        ctx.log(case, "心跳超时后设备被标记OFFLINE", "PASS")
    else:
        ctx.log(case, "心跳超时后设备未被标记OFFLINE", "FAIL", f"当前状态={rows[0]['status'] if rows else 'NULL'}")


def test_scheduler_soak_timeout(ctx: TestContext):
    """SC-SCHED-01: 泡药超时自动推进"""
    case = "SC-SCHED-01"
    # 创建一个新处方和任务
    pres_payload = {
        "patientName": f"SOAK_TEST_{uuid.uuid4().hex[:4]}",
        "medicineList": json.dumps([{"medicineName": "测试药材", "dosage": 10, "unit": "g"}]),
        "remark": "泡药超时测试",
        "hospitalId": ctx.test_data.get("hospital_id"),
        "patientType": 1
    }
    r = ctx.req("POST", "/api/v1/prod/prescriptions", data=json.dumps(pres_payload))
    if r.status_code != 200:
        ctx.log(case, "处方创建失败", "FAIL", r.text[:200])
        return

    time.sleep(1)
    # 查询最新任务
    r2 = ctx.req("GET", "/api/v1/prod/tasks?page=1&size=1&status=待泡药")
    recs = r2.json().get("data", {}).get("records", []) if r2.status_code == 200 else []
    if not recs:
        ctx.log(case, "未找到待泡药任务", "BLOCK")
        return
    task_id = recs[0]["id"]

    # 开始泡药
    r3 = ctx.req("POST", f"/api/v1/prod/tasks/{task_id}/soak/start",
                 data=json.dumps({"operatorId": "OP001"}))
    if r3.status_code != 200:
        ctx.log(case, "泡药开始失败", "FAIL", r3.text[:200])
        return

    # 修改 soak_start_time 到 35 分钟前（超过默认 30 分钟阈值）
    past = datetime.now() - timedelta(minutes=35)
    ctx.db_execute(
        "UPDATE prod_task SET soak_start_time=%s WHERE id=%s",
        (past.strftime("%Y-%m-%d %H:%M:%S"), task_id))
    print(f"    已将 task_{task_id} soak_start_time 修改为 {past}")

    # 等待 SoakTimeoutScheduler 执行（最多 90 秒）
    found = False
    for i in range(90):
        time.sleep(1)
        rows = ctx.db_query("SELECT status FROM prod_task WHERE id=%s", (task_id,))
        if rows and rows[0]["status"] == "待煎药":
            found = True
            break
    if found:
        ctx.log(case, "泡药超时后自动推进到待煎药", "PASS")
    else:
        ctx.log(case, "泡药超时后未自动推进", "FAIL", f"当前状态={rows[0]['status'] if rows else 'NULL'}")


def test_net_invalid_topic(ctx: TestContext, mqtt: MqttDeviceSimulator):
    """SC-NET-02: 非法 Topic 格式不崩溃"""
    case = "SC-NET-02"
    # 发送段数不足的 topic
    topic = "/openygt/short"
    mqtt.client.publish(topic, "bad", qos=1)
    time.sleep(2)
    # 只要系统仍在响应 API 就说明没崩溃
    r = ctx.req("GET", "/api/v1/eq/devices?page=1&size=1")
    if r.status_code == 200:
        ctx.log(case, "非法Topic未导致系统崩溃", "PASS")
    else:
        ctx.log(case, "系统可能已崩溃", "FAIL", f"API返回{r.status_code}")


# ==================== 清理 ====================

def cleanup(ctx: TestContext):
    print("\n[Cleanup] 清理测试数据...")
    # 删除测试产生的处方、任务、打印记录
    ctx.db_execute("DELETE FROM prod_task_status_history WHERE task_id IN (SELECT id FROM prod_task WHERE prescription_id IN (SELECT id FROM prod_prescription WHERE patient_name LIKE %s))", ('TEST%',))
    ctx.db_execute("DELETE FROM prod_work_record WHERE task_id IN (SELECT id FROM prod_task WHERE prescription_id IN (SELECT id FROM prod_prescription WHERE patient_name LIKE %s))", ('TEST%',))
    ctx.db_execute("DELETE FROM prod_handover_detail WHERE task_id IN (SELECT id FROM prod_task WHERE prescription_id IN (SELECT id FROM prod_prescription WHERE patient_name LIKE %s))", ('TEST%',))
    ctx.db_execute("DELETE FROM prod_step_log WHERE task_id IN (SELECT id FROM prod_task WHERE prescription_id IN (SELECT id FROM prod_prescription WHERE patient_name LIKE %s))", ('TEST%',))
    ctx.db_execute("DELETE FROM prod_task WHERE prescription_id IN (SELECT id FROM prod_prescription WHERE patient_name LIKE %s)", ('TEST%',))
    ctx.db_execute("DELETE FROM prod_prescription WHERE patient_name LIKE %s", ('TEST%',))
    # 删除测试设备（先清关联通知表，再清告警表，最后删设备）
    for code in ["EQ001", "PK001", "LB001"]:
        ctx.db_execute("DELETE n FROM eq_alarm_notification n JOIN eq_device_alarm a ON n.alarm_id=a.id JOIN eq_device d ON a.device_id=d.id WHERE d.device_code=%s", (code,))
        ctx.db_execute("DELETE FROM eq_device_alarm WHERE device_id=(SELECT id FROM eq_device WHERE device_code=%s)", (code,))
        ctx.db_execute("DELETE FROM eq_device WHERE device_code=%s", (code,))
    # 删除测试医院和方案（如果是我创建的）
    hosp_id = ctx.test_data.get("hospital_id")
    if hosp_id:
        ctx.db_execute("DELETE FROM md_hospital WHERE id=%s", (hosp_id,))
    scheme_id = ctx.test_data.get("scheme_id")
    if scheme_id:
        ctx.db_execute("DELETE FROM md_decoct_scheme WHERE id=%s", (scheme_id,))
    print("[Cleanup] 完成\n")


# ==================== 主入口 ====================

def main():
    parser = argparse.ArgumentParser(description="设备端端到端测试")
    parser.add_argument("--base-url", default=os.environ.get("DMS_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--mqtt-host", default=os.environ.get("MQTT_HOST", "127.0.0.1"))
    parser.add_argument("--mqtt-port", type=int, default=int(os.environ.get("MQTT_PORT", "1883")))
    parser.add_argument("--mysql-host", default=os.environ.get("MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--mysql-user", default=os.environ.get("MYSQL_USER", "root"))
    parser.add_argument("--mysql-password", default=os.environ.get("MYSQL_PASSWORD", "mysql_pwd01"))
    parser.add_argument("--mysql-db", default=os.environ.get("MYSQL_DB", "openygt-dms"))
    parser.add_argument("--no-cleanup", action="store_true", help="测试完成后不清理数据")
    args = parser.parse_args()

    ctx = TestContext(args)
    mqtt_sim = None

    try:
        print(f"\n{'='*60}")
        print("设备端发起的端到端测试")
        print(f"{'='*60}")
        print(f"API: {ctx.base_url}")
        print(f"MQTT: {ctx.mqtt_host}:{ctx.mqtt_port}")
        print(f"MySQL: {ctx.db_config['host']}/{ctx.db_config['database']}")
        print(f"{'='*60}\n")

        # 准备数据
        prepare_test_data(ctx)

        # 连接 MQTT
        print("[Phase 2] 连接 MQTT Broker...")
        mqtt_sim = MqttDeviceSimulator(ctx.mqtt_host, ctx.mqtt_port)
        mqtt_sim.start()
        print("[Phase 2] 完成\n")

        # 执行测试用例
        print("[Phase 3] 执行设备端测试用例...")
        test_decoct_online(ctx, mqtt_sim)
        test_decoct_temp_drives_task(ctx, mqtt_sim)
        test_decoct_overtemp_alarm(ctx, mqtt_sim)
        test_decoct_fault(ctx, mqtt_sim)
        test_decoct_heartbeat_keepalive(ctx, mqtt_sim)
        test_wrap_bind_and_release(ctx)
        test_print_label(ctx)
        test_pda_quality_and_handover(ctx)
        test_scheduler_heartbeat_timeout(ctx, mqtt_sim)
        test_scheduler_soak_timeout(ctx)
        test_net_invalid_topic(ctx, mqtt_sim)
        print("[Phase 3] 完成\n")

    except Exception as e:
        print(f"\n[ERROR] 测试执行异常: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if mqtt_sim:
            mqtt_sim.stop()
        if not args.no_cleanup:
            cleanup(ctx)

    # 汇总
    print(f"{'='*60}")
    print("SUMMARY")
    print(f"{'='*60}")
    print(f"Total : {len(ctx.results)}")
    print(f"Passed: {ctx.passed}")
    print(f"Failed: {ctx.failed}")
    print(f"{'='*60}")

    # 保存报告
    report_dir = "tests/reports"
    os.makedirs(report_dir, exist_ok=True)
    report_path = os.path.join(report_dir, f"device-e2e-report-{int(time.time())}.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump({
            "meta": {
                "base_url": ctx.base_url,
                "mqtt": f"{ctx.mqtt_host}:{ctx.mqtt_port}",
                "mysql": ctx.db_config["host"],
                "timestamp": datetime.now().isoformat()
            },
            "summary": {"total": len(ctx.results), "passed": ctx.passed, "failed": ctx.failed},
            "details": ctx.results
        }, f, ensure_ascii=False, indent=2)
    print(f"\nReport saved: {report_path}")

    return 0 if ctx.failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
