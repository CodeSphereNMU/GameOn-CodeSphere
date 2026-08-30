package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Dedicated service for post-image storage and validation.
 *
 * <p>Per the agreed approach, uploaded images are validated and then stored as-is:
 * the original bytes are written to the filesystem with a unique, server-controlled
 * filename. Images are NOT decoded, resized, recompressed or converted, so the upload
 * format (including WEBP) is preserved and no image-processing dependency is required.</p>
 *
 * <p>Validation combines three checks: file extension, declared MIME/content type, and
 * the actual leading magic bytes of the file. This defends against unsafe filenames,
 * path traversal, and non-image files disguised with an image extension.</p>
 *
 * <p>Controllers must not touch the filesystem directly; they delegate here.</p>
 */
@Service
public class ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

    /** Maximum accepted size of a single uploaded image. */
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    /** Public URL prefix under which stored post images are served. */
    public static final String PUBLIC_URL_PREFIX = "/uploads/posts/";

    /** Detected image kinds we accept. */
    private enum ImageType {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        final String extension;
        final String mimeType;

        ImageType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
    }

    /** Absolute or relative directory in which image files are stored. */
    private final Path storageRoot;

    public ImageStorageService(
            @Value("${gameon.uploads.posts-dir:${GAMEON_UPLOADS_POSTS_DIR:uploads/posts}}") String postsDir) {
        this.storageRoot = Paths.get(postsDir).toAbsolutePath().normalize();
    }

    /** Returns the absolute storage directory (created lazily on first store). */
    public Path getStorageRoot() {
        return storageRoot;
    }

    /**
     * Validates and stores a single uploaded image, returning the public URL path
     * (e.g. {@code /uploads/posts/ab12cd34ef.webp}) to persist in the database.
     *
     * @throws BusinessRuleException if the file is missing, too large, or not a
     *                               genuine JPG/PNG/WEBP image.
     */
    public String store(MultipartFile file) {
        ImageType type = validate(file);
        try {
            Files.createDirectories(storageRoot);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + type.extension;
            // Resolve + normalize + containment check: defence-in-depth against traversal.
            Path target = storageRoot.resolve(filename).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BusinessRuleException("Invalid image storage path.");
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored post image: {} ({} bytes, {})", filename, file.getSize(), type.mimeType);
            return PUBLIC_URL_PREFIX + filename;
        } catch (IOException e) {
            logger.error("Failed to store uploaded image", e);
            throw new BusinessRuleException("Could not store the uploaded image. Please try again.");
        }
    }

    /**
     * Deletes a stored image file given its public URL path (as stored in the DB).
     * Safe to call for already-missing files; never throws for cleanup failures.
     * Ignores paths that do not resolve within the managed storage directory.
     */
    public void delete(String publicPath) {
        if (publicPath == null || publicPath.isBlank()) {
            return;
        }
        try {
            String filename = extractFilename(publicPath);
            if (filename == null) {
                return;
            }
            Path target = storageRoot.resolve(filename).normalize();
            if (!target.startsWith(storageRoot)) {
                logger.warn("Refusing to delete image outside storage root: {}", publicPath);
                return;
            }
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                logger.info("Deleted post image file: {}", filename);
            }
        } catch (IOException e) {
            // Cleanup is best-effort; log and continue so it never breaks the request.
            logger.warn("Could not delete post image file for path {}: {}", publicPath, e.getMessage());
        }
    }

    /**
     * Validates extension, declared content type and magic bytes. Returns the detected
     * {@link ImageType} or throws {@link BusinessRuleException} for anything invalid.
     */
    private ImageType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("Each image must be 5 MB or smaller.");
        }

        ImageType byExtension = detectByExtension(file.getOriginalFilename());
        if (byExtension == null) {
            throw new BusinessRuleException("Unsupported image type. Allowed: JPG, JPEG, PNG, WEBP.");
        }

        ImageType bySignature = detectBySignature(file);
        if (bySignature == null) {
            throw new BusinessRuleException("File does not appear to be a valid image.");
        }

        // Extension and actual content must agree (JPEG extension maps to JPEG signature, etc.).
        if (byExtension != bySignature) {
            throw new BusinessRuleException("Image content does not match its file extension.");
        }

        // Declared MIME type, when present, must be consistent with the detected type.
        String declared = file.getContentType();
        if (declared != null && !declared.isBlank()) {
            String normalized = declared.toLowerCase(Locale.ROOT).trim();
            boolean mimeOk = normalized.equals(bySignature.mimeType)
                    // browsers sometimes send image/jpg for JPEG
                    || (bySignature == ImageType.JPEG && normalized.equals("image/jpg"));
            if (!mimeOk) {
                throw new BusinessRuleException("Declared content type does not match the image data.");
            }
        }

        return bySignature;
    }

    private ImageType detectByExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT).trim();
        return switch (ext) {
            case "jpg", "jpeg" -> ImageType.JPEG;
            case "png" -> ImageType.PNG;
            case "webp" -> ImageType.WEBP;
            default -> null;
        };
    }

    /**
     * Inspects the leading bytes of the file to determine the true image type.
     * - JPEG: FF D8 FF
     * - PNG:  89 50 4E 47 0D 0A 1A 0A
     * - WEBP: "RIFF" .... "WEBP"  (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
     */
    private ImageType detectBySignature(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (var in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return null;
        }
        if (read < 3) {
            return null;
        }

        // JPEG
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return ImageType.JPEG;
        }
        // PNG
        if (read >= 8
                && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
            return ImageType.PNG;
        }
        // WEBP: RIFF....WEBP
        if (read >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return ImageType.WEBP;
        }
        return null;
    }

    private String extractFilename(String publicPath) {
        String cleaned = publicPath.trim();
        int slash = cleaned.lastIndexOf('/');
        String name = (slash >= 0) ? cleaned.substring(slash + 1) : cleaned;
        // Reject anything that could escape the directory.
        if (name.isBlank() || name.contains("..") || name.contains("\\") || name.contains("/")) {
            return null;
        }
        return name;
    }
}
