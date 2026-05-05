package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.PutOnRequest;
import cn.org.openygt.production.dto.TakeOffRequest;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.Shelf;
import cn.org.openygt.production.entity.ShelfRecord;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.ShelfRecordMapper;
import cn.org.openygt.production.mapper.ShelfMapper;
import cn.org.openygt.production.service.ShelfService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.org.openygt.production.ProductionModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/shelf")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;
    private final ShelfMapper shelfMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final ShelfRecordMapper shelfRecordMapper;

    /**
     * 货架列表（适配前端成品暂存页面字段格式）。
     * 使用数据库分页，在分页结果上做字段映射和前端状态过滤。
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Integer backendStatus = convertFrontendStatus(status);
        // 使用 pageShelves 做真正的数据库分页
        IPage<Shelf> pageResult = shelfService.pageShelves(zone, backendStatus, code, page, size);
        List<Shelf> records = pageResult.getRecords();

        // 对分页结果做前端状态过滤和字段映射
        List<Map<String, Object>> list = records.stream()
                .filter(s -> {
                    if (!StringUtils.hasText(status)) return true;
                    String fs = calculateFrontendStatus(s);
                    return status.equals(fs);
                })
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", s.getShelfCode());
                    map.put("name", s.getShelfName());
                    map.put("zone", s.getAreaCode());
                    map.put("rowNo", s.getRowNum() != null ? String.format("%02d", s.getRowNum()) : "");
                    map.put("layer", s.getLayerNum() != null ? String.format("%02d", s.getLayerNum()) : "");
                    map.put("capacity", s.getCapacity());
                    map.put("current", s.getCurrentCount());
                    map.put("type", s.getShelfType());
                    map.put("status", calculateFrontendStatus(s));
                    map.put("updateTime", s.getUpdatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        return ApiResponse.success(result);
    }

    private String calculateFrontendStatus(Shelf s) {
        Integer st = s.getStatus();
        Integer cnt = s.getCurrentCount() != null ? s.getCurrentCount() : 0;
        Integer cap = s.getCapacity() != null ? s.getCapacity() : 0;
        if (st == null || st != 1) return "DISABLED";
        if (cnt == 0) return "FREE";
        if (cnt >= cap) return "FULL";
        return "IN_USE";
    }

    private Integer convertFrontendStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        // FREE/IN_USE/FULL 对应后端 status=1；DISABLED 对应后端 status=2
        if ("DISABLED".equals(status)) return 2;
        return 1;
    }

    /** 成品货架主数据分页（基础数据维护） */
    @GetMapping("/page")
    public ApiResponse<IPage<Shelf>> page(
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(shelfService.pageShelves(areaCode, status, keyword, page, size));
    }

    @PostMapping
    public ApiResponse<Shelf> create(@RequestBody Shelf shelf) {
        return ApiResponse.success(shelfService.createShelf(shelf));
    }

    @PutMapping("/{id}")
    public ApiResponse<Shelf> update(@PathVariable Long id, @RequestBody Shelf shelf) {
        return ApiResponse.success(shelfService.updateShelf(id, shelf));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shelfService.deleteShelf(id);
        return ApiResponse.success();
    }

    /**
     * 上架（适配前端成品暂存页面参数格式）。
     * 前端传：{ bagCode, prescriptionNo, patientName, shelfCode, remark }
     */
    @PostMapping("/put-on")
    public ApiResponse<ShelfRecord> putOn(@RequestBody Map<String, Object> body) {
        String bagCode = (String) body.get("bagCode");
        String prescriptionNo = (String) body.get("prescriptionNo");
        String shelfCode = (String) body.get("shelfCode");

        if (!StringUtils.hasText(bagCode)) {
            throw new IllegalArgumentException("药袋条码不能为空");
        }
        if (!StringUtils.hasText(shelfCode)) {
            throw new IllegalArgumentException("货架编码不能为空");
        }

        // 根据 shelfCode 精确查唯一货架
        LambdaQueryWrapper<Shelf> shelfWrapper = new LambdaQueryWrapper<>();
        shelfWrapper.eq(Shelf::getShelfCode, shelfCode.trim());
        Shelf shelf = shelfMapper.selectOne(shelfWrapper);
        if (shelf == null) {
            throw new IllegalArgumentException("货架不存在: " + shelfCode);
        }

        // 根据 prescriptionNo 查 prescriptionId
        Long prescriptionId = null;
        if (StringUtils.hasText(prescriptionNo)) {
            LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
            pWrapper.eq(Prescription::getPrescriptionNumber, prescriptionNo.trim()).last("LIMIT 1");
            Prescription p = prescriptionMapper.selectOne(pWrapper);
            if (p != null) {
                prescriptionId = p.getId();
            }
        }

        return ApiResponse.success(shelfService.putOn(prescriptionId, bagCode.trim(), shelf.getId(), null));
    }

    /**
     * 下架（适配前端成品暂存页面参数格式）。
     * 前端传：{ bagCode, prescriptionNo, shelfCode, reason, remark }
     */
    @PostMapping("/take-off")
    public ApiResponse<ShelfRecord> takeOff(@RequestBody Map<String, Object> body) {
        String bagCode = (String) body.get("bagCode");
        String reason = (String) body.get("reason");

        if (!StringUtils.hasText(bagCode)) {
            throw new IllegalArgumentException("药袋条码不能为空");
        }

        // 根据 bagCode（packageBarcode）查最新在架记录
        LambdaQueryWrapper<ShelfRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShelfRecord::getPackageBarcode, bagCode.trim())
               .eq(ShelfRecord::getStatus, 1)
               .orderByDesc(ShelfRecord::getCreatedAt)
               .last("LIMIT 1");
        ShelfRecord record = shelfRecordMapper.selectOne(wrapper);
        if (record == null) {
            throw new IllegalArgumentException("未找到在架记录: " + bagCode);
        }

        String takeOffType = reason != null ? reason : "PICKUP";
        return ApiResponse.success(shelfService.takeOff(record.getId(), takeOffType, null));
    }
}
