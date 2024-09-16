package com.example.fbxchecker;

import java.util.List;

public class ObjectNameValidator {

    private String baseName;

    // Конструктор принимает уже готовое базовое имя
    public ObjectNameValidator(String baseName) {
        this.baseName = baseName;
    }

    // Проверка имени объекта SM_ + baseName + _Main
    public void checkMainObject(List<String> modelNames, ValidationResult result) {
        if (!baseName.contains("_Ground")) {
            String expectedName = "SM_" + baseName + "_Main";
            if (modelNames.contains(expectedName)) {
                result.addMessage("Имя объекта " + expectedName + ": ОК");
            } else {
                result.addMessage("Ошибка: Отсутствует объект " + expectedName);
            }

            // Проверка _MainGlass объекта
            String expectedGlassName = "SM_" + baseName + "_MainGlass";
            if (modelNames.contains(expectedGlassName)) {
                result.addMessage("Имя объекта " + expectedGlassName + ": ОК");
            } else {
                result.addMessage("Ошибка: Отсутствует объект " + expectedGlassName);
            }
        } else {
            // Для объектов с _Ground просто проверяем наличие самого объекта
            String expectedGroundName = "SM_" + baseName;
            if (modelNames.contains(expectedGroundName)) {
                result.addMessage("Имя объекта " + expectedGroundName + ": ОК");
            } else {
                result.addMessage("Ошибка: Отсутствует объект " + expectedGroundName);
            }
        }
    }

    // Проверка наличия объектов UCX_SM_ + baseName + _Main + номер (или _Ground + номер, если это Ground)
    public void checkUcObjects(List<String> modelNames, ValidationResult result) {
        int index = 1;
        boolean isGround = baseName.contains("_Ground");

        // Проверяем все UCX объекты, пока не найдем отсутствующий (и не выводим ошибку)
        while (true) {
            String expectedName;
            if (isGround) {
                expectedName = String.format("UCX_SM_%s_%03d", baseName, index);
            } else {
                expectedName = String.format("UCX_SM_%s_Main_%03d", baseName, index);
            }

            if (modelNames.contains(expectedName)) {
                result.addMessage("Имя объекта " + expectedName + ": ОК");
                index++;
            } else {
                // Останавливаем проверку при отсутствии объекта
                break;
            }
        }
    }
}