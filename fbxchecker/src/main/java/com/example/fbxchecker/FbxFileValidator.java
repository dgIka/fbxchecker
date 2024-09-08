package com.example.fbxchecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public class FbxFileValidator {
    //проверяем файлы на соответствие
    private static final long MAX_SIZE_MB = 500 * 1024 * 1024;
    private FileNameValidator fileNameValidator;

    public FbxFileValidator() {
        this.fileNameValidator = new FileNameValidator();
    }

    public boolean validateZipFile(String filepath, ValidationResult result) {
        File file = new File(filepath);

        //проверка существования файла
        if(!file.exists()) {
            System.out.println("Файл не найден: " + filepath);
            return false;
        }

        // Проверка размера файла
        if (file.length() > MAX_SIZE_MB) {
            result.addMessage("Ошибка: размер файла превышает 500 MB.");
            return false;
        }

        //проверка расширения архива
        if(!filepath.endsWith(".zip")) {
            result.addMessage("Ошибка: файл не имеет расширения .zip");
            return false;
        }

        if(!fileNameValidator.validateFileName(file.getName(), result)) {
            result.addMessage("Ошибка: имя файла не соответствует требованиям.");
            return false;
        }
        result.addMessage("Файл проверен успешно.");
        return true;
    }

    public Path extractZipFile(String zipFilePath) throws IOException {
        // Создаем временную директорию для разархивирования
        Path tempDir = Files.createTempDirectory("tempDir_");

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            zipFile.stream().forEach(entry -> {
                try {
                    Path path = tempDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(path);
                    } else {
                        Files.createDirectories(path.getParent());
                        Files.copy(zipFile.getInputStream(entry), path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Возвращаем путь к временной директории с разархивированными файлами
        return tempDir;
    }

    public void deleteTempDirectory(Path tempDir) throws IOException {
        // Удаляем все файлы и директории во временной папке
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Сортируем для удаления вложенных файлов сначала
                .map(Path::toFile)
                .forEach(File::delete);
    }


}
