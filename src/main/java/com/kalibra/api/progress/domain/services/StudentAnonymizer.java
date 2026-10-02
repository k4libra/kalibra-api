package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.valueobjects.AnonymousStudentCode;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public class StudentAnonymizer {

    private static final String PREFIX = "STU-";
    private static final int CODE_BYTES = 5;

    // One-way and stable: the same student gets the same code in every export, and the code
    // cannot be turned back into the student id.
    public AnonymousStudentCode codeFor(StudentId studentId) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(studentId.value().toString().getBytes(StandardCharsets.UTF_8));
            var code = HexFormat.of().formatHex(digest, 0, CODE_BYTES).toUpperCase(Locale.ROOT);
            return new AnonymousStudentCode(PREFIX + code);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is not available", unavailable);
        }
    }
}
