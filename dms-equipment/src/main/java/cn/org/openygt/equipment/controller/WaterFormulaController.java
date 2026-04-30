package cn.org.openygt.equipment.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.equipment.EquipmentModule;
import cn.org.openygt.equipment.entity.WaterFormula;
import cn.org.openygt.equipment.service.WaterFormulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EquipmentModule.API_PREFIX + "/water-formulas")
@RequiredArgsConstructor
public class WaterFormulaController {

    private final WaterFormulaService waterFormulaService;

    @GetMapping
    public ApiResponse<List<WaterFormula>> getAllFormulas() {
        return ApiResponse.success(waterFormulaService.getAllFormulas());
    }

    @PostMapping
    public ApiResponse<WaterFormula> createFormula(@RequestBody WaterFormula formula) {
        return ApiResponse.success(waterFormulaService.createFormula(formula));
    }

    @PutMapping("/{id}")
    public ApiResponse<WaterFormula> updateFormula(@PathVariable Long id, @RequestBody WaterFormula formula) {
        return ApiResponse.success(waterFormulaService.updateFormula(id, formula));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFormula(@PathVariable Long id) {
        waterFormulaService.deleteFormula(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/calculate")
    public ApiResponse<Map<String, Object>> calculate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> variables) {
        return ApiResponse.success(waterFormulaService.calculate(id, variables));
    }
}
