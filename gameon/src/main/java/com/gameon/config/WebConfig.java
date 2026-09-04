package com.gameon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Web MVC configuration.
 *
 * <p>Exposes ONLY the post-image upload directory under the public URL prefix
 * {@code /uploads/posts/**}. No other filesystem location is served. Standard
 * HTTP caching headers are applied so browsers can cache uploaded images normally
 * (uploads use unique, immutable filenames).</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path postsUploadDir;
    private final Path avatarsUploadDir;

    public WebConfig(
            @Value("${gameon.uploads.posts-dir:${GAMEON_UPLOADS_POSTS_DIR:uploads/posts}}") String postsDir,
            @Value("${gameon.uploads.avatars-dir:${GAMEON_UPLOADS_AVATARS_DIR:uploads/profile-pictures}}") String avatarsDir) {
        this.postsUploadDir = Paths.get(postsDir).toAbsolutePath().normalize();
        this.avatarsUploadDir = Paths.get(avatarsDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Spring's PathResourceResolver only treats a location as a directory base when the
        // location URI ends with a slash. Path.toUri() on a not-yet-created directory returns
        // a URI WITHOUT a trailing slash (e.g. file:///C:/app/uploads/posts), which makes every
        // request under /uploads/posts/** fail to resolve (404 -> broken image icon in the browser).
        // Appending the trailing slash is the fix.
        registry.addResourceHandler("/uploads/posts/**")
                .addResourceLocations(locationOf(postsUploadDir))
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .resourceChain(true);

        // Profile pictures follow the exact same static-serving pattern as post images.
        registry.addResourceHandler("/uploads/profile-pictures/**")
                .addResourceLocations(locationOf(avatarsUploadDir))
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .resourceChain(true);
    }

    /** Builds a directory location URI with the mandatory trailing slash (see note above). */
    private String locationOf(Path dir) {
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        return location;
    }
}
