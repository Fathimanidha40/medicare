package com.medicare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String frontendPath = Paths.get("../frontend").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/static/",
                        frontendPath + "/",
                        "file:./frontend/",
                        "file:../frontend/"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root to index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
