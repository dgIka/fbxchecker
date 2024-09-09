package com.example.fbxchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FbxValidator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Пожалуйста, укажите путь к ZIP файлу.");
            return;
        }

        String zipFilePath = args[0];
        String jsonFilePath = args[1];
        FbxFileValidator validator = new FbxFileValidator();
        ValidationResult result = new ValidationResult();

        // Проверка ZIP файла
        if (validator.validateZipFile(zipFilePath, result)) {
            System.out.println("ZIP файл проверен.");
        }

        result.addSeparator();

        // Проверка JSON файла
        try {
            JsonFbxValidator.validateJsonFile(jsonFilePath, result);
        } catch (IOException e) {
            result.addMessage("Ошибка при чтении JSON файла: " + e.getMessage());
        }

        // Сохранение результатов проверки в файл
        try {
            Files.writeString(Path.of("validation_report.txt"), result.generateReport());
            System.out.println("Результаты проверки сохранены в файл: validation_report.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}