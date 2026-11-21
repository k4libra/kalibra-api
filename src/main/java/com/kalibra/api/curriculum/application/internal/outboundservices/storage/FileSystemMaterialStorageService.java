package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

// Local adapter for development and tests (STORAGE_PROVIDER=filesystem). The adaptive engine cannot
// read these files, so materials stored here stay pending ingestion.
@Service
@ConditionalOnProperty(name = "kalibra.storage.provider", havingValue = "filesystem")
public class FileSystemMaterialStorageService implements MaterialStorageService {

    private static final Pattern REFERENCE = Pattern.compile("^[0-9a-f-]{36}(\\.[a-z0-9]{1,5})?$");
    private static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]{1,5}$");

    private final Path root;

    public FileSystemMaterialStorageService(@Value("${kalibra.storage.materials-dir}") String materialsDir) {
        this.root = Path.of(materialsDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String fileName, byte[] content) {
        var reference = UUID.randomUUID() + extensionOf(fileName);
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(reference), content);
        } catch (IOException failure) {
            throw new UncheckedIOException("Curricular material could not be stored", failure);
        }
        return reference;
    }

    @Override
    public byte[] load(String storageReference) {
        if (storageReference == null || !REFERENCE.matcher(storageReference).matches()) {
            throw new IllegalArgumentException("Invalid storage reference: " + storageReference);
        }
        try {
            return Files.readAllBytes(root.resolve(storageReference));
        } catch (IOException failure) {
            throw new UncheckedIOException("Curricular material could not be loaded: " + storageReference, failure);
        }
    }

    @Override
    public String temporaryUrl(String storageReference) {
        throw new UnsupportedOperationException("Local material storage is not reachable by the adaptive engine; use the r2 storage provider");
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') < 0) {
            return "";
        }
        var extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return EXTENSION.matcher(extension).matches() ? "." + extension : "";
    }
}
