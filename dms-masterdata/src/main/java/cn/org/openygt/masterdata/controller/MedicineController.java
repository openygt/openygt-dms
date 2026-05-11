package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.MasterdataModule;
import cn.org.openygt.masterdata.dto.MedicineResponse;
import cn.org.openygt.masterdata.entity.Medicine;
import cn.org.openygt.masterdata.service.MedicineService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController("mdMedicineController")
@RequestMapping(MasterdataModule.API_PREFIX + "/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ApiResponse<MedicineResponse> create(@RequestBody @Valid Medicine medicine) {
        return ApiResponse.success(toResponse(medicineService.create(medicine)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicineResponse> update(@PathVariable Long id, @RequestBody Medicine medicine) {
        return ApiResponse.success(toResponse(medicineService.update(id, medicine)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicineResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(toResponse(medicineService.getById(id)));
    }

    @GetMapping
    public ApiResponse<IPage<MedicineResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Medicine> entityPage = medicineService.list(keyword, page, size);
        IPage<MedicineResponse> respPage = entityPage.convert(this::toResponse);
        return ApiResponse.success(respPage);
    }

    @GetMapping("/all")
    public ApiResponse<List<MedicineResponse>> all() {
        IPage<Medicine> entityPage = medicineService.list(null, 1, 9999);
        return ApiResponse.success(entityPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return ApiResponse.success();
    }

    private MedicineResponse toResponse(Medicine entity) {
        if (entity == null) return null;
        MedicineResponse resp = new MedicineResponse();
        resp.setId(entity.getId());
        resp.setCode(entity.getCode());
        resp.setName(entity.getName());
        resp.setCategoryId(entity.getCategoryId());
        resp.setUnitName(entity.getUnitName());
        resp.setSalePrice(entity.getSalePrice());
        resp.setModel(entity.getModel());
        resp.setDrugType(entity.getDrugType());
        resp.setIsEnabled(entity.getIsEnabled());
        resp.setPinyin(entity.getPinyin());
        resp.setEnglishName(entity.getEnglishName());
        resp.setMedicinalPart(entity.getMedicinalPart());
        resp.setProcessingMethod(entity.getProcessingMethod());
        resp.setDrugLevel(entity.getDrugLevel());
        resp.setEfficacyCategory(entity.getEfficacyCategory());
        resp.setMainUsage(entity.getMainUsage());
        resp.setStorage(entity.getStorage());
        resp.setAttention(entity.getAttention());
        resp.setDrugImg(entity.getDrugImg());
        resp.setRemark(entity.getRemark());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
