package com.kalibra.api.curriculum.application.internal.outboundservices.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

// Cloudflare R2 through its S3-compatible API. The bucket is private: the adaptive engine reads a
// material only through a short-lived presigned URL. Clients are built on first use so the API
// starts without the credentials; until they are set, uploads answer 503.
@Service
@ConditionalOnProperty(name = "kalibra.storage.provider", havingValue = "r2", matchIfMissing = true)
@EnableConfigurationProperties(R2Properties.class)
public class R2MaterialStorageService implements MaterialStorageService {

    private static final Pattern REFERENCE = Pattern.compile("^[0-9a-f-]{36}(\\.[a-z0-9]{1,5})?$");
    private static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]{1,5}$");
    private static final Region R2_REGION = Region.of("auto");

    private final R2Properties properties;
    private S3Client client;
    private S3Presigner presigner;

    public R2MaterialStorageService(R2Properties properties) {
        this.properties = properties;
    }

    @Override
    public String store(String fileName, byte[] content) {
        var reference = UUID.randomUUID() + extensionOf(fileName);
        try {
            client().putObject(request -> request.bucket(properties.bucket()).key(reference), RequestBody.fromBytes(content));
        } catch (SdkException failure) {
            throw new MaterialStorageUnavailableException("Curricular material could not be stored", failure);
        }
        return reference;
    }

    @Override
    public byte[] load(String storageReference) {
        requireValid(storageReference);
        try {
            return client().getObjectAsBytes(request -> request.bucket(properties.bucket()).key(storageReference)).asByteArray();
        } catch (SdkException failure) {
            throw new MaterialStorageUnavailableException("Curricular material could not be loaded: " + storageReference, failure);
        }
    }

    @Override
    public String temporaryUrl(String storageReference) {
        requireValid(storageReference);
        return presigner().presignGetObject(presign -> presign
                        .signatureDuration(properties.urlValidity())
                        .getObjectRequest(request -> request.bucket(properties.bucket()).key(storageReference)))
                .url().toString();
    }

    private synchronized S3Client client() {
        if (client == null) {
            requireConfigured();
            client = S3Client.builder()
                    .endpointOverride(URI.create(properties.endpoint()))
                    .region(R2_REGION)
                    .credentialsProvider(credentials())
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return client;
    }

    private synchronized S3Presigner presigner() {
        if (presigner == null) {
            requireConfigured();
            presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(properties.endpoint()))
                    .region(R2_REGION)
                    .credentialsProvider(credentials())
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return presigner;
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey()));
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new MaterialStorageUnavailableException(
                    "Cloudflare R2 is not configured: set R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY and R2_BUCKET");
        }
    }

    private void requireValid(String storageReference) {
        if (storageReference == null || !REFERENCE.matcher(storageReference).matches()) {
            throw new IllegalArgumentException("Invalid storage reference: " + storageReference);
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') < 0) {
            return "";
        }
        var extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return EXTENSION.matcher(extension).matches() ? "." + extension : "";
    }
}
