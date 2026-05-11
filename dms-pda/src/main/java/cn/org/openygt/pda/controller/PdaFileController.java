package cn.org.openygt.pda.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.pda.service.PdaReviewPhotoService;
import cn.org.openygt.rbac.annotation.RequiresPermissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/pda")
@RequiredArgsConstructor
public class PdaFileController {

    private final PdaReviewPhotoService reviewPhotoService;

    @Value("${pda.upload.path:uploads/pda}")
    private String uploadPath;

    @PostMapping("/file/upload")
    @RequiresPermissions({"ROLE_WORKER", "ROLE_LEADER", "ROLE_INSPECTOR", "ROLE_DIRECTOR", "ROLE_ADMIN"})
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") Long taskId,
            @RequestParam(value = "photoType", defaultValue = "REVIEW") String photoType,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("username") String username,
            HttpServletRequest request) {

        if (!java.util.Arrays.asList("REVIEW", "WEIGHING", "EXCEPTION").contains(photoType)) {
            return ApiResponse.error(400, "photoType 必须是 REVIEW、WEIGHING 或 EXCEPTION");
        }

        if (file.isEmpty()) {
            return ApiResponse.error(400, "上传文件为空");
        }

        // 校验文件大小（5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ApiResponse.error(400, "文件大小超过 5MB 限制");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase() : "";
        if (!ext.matches("jpg|jpeg|png|gif")) {
            return ApiResponse.error(400, "仅支持 JPG/PNG/GIF 格式");
        }

        try {
            // 创建上传目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dirPath = Paths.get(uploadPath, dateDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path filePath = dirPath.resolve(newFilename);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 构建访问 URL
            String fileUrl = "/uploads/pda/" + dateDir + "/" + newFilename;

            // 记录到数据库
            reviewPhotoService.uploadPhoto(
                    taskId, null, fileUrl, photoType, file.getSize(),
                    userId, username, remark);

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", newFilename);
            result.put("size", file.getSize());

            log.info("PDA文件上传成功: userId={}, taskId={}, file={}", userId, taskId, newFilename);
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("PDA文件上传失败: userId={}, taskId={}", userId, taskId, e);
            return ApiResponse.error(500, "文件保存失败: " + e.getMessage());
        }
    }
}
