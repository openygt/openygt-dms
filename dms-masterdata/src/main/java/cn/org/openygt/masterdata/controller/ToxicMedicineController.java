package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.entity.ToxicMedicine;
import cn.org.openygt.masterdata.service.ToxicMedicineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/base/toxic-medicine")
@RequiredArgsConstructor
public class ToxicMedicineController {

    private final ToxicMedicineService toxicMedicineService;

    @GetMapping
    public ApiResponse<IPage<ToxicMedicine>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(toxicMedicineService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<ToxicMedicine>().eq(ToxicMedicine::getIsActive, 1)));
    }

    @PostMapping
    public ApiResponse<ToxicMedicine> save(@RequestBody @Valid ToxicMedicine toxicMedicine) {
        toxicMedicineService.save(toxicMedicine);
        return ApiResponse.success(toxicMedicine);
    }

    @PutMapping("/{id}")
    public ApiResponse<ToxicMedicine> update(@PathVariable Long id, @RequestBody ToxicMedicine toxicMedicine) {
        toxicMedicine.setId(id);
        toxicMedicineService.updateById(toxicMedicine);
        return ApiResponse.success(toxicMedicine);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        toxicMedicineService.removeById(id);
        return ApiResponse.success(null);
    }
}
