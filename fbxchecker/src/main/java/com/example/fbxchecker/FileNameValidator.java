package com.example.fbxchecker;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class FileNameValidator {

    // Статический метод для проверки списка файлов
    public static void validateFileNames(List<String> fileNames, String baseName, ValidationResult result) {
        // Списки для хранения разных типов файлов
        List<String> mainFiles = new ArrayList<>();
        List<String> diffuseTextures = new ArrayList<>();
        List<String> ermTextures = new ArrayList<>();
        List<String> normalTextures = new ArrayList<>();

        // Разделение файлов на группы
        for (String fileName : fileNames) {
            if (fileName.endsWith(".fbx") || fileName.endsWith(".geojson")) {
                mainFiles.add(fileName);
            } else if (fileName.endsWith(".png")) {
                if (fileName.contains("_Diffuse")) {
                    diffuseTextures.add(fileName);
                } else if (fileName.contains("_ERM")) {
                    ermTextures.add(fileName);
                } else if (fileName.contains("_Normal")) {
                    normalTextures.add(fileName);
                }
            } else {
                result.addMessage("Ошибка: неподдерживаемый формат файла - " + fileName);
            }
        }

        // Проверка файлов .fbx и .geojson
        for (String fileName : mainFiles) {
            validateMainFileName(fileName, baseName, result);
        }

        // Проверка текстур в нужном порядке
        validateTextures(diffuseTextures, baseName, result, "_Diffuse");
        validateTextures(ermTextures, baseName, result, "_ERM");
        validateTextures(normalTextures, baseName, result, "_Normal");
    }

    // Проверка основного файла (FBX или GeoJSON)
    private static void validateMainFileName(String fileName, String baseName, ValidationResult result) {
        if (fileName == null || fileName.isEmpty()) {
            result.addMessage("Ошибка: Имя файла не должно быть пустым");
            return;
        }

        if (!fileName.startsWith("SM_")) {
            result.addMessage("Ошибка: Имя файла должно начинаться с 'SM_'");
        }

        String expectedName = "SM_" + baseName;
        if (!fileName.contains(expectedName)) {
            result.addMessage("Ошибка: Имя файла должно содержать '" + expectedName + "'");
        }

        if (!(fileName.endsWith(".fbx") || fileName.endsWith(".geojson"))) {
            result.addMessage("Ошибка: Неверное расширение файла. Допустимы только .fbx и .geojson");
        } else {
            result.addMessage(fileName + ": ОК");
        }
    }

    // Проверка текстур
    private static void validateTextureFileName(String fileName, String baseName, ValidationResult result, String textureType) {
        if (!fileName.startsWith("T_" + baseName)) {
            result.addMessage("Ошибка: Текстура должна начинаться с 'T_" + baseName + "'");
            return;
        }

        // Удаляем базовое имя и префикс
        String textureInfo = fileName.substring(("T_" + baseName).length());

        // Проверяем, что название текстуры содержит нужный тип карты
        if (!textureInfo.contains(textureType)) {
            result.addMessage("Ошибка: Неверный тип текстуры в файле " + fileName + ". Ожидаемый тип: " + textureType);
            return;
        }

        // Проверяем, что есть слот материала и UDIM
        String regex = "_(1)\\.\\d{4}\\.png$";  // Например: "_1.1001.png"
        if (!Pattern.matches(".*" + regex, textureInfo)) {
            result.addMessage("Ошибка: Неверный формат текстуры в файле " + fileName + ". Ожидаемый формат: T_" + baseName + textureType + "_1.<UDIM>.png");
        } else {
            result.addMessage(fileName + ": ОК");
        }
    }

    // Метод для проверки текстур в нужном порядке
    private static void validateTextures(List<String> textures, String baseName, ValidationResult result, String textureType) {
        for (String fileName : textures) {
            validateTextureFileName(fileName, baseName, result, textureType);
        }
    }
}