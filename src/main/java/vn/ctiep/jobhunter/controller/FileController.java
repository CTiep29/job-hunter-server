package vn.ctiep.jobhunter.controller;

import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.ctiep.jobhunter.domain.response.file.ResUploadFileDTO;
import vn.ctiep.jobhunter.service.FileService;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.error.StorageException;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;

@RestController
@RequestMapping("api/v1")
public class FileController {
    private final FileService fileService;

    @Value("${ctiep.upload-file.base-uri}")
    private String baseURI;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files")
    @ApiMessage("Upload single file")
    public ResponseEntity<ResUploadFileDTO> upload(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder)
            throws URISyntaxException, IOException, StorageException {

        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty. Please upload a file");
        }

        String fileName = file.getOriginalFilename();
        List<String> allowedExtensions = Arrays.asList("pdf", "jpg", "jpeg", "png", "doc", "docx");
        boolean isValid = allowedExtensions.stream()
                .anyMatch(item -> fileName.toLowerCase().endsWith(item));
        if (!isValid) {
            throw new StorageException("Invalid file extension. Only allows " + allowedExtensions);
        }

        // Upload lên Cloudinary thay vì lưu local
        String uploadedUrl = this.fileService.uploadToCloudinary(file);

        ResUploadFileDTO res = new ResUploadFileDTO(uploadedUrl, Instant.now());
        return ResponseEntity.ok().body(res);
    }

}
