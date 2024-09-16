package com.example.fbxchecker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class TextureValidator {

    // Хэшмапа для хранения информации о максимальном разрешении текстуры для каждого UDIM
    private Map<Integer, Integer> udimResolutionMap = new HashMap<>();

    // Метод для получения хэшмапы с разрешениями UDIM
    public Map<Integer, Integer> getUdimResolutionMap() {
        return udimResolutionMap;
    }

    // Метод для проверки списка текстур
    public void validateTextures(List<String> textureFiles, ValidationResult result, Map<Integer, Integer> udimResolutionMap) {
        // Определяем длину столбцов для выравнивания
        int textureNameLength = 40;  // длина столбца для названия текстуры
        int resolutionLength = 20;   // длина столбца для разрешения
        int alphaChannelLength = 25; // длина столбца для альфа-канала
        int bitDepthLength = 20;     // длина столбца для битности

        String separator = "     "; // 5 пробелов как разделитель между столбцами

        for (String textureFile : textureFiles) {
            try {
                File file = new File(textureFile);
                BufferedImage image = ImageIO.read(file);

                int width = image.getWidth();
                int height = image.getHeight();
                boolean hasAlpha = image.getColorModel().hasAlpha();
                String alphaChannelStatus = hasAlpha ? " Альфа-канал: есть" : " Альфа-канал: отсутствует";

                boolean is8Bit = checkBitDepth(image);
                String bitDepthStatus = is8Bit ? "8 bit : OK" : "8 bit : Ошибка";

                // Получаем только имя файла с расширением, без полного пути
                String fileName = file.getName();

                // Форматируем строку для вывода в соответствии с заданными длинами столбцов
                String formattedTextureName = String.format("%-" + textureNameLength + "s", fileName);
                String formattedResolution = String.format("%-" + resolutionLength + "s", " Размер: " + width + " x " + height);
                String formattedAlphaChannel = String.format("%-" + alphaChannelLength + "s", alphaChannelStatus);
                String formattedBitDepth = String.format("%-" + bitDepthLength + "s", bitDepthStatus);

                // Проверка размера текстуры и запись результатов в один столбец с 5 пробелами между проверками
                if (!isValidTextureSize(width, height)) {
                    result.addMessage(formattedTextureName + separator + formattedResolution + separator + "Ошибка: недопустимый размер");
                } else {
                    result.addMessage(formattedTextureName + separator + formattedResolution + separator + formattedAlphaChannel + separator + formattedBitDepth);
                }

                // Проверка цветовой палитры для 256x256 текстур
                if (width == 256 && height == 256 && !isSingleColorTexture(image)) {
                    result.addMessage(fileName + ": Ошибка - текстура 256x256 не является заглушкой.");
                }

                // Извлечение UDIM из имени файла и запись максимального разрешения для этого UDIM
                int udim = extractUdimFromFileName(fileName);
                int currentMaxResolution = udimResolutionMap.getOrDefault(udim, 0);
                int textureResolution = Math.max(width, height);
                if (textureResolution > currentMaxResolution) {
                    udimResolutionMap.put(udim, textureResolution);
                }

            } catch (IOException e) {
                result.addMessage("Ошибка при обработке файла " + new File(textureFile).getName() + ": " + e.getMessage());
            }
        }
    }

    // Метод для проверки допустимых размеров текстур
    private boolean isValidTextureSize(int width, int height) {
        return (width == 256 && height == 256) || (width == 2048 && height == 2048) || (width == 4096 && height == 4096);
    }

    // Метод для проверки, что текстура 8-bit
    private boolean checkBitDepth(BufferedImage image) {
        return image.getColorModel().getPixelSize() == 8;
    }

    // Метод для проверки, что текстура 256x256 состоит из одного цвета
    private boolean isSingleColorTexture(BufferedImage image) {
        // Простой метод для проверки, все ли пиксели имеют один цвет
        int width = image.getWidth();
        int height = image.getHeight();
        int firstPixel = image.getRGB(0, 0);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (image.getRGB(x, y) != firstPixel) {
                    return false;
                }
            }
        }
        return true;
    }

    // Метод для извлечения UDIM из имени файла
    private int extractUdimFromFileName(String textureFile) {
        // Предполагаем, что UDIM - это последние 4 цифры перед расширением
        String[] parts = textureFile.split("\\.");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[parts.length - 2]);  // Извлекаем предпоследнюю часть
            } catch (NumberFormatException e) {
                return -1;  // Если не удалось распознать номер UDIM, возвращаем -1
            }
        }
        return -1;
    }
}