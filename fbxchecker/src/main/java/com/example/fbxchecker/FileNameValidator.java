package com.example.fbxchecker;

import java.util.regex.Pattern;

public class FileNameValidator {

    //проверяем имя файла
    public boolean validateFileName(String fileName, ValidationResult result) {
        if (fileName == null || fileName.isEmpty()) {
            result.addMessage("Ошибка: Имя файла не должно быть пустым");
            return false;
        }

        if (!fileName.startsWith("SM_")) {
            result.addMessage("Ошибка: Имя файла должно начинаться с 'SM_'");
        }


        // Убираем префикс "SM_" для дальнейших проверок
        String nameWithoutPrefix = fileName.substring(3);

        // Убираем расширение файла (если есть)
        String nameWithoutExtension = nameWithoutPrefix.contains(".")
                ? nameWithoutPrefix.substring(0, nameWithoutPrefix.lastIndexOf('.'))
                : nameWithoutPrefix;

        result.addMessage("Имя проекта " + nameWithoutExtension);
        result.addSeparator();

        // Регулярное выражение для проверки допустимых символов
        String regex = "^[a-zA-Z0-9_]+$";
        if (!Pattern.matches(regex, nameWithoutExtension)) {
            result.addMessage("Ошибка: Имя файла может содержать только латиницу, цифры и символы нижнего подчеркивания.");
        }

        // Проверка на наличие двух подряд идущих символов нижнего подчеркивания
        if (fileName.contains("__")) {
            result.addMessage("Ошибка: Два символа нижнего подчеркивания подряд недопустимы.");
        }

        result.addMessage("Проверка имени файла: ОК");
        return true;
    }
}
