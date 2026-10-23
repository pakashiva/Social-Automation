package com.shoppingmall.service.impl;

import com.shoppingmall.config.UploadProperties;
import com.shoppingmall.exception.BusinessException;
import com.shoppingmall.repository.AppSettingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ImageFiles {

    private final UploadProperties properties;
    private final AppSettingRepository settings;

    public ImageFiles(UploadProperties properties, AppSettingRepository settings) {
        this.properties = properties;
        this.settings = settings;
    }

    public String store(String folder, long ownerId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("Upload a JPEG, PNG, or WEBP image within the size limit.");
        }
        int max = settings.findById(1L).orElseThrow().getMaxUploadBytes();
        if (bytes.length > max) {
            throw new BusinessException("Upload a JPEG, PNG, or WEBP image within the size limit.");
        }
        String extension = extension(bytes);
        String name = UUID.randomUUID() + extension;
        Path directory = Path.of(properties.getDir()).resolve(folder).resolve(Long.toString(ownerId));
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name), bytes);
        } catch (IOException exception) {
            throw new BusinessException("Upload a JPEG, PNG, or WEBP image within the size limit.");
        }
        return folder + "/" + ownerId + "/" + name;
    }

    public static String extension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89
                && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E
                && (bytes[3] & 0xFF) == 0x47) {
            return ".png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return ".webp";
        }
        throw new BusinessException("Upload a JPEG, PNG, or WEBP image within the size limit.");
    }
}
