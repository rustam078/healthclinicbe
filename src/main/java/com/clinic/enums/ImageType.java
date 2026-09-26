package com.clinic.enums;

import com.clinic.exception.BusinessException;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Logo formats accepted, detected from the file's first bytes (not its name or declared type).
 * SVG is intentionally excluded because it can carry scripts.
 */
public enum ImageType {
    PNG("png", "image/png", header -> startsWith(header, 0x89, 0x50, 0x4E, 0x47)),
    JPEG("jpg", "image/jpeg", header -> startsWith(header, 0xFF, 0xD8, 0xFF)),
    WEBP("webp", "image/webp", header -> ascii(header, 0, "RIFF") && ascii(header, 8, "WEBP"));

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private final String extension;
    private final String contentType;
    private final Predicate<byte[]> signature;

    ImageType(String extension, String contentType, Predicate<byte[]> signature) {
        this.extension = extension;
        this.contentType = contentType;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static ImageType detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Please choose an image file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw BusinessException.badRequest("The file is too large. Maximum size is 2 MB");
        }
        byte[] header = readHeader(file);
        return Arrays.stream(values()).filter(type -> type.signature.test(header)).findFirst()
                .orElseThrow(() -> BusinessException.badRequest("Only PNG, JPG or WebP images are allowed"));
    }

    public static ImageType fromPath(String path) {
        String lower = path.toLowerCase();
        return Arrays.stream(values()).filter(type -> lower.endsWith("." + type.extension)).findFirst().orElse(PNG);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(12);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static boolean startsWith(byte[] header, int... expected) {
        if (header.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] header, int offset, String text) {
        byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        return header.length >= offset + expected.length
                && Arrays.equals(Arrays.copyOfRange(header, offset, offset + expected.length), expected);
    }
}
