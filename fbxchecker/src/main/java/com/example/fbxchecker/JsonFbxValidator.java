package com.example.fbxchecker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class JsonFbxValidator {

    public static void validateJsonFile(String jsonFilePath, ValidationResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        // Проверка версии FBX
        int version = rootNode.get("version").asInt();
        String versionString = (version / 1000) + "." + (version / 100 % 10) + "." + (version % 10);
        result.addMessage("Версия FBX: " + versionString);

        // Извлечение имен моделей
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null) {
            for (JsonNode modelNode : objectsNode.get("children")) {
                if (modelNode.get("name").asText().equals("Model")) {
                    JsonNode properties = modelNode.get("properties");
                    if (properties.isArray() && properties.size() > 0) {
                        String modelName = properties.get(0).get("value").asText().split("\u0000")[0]; // Извлекаем имя модели
                        result.addMessage("Имя модели: " + modelName);
                    }
                }
            }
        } else {
            result.addMessage("Ошибка: Объект 'Objects' не найден.");
        }
    }

    // Метод для поиска узла по имени
    private static JsonNode findNodeByName(JsonNode rootNode, String name) {
        if (rootNode.isObject() && rootNode.has("name") && rootNode.get("name").asText().equals(name)) {
            return rootNode;
        }

        if (rootNode.has("children")) {
            for (JsonNode childNode : rootNode.get("children")) {
                JsonNode foundNode = findNodeByName(childNode, name);
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }
        return null;
    }
}
