package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused unit tests for {@link ImageStorageService}: validation (extension + MIME +
 * magic bytes), storage of the original bytes with unique filenames, and safe deletion.
 */
class ImageStorageServiceTest {

    private ImageStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(tempDir.toString());
    }

    // ===== Magic byte helpers =====

    private static byte[] jpegBytes() {
        byte[] b = new byte[64];
        b[0] = (byte) 0xFF; b[1] = (byte) 0xD8; b[2] = (byte) 0xFF; b[3] = (byte) 0xE0;
        return b;
    }

    private static byte[] pngBytes() {
        byte[] b = new byte[64];
        byte[] sig = {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
        System.arraycopy(sig, 0, b, 0, sig.length);
        return b;
    }

    private static byte[] webpBytes() {
        byte[] b = new byte[64];
        byte[] riff = {'R', 'I', 'F', 'F'};
        byte[] webp = {'W', 'E', 'B', 'P'};
        System.arraycopy(riff, 0, b, 0, 4);
        System.arraycopy(webp, 0, b, 8, 4);
        return b;
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("images", name, contentType, content);
    }

    // ===== Valid stores (formats preserved, no decode) =====

    @Test
    void storesJpegPngWebpAndPreservesFormatAndBytes() throws IOException {
        String jpgPath = service.store(file("photo.jpg", "image/jpeg", jpegBytes()));
        String pngPath = service.store(file("logo.png", "image/png", pngBytes()));
        String webpPath = service.store(file("art.webp", "image/webp", webpBytes()));

        assertThat(jpgPath).startsWith("/uploads/posts/").endsWith(".jpg");
        assertThat(pngPath).endsWith(".png");
        assertThat(webpPath).endsWith(".webp");

        // Files exist and their bytes are stored unchanged (no resize/recompress/convert).
        Path stored = tempDir.resolve(jpgPath.substring("/uploads/posts/".length()));
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).isEqualTo(jpegBytes());
    }

    @Test
    void generatesUniqueFilenamesForIdenticalUploads() {
        String p1 = service.store(file("same.png", "image/png", pngBytes()));
        String p2 = service.store(file("same.png", "image/png", pngBytes()));
        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void doesNotTrustUserSuppliedFilename() {
        String path = service.store(file("../../evil.png", "image/png", pngBytes()));
        // Stored filename is a server-generated UUID, not the supplied name, and stays in the dir.
        assertThat(path).doesNotContain("evil");
        assertThat(path).doesNotContain("..");
        assertThat(path).matches("/uploads/posts/[a-f0-9]+\\.png");
    }

    // ===== Rejections =====

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> service.store(file("x.png", "image/png", new byte[0])))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[(int) (ImageStorageService.MAX_FILE_SIZE_BYTES + 1)];
        // Give it a valid PNG header so only the size check trips.
        byte[] png = pngBytes();
        System.arraycopy(png, 0, big, 0, png.length);
        assertThatThrownBy(() -> service.store(file("big.png", "image/png", big)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> service.store(file("script.gif", "image/gif", jpegBytes())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsNonImageDisguisedWithImageExtension() {
        byte[] notImage = "#!/bin/sh\necho hi".getBytes();
        assertThatThrownBy(() -> service.store(file("evil.png", "image/png", notImage)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsExtensionContentMismatch() {
        // PNG magic bytes but a .jpg extension.
        assertThatThrownBy(() -> service.store(file("mismatch.jpg", "image/jpeg", pngBytes())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsDeclaredMimeMismatch() {
        // Genuine PNG bytes + .png name, but a lying content type.
        assertThatThrownBy(() -> service.store(file("real.png", "image/webp", pngBytes())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void acceptsImageJpgMimeAlias() {
        // Browsers sometimes send image/jpg for JPEG; this should be accepted.
        String path = service.store(file("photo.jpeg", "image/jpg", jpegBytes()));
        assertThat(path).endsWith(".jpg");
    }

    // ===== Deletion =====

    @Test
    void deleteRemovesStoredFile() {
        String path = service.store(file("photo.png", "image/png", pngBytes()));
        Path stored = tempDir.resolve(path.substring("/uploads/posts/".length()));
        assertThat(Files.exists(stored)).isTrue();

        service.delete(path);
        assertThat(Files.exists(stored)).isFalse();
    }

    @Test
    void deleteIgnoresTraversalAttempts() throws IOException {
        // Create a sentinel file outside the storage root.
        Path outside = tempDir.getParent().resolve("sentinel-" + System.nanoTime() + ".txt");
        Files.writeString(outside, "keep me");
        try {
            service.delete("/uploads/posts/../../" + outside.getFileName());
            assertThat(Files.exists(outside)).isTrue();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void deleteIsSafeForNullBlankAndMissing() {
        service.delete(null);
        service.delete("");
        service.delete("/uploads/posts/does-not-exist.png");
        // No exception thrown.
        assertThat(true).isTrue();
    }
}
