package com.example.fbxchecker;

import java.util.*;

public class TexelDensityCalculator {

    private List<double[]> vertices;
    private List<int[]> triangleIndices;
    private List<double[]> uvCoords;
    private List<int[]> uvIndices;
    private Map<Integer, Integer> udimResolutionMap;

    // Добавляем константу EPSILON для учёта погрешности
    private static final double EPSILON = 0.0001;

    public TexelDensityCalculator(List<double[]> vertices, List<int[]> triangleIndices,
                                  List<double[]> uvCoords, List<int[]> uvIndices,
                                  Map<Integer, Integer> udimResolutionMap) {
        this.vertices = vertices;
        this.triangleIndices = triangleIndices;
        this.uvCoords = uvCoords;
        this.uvIndices = uvIndices;
        this.udimResolutionMap = udimResolutionMap;
    }

    public void calculateTexelDensity(ValidationResult result) {
        // Карта для хранения данных по каждому UDIM
        Map<Integer, UdimData> udimDataMap = new HashMap<>();

        // Проходим по всем треугольникам
        for (int i = 0; i < triangleIndices.size(); i++) {
            int[] triIndices = triangleIndices.get(i);
            int[] uvTriIndices = uvIndices.get(i);

            // Координаты вершин в мировом пространстве
            double[] vertexA = vertices.get(triIndices[0]);
            double[] vertexB = vertices.get(triIndices[1]);
            double[] vertexC = vertices.get(triIndices[2]);

            // Координаты в UV-пространстве
            double[] uvA = uvCoords.get(uvTriIndices[0]);
            double[] uvB = uvCoords.get(uvTriIndices[1]);
            double[] uvC = uvCoords.get(uvTriIndices[2]);

            // Вычисляем UDIM для треугольника
            int udim = calculateUdim(uvA, uvB, uvC);

            // Получаем разрешение текстуры для этого UDIM
            int textureResolution = udimResolutionMap.getOrDefault(udim, 0);

            // Собираем данные по UDIM
            UdimData udimData = udimDataMap.getOrDefault(udim, new UdimData(udim, textureResolution));
            udimData.addTriangle();  // Увеличиваем общее количество полигонов
            udimDataMap.put(udim, udimData);

            if (textureResolution == 0 || textureResolution == 256) {
                // Если разрешение текстуры не найдено или это заглушка, не вычисляем Texel Density
                continue;
            }

            // Вычисляем площадь треугольника в мировом пространстве
            double worldArea = calculateTriangleArea(vertexA, vertexB, vertexC);

            // Вычисляем площадь треугольника в UV-пространстве
            double uvArea = calculateTriangleArea2D(uvA, uvB, uvC);

            // Проверяем, чтобы площади были положительными и ненулевыми
            if (worldArea <= EPSILON || uvArea <= EPSILON) {
                continue;  // Пропускаем некорректные треугольники
            }

            // Вычисляем texel density для треугольника
            double texelDensity = Math.sqrt(uvArea) * textureResolution / Math.sqrt(worldArea);

            // Добавляем Texel Density в данные UDIM
            udimData.addTexelDensity(texelDensity);
            udimData.addTriangleWithTexelDensity(texelDensity);
        }

        // Выводим результаты
        result.addSeparator();
        result.addMessage("9. Texel Density по UDIM:");
        for (UdimData udimData : udimDataMap.values()) {
            int textureResolution = udimResolutionMap.getOrDefault(udimData.udim, 0);
            int totalTriangles = udimData.getTotalTriangles();

            if (textureResolution == 0 || textureResolution == 256) {
                // UDIM с заглушкой
                result.addMessage("UDIM: " + udimData.udim +
                        ", Заглушка, Всего полигонов: " + totalTriangles);
                continue;
            }

            double averageTexelDensity = udimData.getAverageTexelDensity();
            int errorTriangles = udimData.getTrianglesOutOfRange(512.0, 1706.0);

            String status = (errorTriangles == 0) ? "ОК" : "Ошибка";
            result.addMessage("UDIM: " + udimData.udim +
                    ", Средний Texel Density: " + String.format("%.2f", averageTexelDensity) +
                    ", " + status + ", Полигонов вне диапазона: " + errorTriangles +
                    ", Всего полигонов: " + totalTriangles);
        }
    }

    // Метод для вычисления UDIM на основе UV-координат треугольника
    private int calculateUdim(double[] uvA, double[] uvB, double[] uvC) {
        // Берём среднее значение UV для треугольника
        double u = (uvA[0] + uvB[0] + uvC[0]) / 3.0;
        double v = (uvA[1] + uvB[1] + uvC[1]) / 3.0;

        int uTile = (int) Math.floor(u);
        int vTile = (int) Math.floor(v);

        // UDIM = 1001 + uTile + vTile * 10
        return 1001 + uTile + vTile * 10;
    }

    // Метод для вычисления площади треугольника в 3D
    private double calculateTriangleArea(double[] A, double[] B, double[] C) {
        double[] AB = new double[]{B[0] - A[0], B[1] - A[1], B[2] - A[2]};
        double[] AC = new double[]{C[0] - A[0], C[1] - A[1], C[2] - A[2]};

        double[] crossProduct = new double[]{
                AB[1] * AC[2] - AB[2] * AC[1],
                AB[2] * AC[0] - AB[0] * AC[2],
                AB[0] * AC[1] - AB[1] * AC[0]
        };

        double area = 0.5 * Math.sqrt(
                crossProduct[0] * crossProduct[0] +
                        crossProduct[1] * crossProduct[1] +
                        crossProduct[2] * crossProduct[2]
        );

        return area;
    }

    // Метод для вычисления площади треугольника в 2D (UV-пространство)
    private double calculateTriangleArea2D(double[] A, double[] B, double[] C) {
        double area = 0.5 * Math.abs(
                (B[0] - A[0]) * (C[1] - A[1]) - (C[0] - A[0]) * (B[1] - A[1])
        );
        return area;
    }

    // Вспомогательный класс для хранения данных по UDIM
    private static class UdimData {
        int udim;
        int textureResolution;
        List<Double> texelDensities;
        int trianglesOutOfRange;
        int totalTriangles;
        int trianglesWithTexelDensity;

        public UdimData(int udim, int textureResolution) {
            this.udim = udim;
            this.textureResolution = textureResolution;
            this.texelDensities = new ArrayList<>();
            this.trianglesOutOfRange = 0;
            this.totalTriangles = 0;
            this.trianglesWithTexelDensity = 0;
        }

        public void addTriangle() {
            totalTriangles++;
        }

        public void addTexelDensity(double texelDensity) {
            texelDensities.add(texelDensity);
        }

        public void addTriangleWithTexelDensity(double texelDensity) {
            trianglesWithTexelDensity++;
            // Округляем texelDensity перед сравнением
            double roundedTexelDensity = Math.round(texelDensity * 100.0) / 100.0;

            if (roundedTexelDensity < 512.0 - EPSILON || roundedTexelDensity > 1706.0 + EPSILON) {
                trianglesOutOfRange++;
                // Для отладки выводим значение, которое вне диапазона
                System.out.println("UDIM " + udim + ": Triangle's texel density " + roundedTexelDensity + " out of range.");
            }
        }

        public double getAverageTexelDensity() {
            if (texelDensities.isEmpty()) {
                return 0.0;
            }
            double sum = 0;
            for (double td : texelDensities) {
                sum += td;
            }
            return sum / texelDensities.size();
        }

        public int getTrianglesOutOfRange(double min, double max) {
            return trianglesOutOfRange;
        }

        public int getTotalTriangles() {
            return totalTriangles;
        }
    }
}