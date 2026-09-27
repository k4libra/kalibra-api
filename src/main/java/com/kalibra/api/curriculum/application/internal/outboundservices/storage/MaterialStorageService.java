package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

public interface MaterialStorageService {

    String store(String fileName, byte[] content);

    byte[] load(String storageReference);
}
