package com.clinic.service;

import com.clinic.exception.BusinessException;
import com.clinic.exception.NotFoundException;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded files on local disk and returns a relative path for the database.
 * This is the single place to change if storage moves to object storage later.
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(InputStream content, String folder, String fileName) {
        Path target = resolve(folder + "/" + fileName);
        try (content) {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store file", ex);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    public Resource load(String relativePath) {
        Path file = resolve(relativePath);
        if (!Files.isReadable(file)) {
            throw new NotFoundException("File", relativePath);
        }
        return new PathResource(file);
    }

    public void delete(String relativePath) {
        if (relativePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not delete file", ex);
        }
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw BusinessException.badRequest("Invalid file path");
        }
        return path;
    }
}
