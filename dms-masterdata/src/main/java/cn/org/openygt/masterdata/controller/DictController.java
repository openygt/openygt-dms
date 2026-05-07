package cn.org.openygt.masterdata.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.masterdata.MasterdataModule;
import cn.org.openygt.masterdata.entity.*;
import cn.org.openygt.masterdata.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用基础数据字典查询接口。
 *
 * <p>为前端下拉框、选择器等组件提供统一字典数据入口，避免为每个字典表单独写查询接口。</p>
 */
@RestController
@RequestMapping(MasterdataModule.API_PREFIX + "/dict")
public class DictController {

    private final DecoctMethodMapper decoctMethodMapper;
    private final ExpressCompanyMapper expressCompanyMapper;
    private final AlarmLevelMapper alarmLevelMapper;
    private final AlarmTypeMapper alarmTypeMapper;
    private final DepartmentMapper departmentMapper;
    private final DutyMapper dutyMapper;
    private final DoctorMapper doctorMapper;
    private final MeasureUnitMapper measureUnitMapper;
    private final MedicineCategoryMapper medicineCategoryMapper;
    private final PrescriptionUsageMapper prescriptionUsageMapper;
    private final DrugFootnoteMapper drugFootnoteMapper;
    private final ToxicityLevelMapper toxicityLevelMapper;

    public DictController(
            DecoctMethodMapper decoctMethodMapper,
            ExpressCompanyMapper expressCompanyMapper,
            AlarmLevelMapper alarmLevelMapper,
            AlarmTypeMapper alarmTypeMapper,
            DepartmentMapper departmentMapper,
            DutyMapper dutyMapper,
            DoctorMapper doctorMapper,
            MeasureUnitMapper measureUnitMapper,
            MedicineCategoryMapper medicineCategoryMapper,
            PrescriptionUsageMapper prescriptionUsageMapper,
            DrugFootnoteMapper drugFootnoteMapper,
            ToxicityLevelMapper toxicityLevelMapper) {
        this.decoctMethodMapper = decoctMethodMapper;
        this.expressCompanyMapper = expressCompanyMapper;
        this.alarmLevelMapper = alarmLevelMapper;
        this.alarmTypeMapper = alarmTypeMapper;
        this.departmentMapper = departmentMapper;
        this.dutyMapper = dutyMapper;
        this.doctorMapper = doctorMapper;
        this.measureUnitMapper = measureUnitMapper;
        this.medicineCategoryMapper = medicineCategoryMapper;
        this.prescriptionUsageMapper = prescriptionUsageMapper;
        this.drugFootnoteMapper = drugFootnoteMapper;
        this.toxicityLevelMapper = toxicityLevelMapper;
    }

    @GetMapping("/{type}")
    public ApiResponse<List<Map<String, Object>>> dict(@PathVariable String type) {
        switch (type) {
            case "decoct-method":
                return ApiResponse.success(decoctMethodMapper.selectList(
                        new LambdaQueryWrapper<DecoctMethod>().eq(DecoctMethod::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getMethodName())).collect(Collectors.toList()));
            case "express-company":
                return ApiResponse.success(expressCompanyMapper.selectList(
                        new LambdaQueryWrapper<ExpressCompany>().eq(ExpressCompany::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "alarm-level":
                return ApiResponse.success(alarmLevelMapper.selectList(
                        new LambdaQueryWrapper<AlarmLevel>().eq(AlarmLevel::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "alarm-type":
                return ApiResponse.success(alarmTypeMapper.selectList(
                        new LambdaQueryWrapper<AlarmType>().eq(AlarmType::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "department":
                return ApiResponse.success(departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>().eq(Department::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "duty":
                return ApiResponse.success(dutyMapper.selectList(
                        new LambdaQueryWrapper<Duty>().eq(Duty::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "doctor":
                return ApiResponse.success(doctorMapper.selectList(
                        new LambdaQueryWrapper<Doctor>().eq(Doctor::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "measure-unit":
                return ApiResponse.success(measureUnitMapper.selectList(
                        new LambdaQueryWrapper<MeasureUnit>().eq(MeasureUnit::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "medicine-category":
                return ApiResponse.success(medicineCategoryMapper.selectList(
                        new LambdaQueryWrapper<MedicineCategory>().eq(MedicineCategory::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getName())).collect(Collectors.toList()));
            case "prescription-usage":
                return ApiResponse.success(prescriptionUsageMapper.selectList(
                        new LambdaQueryWrapper<PrescriptionUsage>().eq(PrescriptionUsage::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getUsageName())).collect(Collectors.toList()));
            case "drug-footnote":
                return ApiResponse.success(drugFootnoteMapper.selectList(
                        new LambdaQueryWrapper<DrugFootnote>().eq(DrugFootnote::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getFootnoteName())).collect(Collectors.toList()));
            case "toxicity-level":
                return ApiResponse.success(toxicityLevelMapper.selectList(
                        new LambdaQueryWrapper<ToxicityLevel>().eq(ToxicityLevel::getDeleted, 0))
                        .stream().map(e -> toMap(e.getId(), e.getLevelName())).collect(Collectors.toList()));
            default:
                throw new IllegalArgumentException("未知字典类型: " + type);
        }
    }

    private Map<String, Object> toMap(Object id, Object name) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("label", name);
        map.put("value", id);
        return map;
    }
}
