package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum MaterialFormat {
    PDF,
    PNG,
    JPEG;

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final int PDF_HEADER_WINDOW = 1024;

    public static MaterialFormat from(String value) {
        return Arrays.stream(values())
                .filter(format -> format.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedMaterialFormatException(value));
    }

    // A file whose first bytes are not the signature of its declared format is corrupt or
    // mislabeled. A PDF may carry its header anywhere in the first 1024 bytes.
    public boolean isSignatureOf(byte[] content) {
        if (content == null) {
            return false;
        }
        return switch (this) {
            case PDF -> indexOf(content, PDF_SIGNATURE, PDF_HEADER_WINDOW) >= 0;
            case PNG -> indexOf(content, PNG_SIGNATURE, PNG_SIGNATURE.length) == 0;
            case JPEG -> indexOf(content, JPEG_SIGNATURE, JPEG_SIGNATURE.length) == 0;
        };
    }

    private static int indexOf(byte[] content, byte[] signature, int window) {
        var last = Math.min(content.length, window) - signature.length;
        for (var start = 0; start <= last; start++) {
            if (Arrays.equals(content, start, start + signature.length, signature, 0, signature.length)) {
                return start;
            }
        }
        return -1;
    }
}
