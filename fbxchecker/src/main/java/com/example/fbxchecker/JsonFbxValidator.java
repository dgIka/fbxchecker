package com.example.fbxchecker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        if (objectsNode != null && objectsNode.has("children")) {
            for (JsonNode modelNode : objectsNode.get("children")) {
                if (modelNode.get("name").asText().equals("Model")) {
                    JsonNode properties = modelNode.get("properties");
                    if (properties != null && properties.isArray() && properties.size() > 1) {
                        // Извлекаем имя модели до первого символа \u0000
                        String modelName = properties.get(1).get("value").asText().split("\u0000")[0];
                        modelNames.add(modelName);  // Добавляем имя в список
                    }
                }
            }
        }

        return modelNames;  // Возвращаем список имен моделей
    }

    // Обновленный метод для получения релевантных моделей и их идентификаторов
    public static Map<Long, String> getRelevantModelIdNameMap(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        Map<Long, String> modelIdNameMap = new LinkedHashMap<>();
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null && objectsNode.has("children")) {
            for (JsonNode objectNode : objectsNode.get("children")) {
                if (objectNode.get("name").asText().equals("Model")) {
                    JsonNode properties = objectNode.get("properties");
                    if (properties != null && properties.isArray()) {
                        Long modelId = null;
                        String modelName = null;
                        for (JsonNode property : properties) {
                            String type = property.get("type").asText();
                            if (type.equals("L") && modelId == null) {
                                modelId = property.get("value").asLong();
                            } else if (type.equals("S") && modelName == null) {
                                modelName = property.get("value").asText().split("\u0000")[0];
                            }
                            if (modelId != null && modelName != null) {
                                break;
                            }
                        }
                        if (modelId != null && modelName != null && !modelName.startsWith("UCX")) {
                            modelIdNameMap.put(modelId, modelName);
                        }
                    }
                }
            }
        }
        return modelIdNameMap;
    }

    // Обновленный метод для получения геометрий и их идентификаторов
    public static Map<Long, JsonNode> getGeometryIdNodeMap(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        Map<Long, JsonNode> geometryIdNodeMap = new LinkedHashMap<>();
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null && objectsNode.has("children")) {
            for (JsonNode objectNode : objectsNode.get("children")) {
                if (objectNode.get("name").asText().equals("Geometry")) {
                    JsonNode properties = objectNode.get("properties");
                    if (properties != null && properties.isArray()) {
                        Long geometryId = null;
                        for (JsonNode property : properties) {
                            String type = property.get("type").asText();
                            if (type.equals("L") && geometryId == null) {
                                geometryId = property.get("value").asLong();
                                break;
                            }
                        }
                        if (geometryId != null) {
                            geometryIdNodeMap.put(geometryId, objectNode);
                        }
                    }
                }
            }
        }
        return geometryIdNodeMap;
    }

    // Обновленный метод для извлечения вершин
    public static List<double[]> extractVertices(String jsonFilePath) throws IOException {
        Map<Long, JsonNode> geometryIdNodeMap = getGeometryIdNodeMap(jsonFilePath);
        List<double[]> vertices = new ArrayList<>();

        for (JsonNode geometryNode : geometryIdNodeMap.values()) {
            List<double[]> modelVertices = extractVerticesFromGeometry(geometryNode);
            vertices.addAll(modelVertices);
        }

        return vertices;
    }

    // Обновленный метод для извлечения индексов полигонов
    public static List<int[]> extractPolygonVertexIndices(String jsonFilePath) throws IOException {
        Map<Long, JsonNode> geometryIdNodeMap = getGeometryIdNodeMap(jsonFilePath);
        List<int[]> triangleIndices = new ArrayList<>();
        int vertexOffset = 0;

        for (JsonNode geometryNode : geometryIdNodeMap.values()) {
            List<double[]> modelVertices = extractVerticesFromGeometry(geometryNode);
            List<int[]> modelTriangleIndices = extractPolygonVertexIndicesFromGeometry(geometryNode, vertexOffset);
            triangleIndices.addAll(modelTriangleIndices);
            vertexOffset += modelVertices.size();
        }

        return triangleIndices;
    }

    // Обновленный метод для извлечения UV координат
    public static List<double[]> extractUVCoords(String jsonFilePath, String uvChannelName) throws IOException {
        Map<Long, JsonNode> geometryIdNodeMap = getGeometryIdNodeMap(jsonFilePath);
        List<double[]> uvCoords = new ArrayList<>();

        for (JsonNode geometryNode : geometryIdNodeMap.values()) {
            List<double[]> modelUVCoords = extractUVCoordsFromGeometry(geometryNode, uvChannelName);
            uvCoords.addAll(modelUVCoords);
        }

        return uvCoords;
    }

    // Обновленный метод для извлечения индексов UV
    public static List<int[]> extractUVIndices(String jsonFilePath, String uvChannelName) throws IOException {
        Map<Long, JsonNode> geometryIdNodeMap = getGeometryIdNodeMap(jsonFilePath);
        List<int[]> uvIndices = new ArrayList<>();
        int uvOffset = 0;

        for (JsonNode geometryNode : geometryIdNodeMap.values()) {
            List<double[]> modelUVCoords = extractUVCoordsFromGeometry(geometryNode, uvChannelName);
            List<int[]> modelUVIndices = extractUVIndicesFromGeometry(geometryNode, uvOffset, uvChannelName);
            uvIndices.addAll(modelUVIndices);
            uvOffset += modelUVCoords.size();
        }

        return uvIndices;
    }

    // Метод для извлечения вершин из геометрии
    public static List<double[]> extractVerticesFromGeometry(JsonNode geometryNode) {
        List<double[]> vertices = new ArrayList<>();
        JsonNode verticesNode = findNodeByName(geometryNode, "Vertices");
        if (verticesNode != null && verticesNode.has("properties")) {
            for (JsonNode property : verticesNode.get("properties")) {
                if (property.get("type").asText().equals("d")) {
                    JsonNode valueNode = property.get("value");
                    if (valueNode.isArray()) {
                        for (int i = 0; i < valueNode.size(); i += 3) {
                            double x = valueNode.get(i).asDouble();
                            double y = valueNode.get(i + 1).asDouble();
                            double z = valueNode.get(i + 2).asDouble();
                            vertices.add(new double[]{x, y, z});
                        }
                    }
                }
            }
        }
        return vertices;
    }

    // Метод для извлечения индексов полигонов из геометрии
    public static List<int[]> extractPolygonVertexIndicesFromGeometry(JsonNode geometryNode, int vertexOffset) {
        List<int[]> triangleIndices = new ArrayList<>();
        JsonNode polygonVertexIndexNode = findNodeByName(geometryNode, "PolygonVertexIndex");
        if (polygonVertexIndexNode != null && polygonVertexIndexNode.has("properties")) {
            for (JsonNode property : polygonVertexIndexNode.get("properties")) {
                if (property.get("type").asText().equals("i")) {
                    JsonNode valueNode = property.get("value");
                    if (valueNode.isArray()) {
                        List<Integer> indices = new ArrayList<>();
                        for (JsonNode indexNode : valueNode) {
                            indices.add(indexNode.asInt());
                        }
                        // Обработка индексов для извлечения треугольников
                        List<Integer> faceIndices = new ArrayList<>();
                        for (int indexValue : indices) {
                            int correctedIndex = (indexValue < 0) ? -indexValue - 1 : indexValue;
                            faceIndices.add(correctedIndex + vertexOffset);
                            if (indexValue < 0) {
                                // Конец полигона
                                if (faceIndices.size() == 3) {
                                    triangleIndices.add(new int[]{
                                            faceIndices.get(0),
                                            faceIndices.get(1),
                                            faceIndices.get(2)
                                    });
                                } else if (faceIndices.size() > 3) {
                                    // Триангуляция многоугольника
                                    for (int j = 1; j < faceIndices.size() - 1; j++) {
                                        triangleIndices.add(new int[]{
                                                faceIndices.get(0),
                                                faceIndices.get(j),
                                                faceIndices.get(j + 1)
                                        });
                                    }
                                }
                                faceIndices.clear();
                            }
                        }
                    }
                }
            }
        }
        return triangleIndices;
    }

    // Метод для извлечения UV координат из геометрии
    public static List<double[]> extractUVCoordsFromGeometry(JsonNode geometryNode, String uvChannelName) {
        List<double[]> uvCoords = new ArrayList<>();
        JsonNode layerElementUVNode = findLayerElementUVByName(geometryNode, uvChannelName);
        if (layerElementUVNode != null) {
            JsonNode uvNode = findNodeByName(layerElementUVNode, "UV");
            if (uvNode != null && uvNode.has("properties")) {
                for (JsonNode property : uvNode.get("properties")) {
                    if (property.get("type").asText().equals("d")) {
                        JsonNode valueNode = property.get("value");
                        if (valueNode.isArray()) {
                            for (int i = 0; i < valueNode.size(); i += 2) {
                                double u = valueNode.get(i).asDouble();
                                double v = valueNode.get(i + 1).asDouble();
                                uvCoords.add(new double[]{u, v});
                            }
                        }
                    }
                }
            }
        }
        return uvCoords;
    }

    // Метод для извлечения UV индексов из геометрии
    public static List<int[]> extractUVIndicesFromGeometry(JsonNode geometryNode, int uvOffset, String uvChannelName) {
        List<int[]> uvIndices = new ArrayList<>();
        JsonNode layerElementUVNode = findLayerElementUVByName(geometryNode, uvChannelName);
        if (layerElementUVNode != null) {
            JsonNode uvIndexNode = findNodeByName(layerElementUVNode, "UVIndex");
            if (uvIndexNode != null && uvIndexNode.has("properties")) {
                for (JsonNode property : uvIndexNode.get("properties")) {
                    if (property.get("type").asText().equals("i")) {
                        JsonNode valueNode = property.get("value");
                        if (valueNode.isArray()) {
                            List<Integer> indices = new ArrayList<>();
                            for (JsonNode indexNode : valueNode) {
                                indices.add(indexNode.asInt() + uvOffset);
                            }
                            // Группируем индексы по треугольникам
                            List<Integer> faceIndices = new ArrayList<>();
                            for (int index : indices) {
                                faceIndices.add(index);
                                if (faceIndices.size() == 3) {
                                    uvIndices.add(new int[]{
                                            faceIndices.get(0),
                                            faceIndices.get(1),
                                            faceIndices.get(2)
                                    });
                                    faceIndices.clear();
                                }
                            }
                        }
                    }
                }
            }
        }
        return uvIndices;
    }

    // Метод для поиска LayerElementUV по имени UV-канала
    private static JsonNode findLayerElementUVByName(JsonNode geometryNode, String uvChannelName) {
        List<JsonNode> layerElementUVNodes = findNodesByName(geometryNode, "LayerElementUV");
        for (JsonNode layerElementUVNode : layerElementUVNodes) {
            String name = getLayerElementUVName(layerElementUVNode);
            if (uvChannelName.equals(name)) {
                return layerElementUVNode;
            }
        }
        return null;
    }

    // Метод для получения имени LayerElementUV
    private static String getLayerElementUVName(JsonNode layerElementUVNode) {
        JsonNode nameNode = findNodeByName(layerElementUVNode, "Name");
        if (nameNode != null && nameNode.has("properties") && nameNode.get("properties").size() > 0) {
            JsonNode property = nameNode.get("properties").get(0);
            if (property.has("type") && property.get("type").asText().equals("S")) {
                return property.get("value").asText();
            }
        }
        return null;
    }

    // Метод для проверки материалов (без изменений)
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

    // Метод для поиска материалов в JSON (без изменений)
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

    // Извлекаем имя материала (без изменений)
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

    // Метод для проверки последовательных материалов с записью индекса в файл (без изменений)
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

    // Метод для проверки количества слоев (без изменений)
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

    // Вспомогательный метод для рекурсивного подсчета слоев (без изменений)
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

    // Метод для вычисления количества полигонов с проверкой индексов и дубликатов (без изменений)
    public static int calculatePolygonCountWithValidation(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        int polygonCount = 0;
        Set<Integer> usedVertices = new HashSet<>();   // Для проверки индексов

        // Поиск узлов Geometry
        JsonNode objectsNode = findNodeByName(rootNode, "Objects");
        if (objectsNode != null && objectsNode.has("children")) {
            for (JsonNode geometryNode : objectsNode.get("children")) {
                if (geometryNode.get("name").asText().equals("Geometry")) {
                    // Извлекаем индексы полигонов
                    JsonNode polygonVertexIndexNode = findNodeByName(geometryNode, "PolygonVertexIndex");
                    if (polygonVertexIndexNode != null && polygonVertexIndexNode.has("properties")) {
                        for (JsonNode property : polygonVertexIndexNode.get("properties")) {
                            if (property.get("type").asText().equals("i")) {
                                JsonNode indicesArray = property.get("value");

                                List<Integer> indices = new ArrayList<>();
                                for (JsonNode indexNode : indicesArray) {
                                    indices.add(indexNode.asInt());
                                }

                                int vertexCounter = 0; // Счетчик вершин между отрицательными значениями

                                for (int indexValue : indices) {
                                    int correctedIndex = (indexValue < 0) ? -indexValue - 1 : indexValue;
                                    vertexCounter++;

                                    // Добавляем индекс в набор использованных вершин
                                    usedVertices.add(correctedIndex);

                                    if (indexValue < 0) {
                                        if (vertexCounter > 3) {
                                            // Если это многоугольник (больше 3 вершин), разбиваем его на треугольники
                                            polygonCount += (vertexCounter - 2); // (n-2) треугольника для многоугольника с n вершинами
                                        } else {
                                            // Если это треугольник
                                            polygonCount++;
                                        }
                                        vertexCounter = 0; // Сбрасываем счетчик вершин для следующего полигона
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Использовано вершин: " + usedVertices.size());
        return polygonCount;  // Возвращаем количество треугольников
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

    // Вспомогательный метод для поиска всех узлов по имени
    private static List<JsonNode> findNodesByName(JsonNode rootNode, String name) {
        List<JsonNode> nodes = new ArrayList<>();
        if (rootNode.isObject() && rootNode.has("name") && rootNode.get("name").asText().equals(name)) {
            nodes.add(rootNode);
        }

        if (rootNode.has("children")) {
            for (JsonNode childNode : rootNode.get("children")) {
                nodes.addAll(findNodesByName(childNode, name));
            }
        }
        return nodes;
    }

    // Вспомогательный метод для получения имени модели
    private static String getModelName(JsonNode modelNode) {
        if (modelNode.has("properties") && modelNode.get("properties").isArray()) {
            JsonNode properties = modelNode.get("properties");
            for (JsonNode property : properties) {
                if (property.has("type") && property.get("type").asText().equals("S")) {
                    String value = property.get("value").asText();
                    String modelName = value.split("\u0000")[0];
                    return modelName;
                }
            }
        }
        return null;
    }
}