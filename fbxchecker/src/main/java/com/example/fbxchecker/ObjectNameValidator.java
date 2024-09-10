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
        String expectedName = "SM_" + baseName + "_Main";
        if (modelNames.contains(expectedName)) {
            result.addMessage("Имя объекта " + expectedName + ": ОК");
        } else {
            result.addMessage("Ошибка: Отсутствует объект " + expectedName);
        }
    }

    // Проверка имени объекта SM_ + baseName + _MainGlass (если в имени нет "_Ground")
    public void checkMainGlassObject(List<String> modelNames, ValidationResult result) {
        if (!baseName.contains("_Ground")) {
            String expectedName = "SM_" + baseName + "_MainGlass";
            if (modelNames.contains(expectedName)) {
                result.addMessage("Имя объекта " + expectedName + ": ОК");
            } else {
                result.addMessage("Ошибка: Отсутствует объект " + expectedName);
            }
        }
    }

    // Проверка наличия объектов UCX_SM_ + baseName + _001, 002, 003 и т.д.
    public void checkUcObjects(List<String> modelNames, ValidationResult result) {
        int index = 1;
        boolean hasMissingObject = false;

        while (true) {
            String expectedName = String.format("UCX_SM_%s_%03d", baseName, index);
            if (modelNames.contains(expectedName)) {
                result.addMessage("Имя объекта " + expectedName + ": ОК");
                index++;
            } else {
                if (index > 1) {
                    result.addMessage("Ошибка: Отсутствует объект " + expectedName);
                }
                hasMissingObject = true;
                break;
            }
        }

        if (!hasMissingObject && index == 1) {
            result.addMessage("Ошибка: Не найдено объектов, начинающихся с UCX_SM_" + baseName + "_001");
        }
    }
}