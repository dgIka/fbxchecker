package com.example.fbxchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FbxValidator {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Пожалуйста, укажите путь к ZIP файлу.");
            return;
        }

        String zipFilePath = args[0];
        String jsonFilePath = args[1];
        FbxFileValidator validator = new FbxFileValidator();
        ValidationResult result = new ValidationResult();

        // Извлекаем базовое имя из пути к архиву
        String baseName = extractBaseName(zipFilePath);

        // Проверка ZIP файла
        if (validator.validateZipFile(zipFilePath, result)) {
            System.out.println(("ZIP файл проверен."));
        }

        result.addSeparator();

        // Проверка JSON файл
        try {
            JsonFbxValidator.validateFbxVersion(jsonFilePath, result);
        } catch (IOException e) {
            result.addMessage("Ошибка при чтении JSON файла: " + e.getMessage());
        }

        // Извлечение и проверка имен объектов
        try {
            List<String> modelNames = JsonFbxValidator.extractModelNames(jsonFilePath);
            ObjectNameValidator objectNameValidator = new ObjectNameValidator(baseName);

            // Выполняем проверки
            objectNameValidator.checkMainObject(modelNames, result);
            objectNameValidator.checkMainGlassObject(modelNames, result);
            objectNameValidator.checkUcObjects(modelNames, result);

        } catch (IOException e) {
            result.addMessage("Ошибка при извлечении имен моделей: " + e.getMessage());
        }

        result.addSeparator();

        JsonFbxValidator.validateMaterials(jsonFilePath, baseName, result);

        //Проверка слоев
//        try {
//            JsonFbxValidator.checkLayers(jsonFilePath, result);
//        } catch (IOException e) {
//            result.addMessage("Ошибка при проверке слоев: " + e.getMessage());
//        }

        result.addSeparator();

        int polyCount = JsonFbxValidator.calculatePolygonCountWithValidation(jsonFilePath);
        if (polyCount < 2000000) {
            result.addMessage("Количество полигонов в сцене: " + polyCount + "   OK");
        } else result.addMessage("Количество полигонов в сцене: " + polyCount + "Ошибка: количество полигонов не должно превышать 2 млн.");

        // Сохранение результатов проверки в файл
        try {
            Files.writeString(Path.of("validation_report.txt"), result.generateReport());
            System.out.println("Результаты проверки сохранены в файл: validation_report.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для извлечения базового имени из пути к архиву или файлу FBX
    private static String extractBaseName(String filePath) {
        int start = filePath.indexOf("SM_");
        int end = filePath.lastIndexOf('.');
        if (start != -1 && end != -1) {
            return filePath.substring(start + 3, end); // Извлекаем все после "SM_" и до расширения
        }
        return "";
    }
}