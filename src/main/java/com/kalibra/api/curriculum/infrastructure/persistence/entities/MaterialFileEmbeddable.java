package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MaterialFileEmbeddable {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(name = "storage_reference", nullable = false)
    private String storageReference;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public MaterialFileEmbeddable() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
