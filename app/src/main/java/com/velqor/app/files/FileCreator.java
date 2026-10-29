package com.velqor.app.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class FileCreator {
    private FileCreator() {}

    public static class ValidationResult {
        public final boolean isValid;
        public final boolean isWarning;
        public final String message;
        public final String suggestedName;

        public ValidationResult(boolean isValid, boolean isWarning, String message, String suggestedName) {
            this.isValid = isValid;
            this.isWarning = isWarning;
            this.message = message;
            this.suggestedName = suggestedName;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, false, null, null);
        }

        public static ValidationResult warning(String msg) {
            return new ValidationResult(true, true, msg, null);
        }

        public static ValidationResult error(String msg, String suggested) {
            return new ValidationResult(false, false, msg, suggested);
        }
    }

    public static ValidationResult validateFileName(File parentDir, String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.error("Tên tệp không được để trống", null);
        }

        String trimmed = name.trim();
        if (trimmed.equals(".") || trimmed.equals("..") || trimmed.matches("^[. ]+$")) {
            return ValidationResult.error("Tên tệp không hợp lệ", null);
        }

        if (trimmed.contains("/") || trimmed.indexOf('\0') >= 0) {
            return ValidationResult.error("Tên không được chứa ký tự '/' hoặc NUL", null);
        }

        byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 255) {
            return ValidationResult.error("Tên tệp quá dài (tối đa 255 byte)", null);
        }

        if (parentDir != null && parentDir.exists()) {
            File[] existing = parentDir.listFiles();
            if (existing != null) {
                for (File f : existing) {
                    if (f.getName().equalsIgnoreCase(trimmed)) {
                        String suggested = suggestUniqueName(parentDir, trimmed);
                        return ValidationResult.error("Tên này đã tồn tại", suggested);
                    }
                }
            }
        }

        if (trimmed.startsWith(".")) {
            return ValidationResult.warning("Tệp sẽ bị ẩn vì bắt đầu bằng dấu chấm");
        }

        return ValidationResult.ok();
    }

    public static String suggestUniqueName(File parentDir, String desiredName) {
        String baseName = desiredName;
        String extension = "";
        int dotIndex = desiredName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = desiredName.substring(0, dotIndex);
            extension = desiredName.substring(dotIndex);
        }

        int count = 2;
        while (count < 1000) {
            String candidate = baseName + " (" + count + ")" + extension;
            File target = new File(parentDir, candidate);
            if (!target.exists()) {
                
                boolean existsCaseInsensitive = false;
                if (parentDir != null && parentDir.exists()) {
                    File[] existing = parentDir.listFiles();
                    if (existing != null) {
                        for (File f : existing) {
                            if (f.getName().equalsIgnoreCase(candidate)) {
                                existsCaseInsensitive = true;
                                break;
                            }
                        }
                    }
                }
                if (!existsCaseInsensitive) {
                    return candidate;
                }
            }
            count++;
        }
        return baseName + "_" + System.currentTimeMillis() + extension;
    }

    public static File createNewFile(File parentDir, String fileName) throws IOException {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            throw new IOException("Thư mục đích không tồn tại hoặc không hợp lệ");
        }
        if (!parentDir.canWrite()) {
            throw new IOException("Không có quyền ghi vào thư mục: " + parentDir.getName());
        }

        ValidationResult vr = validateFileName(parentDir, fileName);
        if (!vr.isValid) {
            throw new IOException(vr.message != null ? vr.message : "Tên tệp không hợp lệ");
        }

        File targetFile = new File(parentDir, fileName.trim());
        if (targetFile.exists()) {
            throw new IOException("Tệp đã tồn tại: " + targetFile.getName());
        }

        
        String template = getTemplateContent(targetFile.getName());
        byte[] contentBytes = template.getBytes(StandardCharsets.UTF_8);

        
        File tempFile = new File(parentDir, targetFile.getName() + ".tmp_create_" + System.currentTimeMillis());
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                if (contentBytes.length > 0) {
                    fos.write(contentBytes);
                }
                fos.flush();
            }

            if (!tempFile.renameTo(targetFile)) {
                
                FileRepository.copy(tempFile, targetFile);
                tempFile.delete();
            }

            if (!targetFile.exists()) {
                throw new IOException("Không thể hoàn tất tạo tệp mới");
            }

            return targetFile;
        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        } catch (Throwable t) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new IOException("Lỗi không mong muốn khi tạo tệp: " + t.getMessage(), t);
        }
    }

    public static String getTemplateContent(String fileName) {
        String nameLower = fileName.toLowerCase();
        int dot = fileName.lastIndexOf('.');
        String nameWithoutExt = (dot > 0) ? fileName.substring(0, dot) : fileName;

        if (nameLower.endsWith(".md")) {
            return "# " + nameWithoutExt + "\n\n";
        } else if (nameLower.endsWith(".html") || nameLower.endsWith(".htm")) {
            return "<!DOCTYPE html>\n" +
                   "<html lang=\"vi\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                   "    <title>" + nameWithoutExt + "</title>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    \n" +
                   "</body>\n" +
                   "</html>\n";
        } else if (nameLower.endsWith(".json")) {
            return "{\n    \n}\n";
        } else if (nameLower.endsWith(".sh")) {
            return "#!/bin/sh\n\n";
        } else if (nameLower.endsWith(".py")) {
            return "#!/usr/bin/env python3\n\n";
        }
        return "";
    }
}
