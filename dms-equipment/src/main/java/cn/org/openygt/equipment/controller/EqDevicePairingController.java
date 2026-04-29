package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDevicePairing;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.service.EqDevicePairingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/pairings")
@RequiredArgsConstructor
public class EqDevicePairingController {

    private final EqDevicePairingService pairingService;
    private final EqDeviceMapper deviceMapper;

    @PostMapping
    public ApiResponse<EqDevicePairing> create(@RequestBody EqDevicePairing pairing) {
        return ApiResponse.success(pairingService.create(pairing));
    }

    @PutMapping("/{id}")
    public ApiResponse<EqDevicePairing> update(@PathVariable Long id, @RequestBody EqDevicePairing pairing) {
        return ApiResponse.success(pairingService.update(id, pairing));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pairingService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable Long id) {
        EqDevicePairing pairing = pairingService.getById(id);
        if (pairing == null) {
            return ApiResponse.error(404, "配对记录不存在");
        }
        return ApiResponse.success(enrichPairing(pairing));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<EqDevicePairing> list = pairingService.listAll();
        List<Map<String, Object>> result = list.stream()
                .map(this::enrichPairing)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    private Map<String, Object> enrichPairing(EqDevicePairing pairing) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pairing.getId());
        map.put("pairingName", pairing.getPairingName());
        map.put("status", pairing.getStatus());
        map.put("createdAt", pairing.getCreatedAt());
        map.put("updatedAt", pairing.getUpdatedAt());

        // 煎药机
        List<Map<String, Object>> decocters = new ArrayList<>();
        List<Long> decocterIdList = new ArrayList<>();
        if (pairing.getDecocterIds() != null && !pairing.getDecocterIds().isEmpty()) {
            for (String idStr : pairing.getDecocterIds().split(",")) {
                try {
                    Long did = Long.parseLong(idStr.trim());
                    decocterIdList.add(did);
                    EqDevice d = deviceMapper.selectById(did);
                    if (d != null) {
                        Map<String, Object> dm = new LinkedHashMap<>();
                        dm.put("id", d.getId());
                        dm.put("deviceCode", d.getDeviceCode());
                        dm.put("name", d.getName());
                        decocters.add(dm);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        map.put("decocters", decocters);
        map.put("decocterIds", decocterIdList);

        // 包装机
        EqDevice packer = deviceMapper.selectById(pairing.getPackerId());
        if (packer != null) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", packer.getId());
            pm.put("deviceCode", packer.getDeviceCode());
            pm.put("name", packer.getName());
            map.put("packer", pm);
        }
        map.put("packerId", pairing.getPackerId());

        // 标签打印机
        EqDevice labeler = deviceMapper.selectById(pairing.getLabelerId());
        if (labeler != null) {
            Map<String, Object> lm = new LinkedHashMap<>();
            lm.put("id", labeler.getId());
            lm.put("deviceCode", labeler.getDeviceCode());
            lm.put("name", labeler.getName());
            map.put("labeler", lm);
        }
        map.put("labelerId", pairing.getLabelerId());

        return map;
    }
}
