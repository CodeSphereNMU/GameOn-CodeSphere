package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.User;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for handling profile picture uploads.
 * Supports JPG, JPEG, PNG, and WEBP formats with a 5MB size limit.
 */
@Service
public class ProfilePictureService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );
    private static final String UPLOAD_DIR = "uploads/profile-pictures";

    private final UserRepository userRepository;

    public ProfilePictureService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Uploads a profile picture for the given user.
     * Validates file type and size, saves to disk, and updates the user entity.
     *
     * @param userId the user's ID
     * @param file   the uploaded file
     * @return the URL path to access the uploaded image
     */
    @Transactional
    public String uploadProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String filename = "user_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            // Delete old profile picture if exists
            deleteOldProfilePicture(user.getProfileImagePath());

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/profile-pictures/" + filename;
            user.setProfileImagePath(imageUrl);
            userRepository.save(user);

            logger.info("User {} uploaded new profile picture: {}", user.getUsername(), filename);
            return imageUrl;

        } catch (IOException e) {
            logger.error("Failed to save profile picture for user {}: {}", userId, e.getMessage());
            throw new BusinessRuleException("Failed to upload profile picture. Please try again.");
        }
    }

    /**
     * Gets the profile picture URL for a user, or null if none set.
     */
    @Transactional(readOnly = true)
    public String getProfilePictureUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return user.getProfileImagePath();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("No file selected. Please choose an image to upload.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("File size exceeds 5MB limit. Please choose a smaller image.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("Invalid file type. Allowed formats: JPG, JPEG, PNG, WEBP.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessRuleException("Invalid file extension. Allowed: .jpg, .jpeg, .png, .webp");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private void deleteOldProfilePicture(String oldPath) {
        if (oldPath == null || oldPath.isBlank()) {
            return;
        }
        try {
            // Convert URL path to filesystem path
            String relativePath = oldPath.replace("/uploads/", "uploads/");
            Path oldFile = Paths.get(relativePath);
            if (Files.exists(oldFile)) {
                Files.delete(oldFile);
                logger.debug("Deleted old profile picture: {}", oldPath);
            }
        } catch (IOException e) {
            logger.warn("Could not delete old profile picture {}: {}", oldPath, e.getMessage());
        }
    }
}
