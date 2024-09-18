package com.example.fbxchecker;

import java.io.IOException;
import java.util.*;

public class TexelDensityCalculator {

    private List<double[]> vertices;
    private List<int[]> triangleIndices;
    private List<double[]> uvCoords;
    private List<int[]> uvIndices;
    private Map<Integer, Integer> udimResolutionMap;

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

            // Проверяем, есть ли информация о разрешении текстуры для этого UDIM
            int textureResolution = udimResolutionMap.getOrDefault(udim, 0);
            if (textureResolution == 0) {
                // Если разрешение текстуры не найдено или это заглушка, пропускаем
                continue;
            }

            // Вычисляем площадь треугольника в мировом пространстве
            double worldArea = calculateTriangleArea(vertexA, vertexB, vertexC);

            // Вычисляем площадь треугольника в UV-пространстве
            double uvArea = calculateTriangleArea2D(uvA, uvB, uvC);

            // Вычисляем texel density для треугольника
            double texelDensity = Math.sqrt(uvArea) * textureResolution / Math.sqrt(worldArea);

            // Собираем данные по UDIM
            UdimData udimData = udimDataMap.getOrDefault(udim, new UdimData(udim, textureResolution));
            udimData.addTexelDensity(texelDensity);
            udimData.addTriangle(texelDensity);
            udimDataMap.put(udim, udimData);
        }

        // Выводим результаты
        result.addSeparator();
        result.addMessage("9. Texel Density по UDIM:");
        for (UdimData udimData : udimDataMap.values()) {
            if (udimResolutionMap.get(udimData.udim) == 256) {
                // Пропускаем UDIM с заглушкой
                continue;
            }
            double averageTexelDensity = udimData.getAverageTexelDensity();
            int errorTriangles = udimData.getTrianglesOutOfRange(512, 1706);

            String status = (errorTriangles == 0) ? "ОК" : "Ошибка";
            result.addMessage("UDIM: " + udimData.udim +
                    ", Средний Texel Density: " + String.format("%.2f", averageTexelDensity) +
                    ", " + status + ", Полигонов вне диапазона: " + errorTriangles);
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

        public UdimData(int udim, int textureResolution) {
            this.udim = udim;
            this.textureResolution = textureResolution;
            this.texelDensities = new ArrayList<>();
            this.trianglesOutOfRange = 0;
        }

        public void addTexelDensity(double texelDensity) {
            texelDensities.add(texelDensity);
        }

        public void addTriangle(double texelDensity) {
            if (texelDensity < 512 || texelDensity > 1706) {
                trianglesOutOfRange++;
            }
        }

        public double getAverageTexelDensity() {
            double sum = 0;
            for (double td : texelDensities) {
                sum += td;
            }
            return sum / texelDensities.size();
        }

        public int getTrianglesOutOfRange(int min, int max) {
            return trianglesOutOfRange;
        }
    }
}