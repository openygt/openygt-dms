package cn.org.openygt.equipment.controller;
import cn.org.openygt.equipment.EquipmentModule;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.dto.TemperatureThresholdDTO;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.dto.TemperatureAggregationDTO;
import cn.org.openygt.equipment.dto.TemperatureLogDTO;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.service.BarcodeService;
import cn.org.openygt.equipment.service.EqDeviceMqttConfigService;
import cn.org.openygt.equipment.service.EqDeviceOperatorService;
import cn.org.openygt.equipment.service.EqDeviceService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/devices")
public class EqDeviceController {

    private final EqDeviceService deviceService;
    private final EquipmentService equipmentService;
    private final BarcodeService barcodeService;
    private final EqDeviceMqttConfigService mqttConfigService;
    private final EqDeviceOperatorService operatorService;

    public EqDeviceController(EqDeviceService deviceService,
                              EquipmentService equipmentService,
                              BarcodeService barcodeService,
                              EqDeviceMqttConfigService mqttConfigService,
                              EqDeviceOperatorService operatorService) {
        this.deviceService = deviceService;
        this.equipmentService = equipmentService;
        this.barcodeService = barcodeService;
        this.mqttConfigService = mqttConfigService;
        this.operatorService = operatorService;
    }

    // ========== 基础 CRUD ==========

    @PostMapping
    public ApiResponse<EqDevice> create(@RequestBody EqDevice device) {
        return ApiResponse.success(deviceService.create(device));
    }

    @PutMapping("/{id}")
    public ApiResponse<EqDevice> update(@PathVariable Long id, @RequestBody EqDevice device) {
        return ApiResponse.success(deviceService.update(id, device));
    }

    @GetMapping("/{id}")
    public ApiResponse<EqDevice> getById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getById(id));
    }

    @GetMapping("/code/{deviceCode}")
    public ApiResponse<EqDevice> getByCode(@PathVariable String deviceCode) {
        return ApiResponse.success(deviceService.getByCode(deviceCode));
    }

    @GetMapping
    @RequiresPermissions("eq:device:view")
    public ApiResponse<IPage<EqDevice>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer deviceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        // 数据权限：eq:device:list / 管理类角色 / prod:task:manage → 全量；否则按本人+未分配设备缩小（见 Service）
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) request.getAttribute("permissions");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("roles");
        Long currentUserId = null;
        String username = (String) request.getAttribute("username");
        boolean superUser = username != null && "admin".equalsIgnoreCase(username.trim());
        boolean fullList = permissions != null && permissions.contains("eq:device:list");
        boolean taskManage = permissions != null && permissions.contains("prod:task:manage");
        // 与前端 JWT 解析一致：ROLE_* 可能落在 roles 或 permissions；部分环境仅用户名可判管理员
        boolean adminLike = roles != null
                && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DIRECTOR") || roles.contains("ROLE_LEADER"));
        if (!adminLike && permissions != null) {
            adminLike = permissions.contains("ROLE_ADMIN") || permissions.contains("ROLE_DIRECTOR")
                    || permissions.contains("ROLE_LEADER");
        }
        boolean widen = fullList || adminLike || taskManage || superUser;
        if (!widen) {
            boolean minePerm = permissions != null && permissions.contains("eq:device:list:mine");
            boolean worker = roles != null && roles.contains("ROLE_WORKER");
            if (minePerm || worker) {
                currentUserId = (Long) request.getAttribute("userId");
            }
        }
        return ApiResponse.success(deviceService.list(keyword, deviceType, status, currentUserId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.success();
    }

    // ========== 设备详情（含关联信息）==========

    @GetMapping("/{code}/detail")
    public ApiResponse<EqDevice> getDeviceDetail(@PathVariable String code) {
        EqDevice device;
        try {
            Long id = Long.valueOf(code);
            device = deviceService.getById(id);
        } catch (NumberFormatException e) {
            device = deviceService.getByCode(code);
        }
        if (device != null) {
            // 加载MQTT配置
            device.setMqttConfig(mqttConfigService.getByDeviceCode(device.getDeviceCode()));
            // 加载当前操作人
            device.setCurrentOperator(operatorService.getCurrentByDeviceCode(device.getDeviceCode()));
        }
        return ApiResponse.success(device);
    }

    // ========== 标签打印 ==========

    @GetMapping("/{id}/label-qr")
    public ApiResponse<Map<String, String>> getQrCode(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "200") int size) {
        EqDevice device = deviceService.getById(id);
        String content = device.getDeviceCode();
        String qrBase64 = barcodeService.generateQrCode(content, size, size);
        Map<String, String> result = new HashMap<>();
        result.put("deviceCode", content);
        result.put("qrCode", qrBase64);
        result.put("deviceName", device.getName());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/label-barcode")
    public ApiResponse<Map<String, String>> getBarcode(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "300") int width,
                                                        @RequestParam(defaultValue = "100") int height) {
        EqDevice device = deviceService.getById(id);
        String content = device.getDeviceCode();
        String barcodeBase64 = barcodeService.generateBarcode(content, width, height);
        Map<String, String> result = new HashMap<>();
        result.put("deviceCode", content);
        result.put("barcode", barcodeBase64);
        result.put("deviceName", device.getName());
        return ApiResponse.success(result);
    }

    // ========== 设备预留/释放 ==========

    @PostMapping("/{id}/reserve")
    public ApiResponse<Void> reserveDevice(@PathVariable Long id,
                                           @RequestParam Long taskId) {
        equipmentService.reserveDevice(taskId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/release")
    public ApiResponse<Void> releaseDevice(@PathVariable Long id) {
        equipmentService.releaseDevice(id);
        return ApiResponse.success();
    }

    // ========== 温度阈值查询 ==========

    @GetMapping("/{id}/threshold")
    public ApiResponse<TemperatureThresholdDTO> getEffectiveThreshold(@PathVariable Long id) {
        return ApiResponse.success(equipmentService.getEffectiveThreshold(id));
    }

    // ========== 温度日志查询 ==========

    @GetMapping("/{id}/temperature-logs")
    public ApiResponse<IPage<TemperatureLogDTO>> getTemperatureLogs(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (start == null) {
            start = LocalDateTime.now().minusHours(24);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        // 直接从温度日志表查询
        return ApiResponse.success(deviceService.getTemperatureLogs(id, start, end, page, size));
    }

    // ========== 温度聚合查询 ==========

    @GetMapping("/{id}/temperature-aggregation")
    public ApiResponse<List<TemperatureAggregationDTO>> getTemperatureAggregation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1min") String interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (start == null) {
            start = LocalDateTime.now().minusHours(24);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        return ApiResponse.success(deviceService.getTemperatureAggregation(id, interval, start, end));
    }
}
