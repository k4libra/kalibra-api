package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class R2MaterialStorageServiceTest {

    private static final String REFERENCE = "3f2b8c1e-7a4d-4c1b-9e2f-5d6a7b8c9d0e.pdf";

    private final R2Properties configured =
            new R2Properties("account123", "access-key", "secret-key", "kalibra-materials", Duration.ofMinutes(15));

    @Test
    void shouldPresignAPrivateHttpsUrlOfTheBucketForTheEngine() {
        var url = new R2MaterialStorageService(configured).temporaryUrl(REFERENCE);

        assertThat(url)
                .startsWith("https://account123.r2.cloudflarestorage.com/kalibra-materials/" + REFERENCE + "?")
                .contains("X-Amz-Expires=900")
                .contains("X-Amz-Signature=")
                .doesNotContain("secret-key");
    }

    @Test
    void shouldRejectAReferenceItDidNotIssue() {
        var service = new R2MaterialStorageService(configured);

        assertThatThrownBy(() -> service.temporaryUrl("../other-bucket/file.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.load(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReportTheStorageAsUnavailableWhileTheCredentialsAreMissing() {
        var service = new R2MaterialStorageService(new R2Properties("", "", "", "", Duration.ofMinutes(15)));

        assertThatThrownBy(() -> service.store("syllabus.pdf", new byte[]{1}))
                .isInstanceOf(MaterialStorageUnavailableException.class)
                .hasMessageContaining("R2_ACCOUNT_ID");
        assertThatThrownBy(() -> service.temporaryUrl(REFERENCE))
                .isInstanceOf(MaterialStorageUnavailableException.class);
    }
}
