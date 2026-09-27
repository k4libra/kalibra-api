package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemMaterialStorageServiceTest {

    @TempDir
    Path root;

    @Test
    void shouldStoreUnderAnOpaqueReferenceAndLoadItBack() {
        // Arrange
        var storage = new FileSystemMaterialStorageService(root.toString());

        // Act
        var reference = storage.store("../../etc/Unit 1.PDF", new byte[]{1, 2, 3});

        // Assert
        assertThat(reference).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(root.resolve(reference)).exists();
        assertThat(storage.load(reference)).containsExactly(1, 2, 3);
    }

    @Test
    void shouldRejectReferencesThatAreNotItsOwn() {
        var storage = new FileSystemMaterialStorageService(root.toString());

        assertThatThrownBy(() -> storage.load("../secret.txt")).isInstanceOf(IllegalArgumentException.class);
    }
}
