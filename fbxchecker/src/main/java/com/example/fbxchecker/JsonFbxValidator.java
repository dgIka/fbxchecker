package com.example.fbxchecker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonFbxValidator {

    // Метод для проверки версии FBX и добавления сообщения в результаты
    public static void validateFbxVersion(String jsonFilePath, ValidationResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        // Проверка версии FBX
        int version = rootNode.get("version").asInt();
        String versionString = (version / 1000) + "." + (version / 100 % 10) + "." + (version % 10);
        result.addMessage("Версия FBX: " + versionString);
    }

    // Статический метод для извлечения имен объектов моделей
    public static List<String> extractModelNames(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        List<String> modelNames = new ArrayList<>();

        // Поиск узла Objects
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null) {
            for (JsonNode modelNode : objectsNode.get("children")) {
                if (modelNode.get("name").asText().equals("Model")) {
                    JsonNode properties = modelNode.get("properties");
                    if (properties.isArray() && properties.size() > 0) {
                        // Извлекаем имя модели до первого символа \u0000
                        String modelName = properties.get(1).get("value").asText().split("\u0000")[0];
                        modelNames.add(modelName);  // Добавляем имя в список
                    }
                }
            }
        }

        return modelNames;  // Возвращаем список имен моделей
    }

    // Статический метод для вычисления количества полигонов
    public static int calculatePolygonCount(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        int polygonCount = 0;

        // Поиск узла Objects
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null) {
            for (JsonNode modelNode : objectsNode.get("children")) {
                // 1. Проверяем наличие узла PolygonVertexIndex
                JsonNode polygonVertexIndexNode = findNodeByName(modelNode, "PolygonVertexIndex");
                if (polygonVertexIndexNode != null && polygonVertexIndexNode.has("properties")) {
                    for (JsonNode vertexIndex : polygonVertexIndexNode.get("properties")) {
                        int value = vertexIndex.get("value").asInt();
                        if (value < 0) { // Если значение отрицательное, это означает завершение треугольника
                            polygonCount++;
                        }
                    }
                }

                // 2. Если PolygonVertexIndex не найден, проверяем узел Vertices
                JsonNode verticesNode = findNodeByName(modelNode, "Vertices");
                if (verticesNode != null && verticesNode.has("properties")) {
                    JsonNode vertexData = verticesNode.get("properties").get(0).get("value");
                    int vertexCount = vertexData.size(); // Количество вершин
                    polygonCount += vertexCount / 3; // Каждые 3 вершины образуют один треугольник
                }
            }
        }

        return polygonCount;  // Возвращаем общее количество полигонов
    }

    // Вспомогательный метод для поиска узла по имени
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
