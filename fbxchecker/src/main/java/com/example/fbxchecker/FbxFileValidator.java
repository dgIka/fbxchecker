package com.example.fbxchecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class FbxFileValidator {
    //проверяем файлы на соответствие
    private static final long MAX_SIZE_MB = 500 * 1024 * 1024;

    public FbxFileValidator() {
    }

    public boolean validateZipFile(String filepath, ValidationResult result) {
        File file = new File(filepath);

        //проверка существования файла
        if(!file.exists()) {
            System.out.println("Файл не найден: " + filepath);
            return false;
        }

        // Проверка размера файла
        double fileSizeInMB = file.length() / (1024.0 * 1024.0);

        if (fileSizeInMB > 500) {
            result.addMessage("2. Размер архива: " + String.format("%.2f", fileSizeInMB) + " MB. Ошибка: размер файла превышает 500 MB.");
            result.addSeparator();
        } else {
            result.addMessage("2. Размер архива: " + String.format("%.2f", fileSizeInMB) + " MB.");
        }

        //проверка расширения архива
        if(!filepath.endsWith(".zip")) {
            result.addMessage("Ошибка: файл не имеет расширения .zip");
            return false;
        }


        return true;
    }

    // Метод для извлечения списка всех файлов в архиве
    public List<String> listFilesInZip(String zipFilePath) throws IOException {
        List<String> fileList = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            zipFile.stream().forEach(entry -> fileList.add(entry.getName()));
        }
        return fileList;
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

    // метод для фильтрации текстур (.png файлы) и возврата полных путей
    public List<String> extractTextureFiles(String zipFilePath, Path tempDir) throws IOException {
        List<String> textureFiles = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            zipFile.stream().forEach(entry -> {
                if (entry.getName().endsWith(".png")) {
                    textureFiles.add(tempDir.resolve(entry.getName()).toString());  // полный путь к файлу
                }
            });
        }
        return textureFiles;
    }


}
