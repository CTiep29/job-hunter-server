package vn.ctiep.jobhunter.service;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.utils.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
        String publicId = generatePublicValue(originalName);

        File fileToUpload;

        if (extension.equals("pdf")) {
            // Nếu là PDF → convert thành PNG
            fileToUpload = convertPdfToImage(convert(file));
        } else {
            fileToUpload = convert(file);
        }

        Map<String, Object> options = new HashMap<>();
        options.put("public_id", publicId);
        options.put("type", "upload");
        options.put("resource_type", "image"); // Luôn để image vì convert xong đều là ảnh

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
    private File convertPdfToImage(File pdfFile) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        // Render trang đầu tiên của PDF thành ảnh
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300); // 300 DPI = nét

        File imageFile = File.createTempFile(UUID.randomUUID().toString(), ".png");
        ImageIO.write(bim, "png", imageFile);

        document.close();
        return imageFile;
    }
    private File convert(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        String extension = "." + getFileName(file.getOriginalFilename())[1];
        File convFile = File.createTempFile(UUID.randomUUID().toString(), extension);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, convFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
