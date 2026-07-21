package com.roadbook.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FileUploadController {

    @Value("${app.upload.dir:/opt/roadbook/uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestAttribute(value = "userId", required = false) Long userId) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String ext = file.getOriginalFilename() != null &&
                    file.getOriginalFilename().contains(".") ?
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")) : ".jpg";
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            file.transferTo(new File(dir, name));
            return ApiResponse.success(Map.of("url", "/uploads/" + name, "name", name));
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}
