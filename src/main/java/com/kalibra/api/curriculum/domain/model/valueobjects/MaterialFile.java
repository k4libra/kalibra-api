package com.kalibra.api.curriculum.domain.model.valueobjects;

public record MaterialFile(String fileName, MaterialFormat format, String storageReference, long sizeBytes) {

    public MaterialFile {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Material file name cannot be blank");
        }
        if (format == null) {
            throw new IllegalArgumentException("Material format cannot be null");
        }
        if (storageReference == null || storageReference.isBlank()) {
            throw new IllegalArgumentException("Material storage reference cannot be blank");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("Material file cannot be empty");
        }
        fileName = fileName.trim();
    }
}
