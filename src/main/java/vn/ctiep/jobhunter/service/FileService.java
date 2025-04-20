package vn.ctiep.jobhunter.service;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.utils.ObjectUtils;
import java.nio.file.StandardCopyOption;

import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileService {
    private final Cloudinary cloudinary;

    public FileService(Cloudinary cloudinary){
        this.cloudinary = cloudinary;
    }


    @Value("${ctiep.upload-file.base-uri}")
    private String baseURI;

    public String uploadToCloudinary(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;

        String originalName = file.getOriginalFilename();
        String extension = getFileName(originalName)[1].toLowerCase();
        String publicId = generatePublicValue(originalName) + "." + extension;
        File fileToUpload = convert(file);

        Map<String, Object> options = new HashMap<>();
        options.put("public_id", publicId);

        if (extension.equals("pdf") || extension.equals("doc") || extension.equals("docx")) {
            options.put("resource_type", "auto");
        }

        Map uploadResult = cloudinary.uploader().upload(fileToUpload, options);
        cleanDisk(fileToUpload);
        return (String) uploadResult.get("secure_url");
    }

    public String uploadImage(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        String publicValue = generatePublicValue(file.getOriginalFilename());
        log.info("publicValue is: {}", publicValue);
        String extension = getFileName(file.getOriginalFilename())[1];
        log.info("extension is: {}", extension);
        File fileUpload = convert(file);
        log.info("fileUpload is: {}", fileUpload);
        cloudinary.uploader().upload(fileUpload, ObjectUtils.asMap("public_id", publicValue));
        cleanDisk(fileUpload);
        return  cloudinary.url().generate(StringUtils.join(publicValue, ".", extension));
    }

    private File convert(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        File convFile = new File(StringUtils.join(generatePublicValue(file.getOriginalFilename()), getFileName(file.getOriginalFilename())[1]));
        try(InputStream is = file.getInputStream()) {
            Files.copy(is, convFile.toPath());
        }
        return convFile;
    }

    private void cleanDisk(File file) {
        try {
            log.info("file.toPath(): {}", file.toPath());
            Path filePath = file.toPath();
            Files.delete(filePath);
        } catch (IOException e) {
            log.error("Error");
        }
    }

    public String generatePublicValue(String originalName){
        String fileName = getFileName(originalName)[0];
        return StringUtils.join(UUID.randomUUID().toString(), "_", fileName);
    }

    public String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }
}
