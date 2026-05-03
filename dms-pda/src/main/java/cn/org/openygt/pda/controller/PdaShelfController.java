package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.ShelfMapper;
import cn.org.openygt.production.mapper.ShelfRecordMapper;
import cn.org.openygt.production.service.ShelfService;
import cn.org.openygt.production.service.TaskService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/pda")
@RequiredArgsConstructor
public class PdaShelfController {

    private final ShelfService shelfService;
    private final ShelfMapper shelfMapper;
    private final ShelfRecordMapper shelfRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final TaskService taskService;

    /**
     * 查询货架列表
     * GET /api/v1/pda/shelves?keyword=
     */
    @GetMapping("/shelves")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<List<Map<String, Object>>> listShelves(
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Shelf> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Shelf::getShelfCode, keyword)
                    .or().like(Shelf::getAreaName, keyword)
                    .or().like(Shelf::getShelfName, keyword));
        }
        wrapper.orderByAsc(Shelf::getShelfCode);
        List<Shelf> shelves = shelfMapper.selectList(wrapper);

        List<Map<String, Object>> result = shelves.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("shelfCode", s.getShelfCode());
            item.put("shelfName", s.getShelfName());
            item.put("areaCode", s.getAreaCode());
            item.put("areaName", s.getAreaName());
            item.put("capacity", s.getCapacity());
            item.put("occupied", s.getCurrentCount() != null ? s.getCurrentCount() : 0);
            return item;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 查询药袋信息
     * GET /api/v1/pda/bag/{barcode}
     */
    @GetMapping("/bag/{barcode}")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> queryBagInfo(@PathVariable String barcode) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return ApiResponse.error(404, "未找到该条码对应的任务");
        }

        // 查找在架上架记录
        LambdaQueryWrapper<ShelfRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ShelfRecord::getPackageBarcode, barcode);
        recordWrapper.eq(ShelfRecord::getStatus, 1);
        recordWrapper.orderByDesc(ShelfRecord::getCreatedAt);
        ShelfRecord record = shelfRecordMapper.selectOne(recordWrapper);

        Prescription prescription = task.getPrescriptionId() != null ?
                prescriptionMapper.selectById(task.getPrescriptionId()) : null;

        Map<String, Object> result = new HashMap<>();
        result.put("bagId", task.getId());
        result.put("barcode", barcode);
        result.put("patientName", prescription != null ? prescription.getPatientName() : "-");
        result.put("prescriptionNumber", prescription != null ? prescription.getPrescriptionNumber() : "-");
        result.put("prescriptionId", task.getPrescriptionId());
        result.put("onShelf", record != null);
        if (record != null) {
            result.put("shelfCode", record.getShelfCode());
            result.put("shelfRecordId", record.getId());
        }

        return ApiResponse.success(result);
    }

    /**
     * 上架
     * POST /api/v1/pda/shelf/put-on
     * { barcode, shelfCode }
     */
    @PostMapping("/shelf/put-on")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> putOnShelf(@RequestBody Map<String, Object> request,
                                                       @RequestAttribute("userId") Long userId) {
        String barcode = (String) request.get("barcode");
        String shelfCode = (String) request.get("shelfCode");

        if (barcode == null || shelfCode == null) {
            return ApiResponse.error(400, "barcode 和 shelfCode 不能为空");
        }

        // 查找货架
        LambdaQueryWrapper<Shelf> shelfWrapper = new LambdaQueryWrapper<>();
        shelfWrapper.eq(Shelf::getShelfCode, shelfCode);
        Shelf shelf = shelfMapper.selectOne(shelfWrapper);
        if (shelf == null) {
            return ApiResponse.error(404, "货架不存在: " + shelfCode);
        }

        // 查找任务获取处方ID
        Task task = taskService.getByBarcode(barcode);
        Long prescriptionId = task != null ? task.getPrescriptionId() : null;

        try {
            ShelfRecord record = shelfService.putOn(prescriptionId, barcode, shelf.getId(), userId);

            Map<String, Object> result = new HashMap<>();
            result.put("recordId", record.getId());
            result.put("shelfCode", shelfCode);
            result.put("barcode", barcode);
            result.put("putOnTime", record.getPutOnTime() != null ? record.getPutOnTime().toString() : "");
            return ApiResponse.success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 下架
     * POST /api/v1/pda/shelf/take-off
     * { barcode, shelfCode }
     */
    @PostMapping("/shelf/take-off")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> takeOffShelf(@RequestBody Map<String, Object> request,
                                                         @RequestAttribute("userId") Long userId) {
        String barcode = (String) request.get("barcode");

        if (barcode == null) {
            return ApiResponse.error(400, "barcode 不能为空");
        }

        // 查找在架上架记录
        LambdaQueryWrapper<ShelfRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ShelfRecord::getPackageBarcode, barcode);
        recordWrapper.eq(ShelfRecord::getStatus, 1);
        recordWrapper.orderByDesc(ShelfRecord::getCreatedAt);
        ShelfRecord record = shelfRecordMapper.selectOne(recordWrapper);

        if (record == null) {
            return ApiResponse.error(404, "未找到该药袋的上架记录");
        }

        try {
            ShelfRecord updated = shelfService.takeOff(record.getId(), "PICKUP", userId);

            Map<String, Object> result = new HashMap<>();
            result.put("recordId", updated.getId());
            result.put("barcode", barcode);
            result.put("shelfCode", record.getShelfCode());
            result.put("takeOffTime", updated.getTakeOffTime() != null ? updated.getTakeOffTime().toString() : "");
            return ApiResponse.success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
