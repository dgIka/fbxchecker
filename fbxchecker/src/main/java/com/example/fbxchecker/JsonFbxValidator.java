package com.example.fbxchecker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonFbxValidator {

    // Метод для проверки версии FBX и добавления сообщения в результаты
    public static void validateFbxVersion(String jsonFilePath, ValidationResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        // Проверка версии FBX
        int version = rootNode.get("version").asInt();
        String versionString = (version / 1000) + "." + (version / 100 % 10) + "." + (version % 10);
        result.addMessage("4. Версия FBX: " + versionString);
        result.addSeparator();
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

    // Статический метод для вычисления количества полигонов с проверкой индексов и дубликатов
    public static int calculatePolygonCountWithValidation(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        int polygonCount = 0;
        Set<Integer> uniqueIndices = new HashSet<>();  // Для проверки дубликатов
        Set<Integer> usedVertices = new HashSet<>();   // Для проверки индексов

        // Поиск узла Objects
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null) {
            for (JsonNode modelNode : objectsNode.get("children")) {
                // Проверяем наличие узла PolygonVertexIndex
                JsonNode polygonVertexIndexNode = findNodeByName(modelNode, "PolygonVertexIndex");
                if (polygonVertexIndexNode != null && polygonVertexIndexNode.has("properties")) {
                    // Извлекаем массив индексов полигонов
                    JsonNode indicesArray = polygonVertexIndexNode.get("properties").get(0).get("value");

                    int vertexCounter = 0; // Счетчик вершин между отрицательными значениями

                    for (JsonNode indexNode : indicesArray) {
                        int indexValue = Math.abs(indexNode.asInt());  // Берем абсолютное значение индекса для проверки
                        vertexCounter++;

                        // Проверка на уникальность индексов
//                        if (!uniqueIndices.add(indexValue)) {
//                            System.out.println("Предупреждение: Дублирующийся индекс " + indexValue);
//                        }

                        // Если отрицательное значение, это конец полигона
                        if (indexNode.asInt() < 0) {
                            if (vertexCounter > 3) {
                                // Если это многоугольник (больше 3 вершин), разбиваем его на треугольники
                                polygonCount += (vertexCounter - 2); // (n-2) треугольника для многоугольника с n вершинами
                            } else {
                                // Если это треугольник
                                polygonCount++;
                            }
                            vertexCounter = 0; // Сбрасываем счетчик вершин для следующего полигона
                        }

                        // Добавляем индекс в набор использованных вершин
                        usedVertices.add(indexValue);
                    }
                }
            }
        }

        System.out.println("Использовано вершин: " + usedVertices.size());
        return polygonCount;  // Возвращаем количество треугольников
    }

    // Метод для проверки количества слоев
    public static void checkLayers(String jsonFilePath, ValidationResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        int layerCount = countLayers(rootNode);

        // Запись результата
        if (layerCount == 1) {
            result.addMessage("Количество слоев в сцене: 1 ОК");
        } else {
            result.addMessage("Количество слоев в сцене: " + layerCount + " Ошибка");
        }
    }

    // Вспомогательный метод для рекурсивного подсчета слоев
    private static int countLayers(JsonNode node) {
        int count = 0;

        if (node.isObject() && node.has("name") && node.get("name").asText().equals("Layer")) {
            count++;
        }

        if (node.has("children")) {
            for (JsonNode childNode : node.get("children")) {
                count += countLayers(childNode);
            }
        }

        return count;
    }

    // Метод для проверки материалов
    public static void validateMaterials(String jsonFilePath, String baseName, ValidationResult result) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        List<String> foundMaterialNames = findMaterials(rootNode);

        // Определяем начальные имена материалов в зависимости от базового имени
        if (baseName.contains("_Ground")) {
            checkSequentialMaterials("M_" + baseName + "_", foundMaterialNames, result);
        } else {
            checkSequentialMaterials("M_" + baseName + "_Main_", foundMaterialNames, result);
            checkSequentialMaterials("M_" + baseName + "_MainGlass_", foundMaterialNames, result);
        }
    }

    // Метод для поиска материалов в JSON
    private static List<String> findMaterials(JsonNode rootNode) {
        List<String> materialNames = new ArrayList<>();

        // Рекурсивно ищем узлы "Material" и извлекаем имена
        if (rootNode.isObject() && rootNode.has("name") && rootNode.get("name").asText().equals("Material")) {
            String materialName = getMaterialName(rootNode);
            if (materialName != null) {
                materialNames.add(materialName);
            }
        }

        if (rootNode.has("children")) {
            for (JsonNode childNode : rootNode.get("children")) {
                materialNames.addAll(findMaterials(childNode));
            }
        }

        return materialNames;
    }

    // Извлекаем имя материала
    private static String getMaterialName(JsonNode materialNode) {
        if (materialNode.has("properties")) {
            for (JsonNode property : materialNode.get("properties")) {
                if (property.has("value") && property.get("type").asText().equals("S")) {
                    String materialName = property.get("value").asText().split("\u0000")[0];
                    return materialName.isEmpty() ? null : materialName;
                }
            }
        }
        return null;
    }

    // Метод для проверки последовательных материалов с записью индекса в файл
    private static void checkSequentialMaterials(String materialBaseName, List<String> foundMaterialNames, ValidationResult result) {
        int index = 1;  // Начинаем с _1

        while (true) {
            String expectedMaterialName = materialBaseName + index;
            if (foundMaterialNames.contains(expectedMaterialName)) {
                result.addMessage(expectedMaterialName + " ОК");
            } else {
                break;  // Если материал не найден, прекращаем проверку
            }
            index++;
        }
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
