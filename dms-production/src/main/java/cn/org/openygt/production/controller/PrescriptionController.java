package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.ProductionModule;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.dto.*;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.entity.Prescription;
import lombok.extern.slf4j.Slf4j;
import cn.org.openygt.production.service.PrescriptionService;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // ==================== 原有方式（兼容） ====================

    @PostMapping
    public ApiResponse<Prescription> create(@Validated @RequestBody PrescriptionCreateRequest request) {
        return ApiResponse.success(prescriptionService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Prescription> getById(@PathVariable Long id) {
        Prescription p = prescriptionService.getDetail(id);
        return p == null ? ApiResponse.error(404, "处方不存在") : ApiResponse.success(p);
    }

    @GetMapping
    public ApiResponse<IPage<Prescription>> list(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer patientType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(prescriptionService.list(hospitalId, patientType, status, keyword, startTime, endTime, page, size));
    }

    @GetMapping("/receive-list")
    public ApiResponse<IPage<Prescription>> receiveList(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer patientType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(prescriptionService.list(hospitalId, patientType, status, keyword, startTime, endTime, page, size));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<Prescription> receive(@PathVariable Long id,
                                              @RequestParam Long operatorId,
                                              @RequestParam String operatorName) {
        try {
            return ApiResponse.success(prescriptionService.receive(id, operatorId, operatorName));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Prescription> reject(@PathVariable Long id,
                                             @Validated @RequestBody PrescriptionRejectRequest request,
                                             @RequestParam Long operatorId,
                                             @RequestParam String operatorName) {
        try {
            return ApiResponse.success(prescriptionService.reject(id, request.getRejectType(), request.getReason(), operatorId, operatorName));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<Prescription> detail(@PathVariable Long id) {
        Prescription p = prescriptionService.getDetail(id);
        return p == null ? ApiResponse.error(404, "处方不存在") : ApiResponse.success(p);
    }

    // ==================== 结构化创建 ====================

    @PostMapping("/structured")
    public ApiResponse<Prescription> createStructured(@Validated @RequestBody PrescriptionStructuredCreateRequest request) {
        try {
            return ApiResponse.success(prescriptionService.createStructured(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== CSV 导入 ====================

    @PostMapping("/import")
    public ApiResponse<List<Prescription>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "1") Integer defaultRepetition) {
        try {
            Charset charset = detectCharset(file);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            List<Prescription> created = prescriptionService.importFromCsv(sb.toString(), hospitalId, defaultRepetition);
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "CSV导入失败: " + e.getMessage());
        }
    }

    // ==================== HIS 推送 ====================

    @PostMapping("/push")
    public ApiResponse<List<Prescription>> pushFromHis(@Validated @RequestBody PrescriptionPushRequest request) {
        try {
            List<Prescription> created = request.getPrescriptions().stream()
                    .map(p -> prescriptionService.createFromPush(p, request.getHospitalCode()))
                    .collect(java.util.stream.Collectors.toList());
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/push-single")
    public ApiResponse<Prescription> pushSingleFromHis(
            @RequestParam String hospitalCode,
            @Validated @RequestBody PrescriptionPushRequest.PushPrescription prescription) {
        try {
            return ApiResponse.success(prescriptionService.createFromPush(prescription, hospitalCode));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== OCR 确认 ====================

    @PostMapping("/ocr")
    public ApiResponse<Prescription> createFromOcr(@Validated @RequestBody OcrPrescriptionRequest request) {
        try {
            return ApiResponse.success(prescriptionService.createFromOcr(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/ocr/upload")
    public ApiResponse<String> uploadOcrImage(@RequestParam("file") MultipartFile file) {
        try {
            // 保存图片，返回URL供OCR识别和确认页面使用
            String fileName = "ocr/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String savePath = "uploads/" + fileName;
            java.io.File dest = new java.io.File(savePath);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);
            return ApiResponse.success("/uploads/" + fileName);
        } catch (Exception e) {
            return ApiResponse.error(500, "图片上传失败: " + e.getMessage());
        }
    }

    // ==================== 异常处方管理 ====================

    @GetMapping("/exceptions")
    public ApiResponse<IPage<Prescription>> listExceptions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(prescriptionService.listExceptions(page, size));
    }

    @PostMapping("/{id}/resolve-exception")
    public ApiResponse<Prescription> resolveException(
            @PathVariable Long id,
            @Validated @RequestBody PrescriptionStructuredCreateRequest correctedData) {
        try {
            return ApiResponse.success(prescriptionService.resolveException(id, correctedData));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private Charset detectCharset(MultipartFile file) {
        // 简单的编码检测：尝试 UTF-8，否则 GBK
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len = reader.read(buf);
            String sample = new String(buf, 0, Math.max(len, 0));
            // 如果有常见GBK字符（如中文乱码特征），尝试GBK
            if (sample.contains("�") || sample.contains("?")) {
                return Charset.forName("GBK");
            }
        } catch (Exception e) { log.error("处方操作异常", e);
        }
        return StandardCharsets.UTF_8;
    }
}
