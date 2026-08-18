package com.gameon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for serving uploaded files.
 * Maps /uploads/** URL path to the uploads/ directory on disk.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded post images from the filesystem
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
