package com.kalibra.api.shared.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MaterialUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement(MultipartProperties properties) {
        var fileLimit = properties.getMaxFileSize().toBytes();
        if (fileLimit > 0 && properties.getMaxRequestSize().toBytes() <= fileLimit) {
            // Multipart headers and form fields must not reduce the advertised file limit.
            properties.setMaxRequestSize(DataSize.ofBytes(Math.addExact(fileLimit, DataSize.ofMegabytes(1).toBytes())));
        }
        return properties.createMultipartConfig();
    }
}
