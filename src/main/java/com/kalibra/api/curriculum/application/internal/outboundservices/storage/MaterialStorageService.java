package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

public interface MaterialStorageService {

    String store(String fileName, byte[] content);

    byte[] load(String storageReference);

    // Short-lived HTTPS URL the adaptive engine can read the file from; the file itself stays private.
    String temporaryUrl(String storageReference);
}
