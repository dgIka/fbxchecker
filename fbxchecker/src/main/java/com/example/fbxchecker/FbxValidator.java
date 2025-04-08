package com.example.fbxchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
        TextureValidator textureValidator = new TextureValidator();

        // Извлекаем базовое имя из пути к архиву
        String baseName = extractBaseName(zipFilePath);
        result.addMessage("1. Имя проекта: " + baseName);  // 1.
        result.addSeparator();

        // Проверка ZIP файла
        if (validator.validateZipFile(zipFilePath, result)) {  // 2.
            System.out.println("ZIP check successful.");
        }

        result.addSeparator();

        // Разархивирование ZIP-файла
        Path tempDir = null;
        try {
            tempDir = validator.extractZipFile(zipFilePath);
        } catch (IOException e) {
            result.addMessage("Ошибка при разархивировании файла: " + e.getMessage());
        }

        if (tempDir == null) {
            result.addMessage("Ошибка: Временная директория не создана.");
            return;
        }

        result.addMessage("3. Список файлов. Проверка имен. \n");  // 3.

        FileNameValidator.validateFileNames(validator.listFilesInZip(zipFilePath), baseName, result);

        result.addSeparator();

        // Проверка JSON файла
        try {
            JsonFbxValidator.validateFbxVersion(jsonFilePath, result);  // 4.
        } catch (IOException e) {
            result.addMessage("Ошибка при чтении JSON файла: " + e.getMessage());
        }

        result.addMessage("5. Проверка имен объектов \n");  // 5.
        // Извлечение и проверка имен объектов
        try {
            List<String> modelNames = JsonFbxValidator.extractModelNames(jsonFilePath);
            ObjectNameValidator objectNameValidator = new ObjectNameValidator(baseName);

            // Выполняем проверки
            objectNameValidator.checkMainObject(modelNames, result);
            objectNameValidator.checkUcObjects(modelNames, result);

        } catch (IOException e) {
            result.addMessage("Ошибка при извлечении имен моделей: " + e.getMessage());
        }

        result.addSeparator();

        result.addMessage("6. Список материалов \n");  // 6.

        JsonFbxValidator.validateMaterials(jsonFilePath, baseName, result);

        result.addSeparator();

        result.addMessage("7. Polycount \n");  // 7.

        int polyCount = JsonFbxValidator.calculatePolygonCountWithValidation(jsonFilePath);
        if (polyCount < 2000000) {
            result.addMessage("Количество полигонов в сцене: " + polyCount + "   OK");
        } else {
            result.addMessage("Количество полигонов в сцене: " + polyCount + " Ошибка: количество полигонов не должно превышать 2 млн.");
        }

        result.addSeparator();

        // Проверка текстур
        result.addMessage("8. Проверка текстур \n");  // 8.

        try {
            // Передаем временную директорию в метод extractTextureFiles
            List<String> textureFiles = validator.extractTextureFiles(zipFilePath, tempDir);
            textureValidator.validateTextures(textureFiles, result, textureValidator.getUdimResolutionMap());
        } catch (IOException e) {
            result.addMessage("Ошибка при извлечении текстур из архива: " + e.getMessage());
        }

        result.addSeparator();

        List<double[]> vertices = null;

        List<int[]> triangleIndices = null;

        List<double[]> uvCoords = null;

        List<int[]> uvIndices = null;

        Set<Long> ucxGeometryIds = JsonFbxValidator.getUCXGeometryIds(jsonFilePath);

        try {
            // Указываем имя UV-канала
            String uvChannelName = null;

            vertices = JsonFbxValidator.extractVertices(jsonFilePath, ucxGeometryIds);
            triangleIndices = JsonFbxValidator.extractPolygonVertexIndices(jsonFilePath, ucxGeometryIds);
            uvCoords = JsonFbxValidator.extractUVCoords(jsonFilePath, ucxGeometryIds, uvChannelName);
            uvIndices = JsonFbxValidator.extractUVIndices(jsonFilePath, ucxGeometryIds, uvChannelName);



            // Для проверки можно вывести размеры списков
            System.out.println("Number of vetices: " + vertices.size());
            System.out.println("Number of triangles: " + triangleIndices.size());
            System.out.println("Number of UV Coords: " + uvCoords.size());
            System.out.println("Number of UV Indices: " + uvIndices.size());

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Создаём экземпляр TexelDensityCalculator
        TexelDensityCalculator texelDensityCalculator = new TexelDensityCalculator(
                vertices,
                triangleIndices,
                uvCoords,
                uvIndices,
                textureValidator.getUdimResolutionMap()
        );

        // Вычисляем Texel Density и добавляем результаты в ValidationResult
        texelDensityCalculator.calculateTexelDensity(result);





        // Сохранение результатов проверки в файл
        try {
            Files.writeString(Path.of(baseName + "_report.txt"), result.generateReport());
            System.out.println("Check results saved: " + baseName + "_report.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Удаление временной директории после всех проверок
        try {
            validator.deleteTempDirectory(tempDir);
            System.out.println("Temp files deleted.");
        } catch (IOException e) {
            System.out.println("Ошибка при удалении временных файлов: " + e.getMessage());
        }
    }

    // Метод для извлечения базового имени из пути к архиву или файлу FBX
    private static String extractBaseName(String filePath) {
        int start = filePath.indexOf("SM_");
        int end = filePath.lastIndexOf('.');
        if (start != -1 && end != -1) {
            return filePath.substring(start + 3, end);  // Извлекаем все после "SM_" и до расширения
        }
        return "";
    }
}