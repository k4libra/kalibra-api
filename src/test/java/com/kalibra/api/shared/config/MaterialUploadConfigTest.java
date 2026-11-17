package com.kalibra.api.shared.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialUploadConfigTest {

    @ParameterizedTest
    @ValueSource(ints = {20, 30})
    void shouldAllowTheFullFileLimitPlusMultipartFraming(int megabytes) {
        var properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofMegabytes(megabytes));
        properties.setMaxRequestSize(DataSize.ofMegabytes(megabytes));

        var config = new MaterialUploadConfig().multipartConfigElement(properties);

        assertThat(config.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(megabytes).toBytes());
        assertThat(config.getMaxRequestSize()).isGreaterThan(config.getMaxFileSize());
    }
}
