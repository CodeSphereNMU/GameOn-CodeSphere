package com.gameon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for handling image file uploads for posts.
 * Validates file type and size, stores with unique filenames,
 * and handles deletion of old images.
 */
@Service
public class ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

    private static final String UPLOAD_DIR = "uploads/posts";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    /**
     * Stores an uploaded image file after validation.
     *
     * @param file the uploaded MultipartFile
     * @return the relative path to the stored image (e.g., "/uploads/posts/uuid-filename.jpg")
     * @throws IllegalArgumentException if validation fails
     * @throws IOException if file storage fails
     */
    public String storeImage(MultipartFile file) throws IOException {
        validateImage(file);

        // Generate unique filename: UUID prefix + original filename
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "-" + originalFilename;

        // Ensure upload directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file
        Path targetPath = uploadPath.resolve(uniqueFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = "/" + UPLOAD_DIR + "/" + uniqueFilename;
        logger.info("Image stored: {}", relativePath);
        return relativePath;
    }

    /**
     * Deletes an image file from disk.
     *
     * @param imagePath the relative path stored in the database (e.g., "/uploads/posts/uuid-filename.jpg")
     */
    public void deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        try {
            // Remove leading slash to get relative filesystem path
            String relativePath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Image deleted: {}", imagePath);
            } else {
                logger.warn("Image file not found for deletion: {}", imagePath);
            }
        } catch (IOException e) {
            logger.error("Failed to delete image: {}", imagePath, e);
        }
    }

    /**
     * Validates the uploaded image file.
     * Checks: not empty, valid content type, valid extension, within size limit,
     * and verifies the file starts with valid image magic bytes.
     */
    private void validateImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file exceeds maximum size of 5MB.");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image format. Allowed: JPG, JPEG, PNG, WEBP.");
        }

        // Validate file extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file extension. Allowed: .jpg, .jpeg, .png, .webp");
        }

        // Validate magic bytes to prevent disguised executable uploads
        validateMagicBytes(file);
    }

    /**
     * Checks magic bytes (file signature) to ensure the file is actually an image.
     * Prevents uploading executables or scripts disguised with image extensions.
     */
    private void validateMagicBytes(MultipartFile file) throws IOException {
        byte[] bytes = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(bytes);
            if (read < 4) {
                throw new IllegalArgumentException("File is too small to be a valid image.");
            }
        }

        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return;
        }

        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 &&
            bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return;
        }

        // WEBP: RIFF....WEBP
        if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 &&
            bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
            bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 &&
            bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) {
            return;
        }

        throw new IllegalArgumentException("File content does not match a valid image format.");
    }

    /**
     * Extracts the file extension from a filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Sanitizes the filename by removing path separators and special characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image.jpg";
        }
        // Remove path info, keep only the filename
        String name = Paths.get(filename).getFileName().toString();
        // Replace any characters that aren't alphanumeric, dots, hyphens, or underscores
        return name.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
