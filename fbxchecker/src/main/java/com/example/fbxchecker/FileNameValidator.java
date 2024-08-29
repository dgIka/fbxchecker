package com.example.fbxchecker;

import java.util.regex.Pattern;

public class FileNameValidator {
    //проверяем имя файла
    public boolean validateFileName(String fileName) {
        if(fileName == null || fileName.isEmpty()) {
            System.out.println("Имя файла не должно быть пустым");
            return false;
        }

        if(!fileName.startsWith("SM_")) {
            System.out.println("Имя файла должно начинаться с 'SM_'");
            return false;
        }

        // Убираем префикс "SM_" для дальнейших проверок
        String nameWithoutPrefix = fileName.substring(3);

        // Убираем расширение файла (если есть)
        String nameWithoutExtension = nameWithoutPrefix.contains(".")
                ? nameWithoutPrefix.substring(0, nameWithoutPrefix.lastIndexOf('.'))
                : nameWithoutPrefix;

        // Регулярное выражение для проверки допустимых символов
        String regex = "^[a-zA-Z0-9_]+$";
        if (!Pattern.matches(regex, nameWithoutExtension)) {
            System.out.println("Имя файла может содержать только латиницу, цифры и символы нижнего подчеркивания.");
            return false;
        }

        // Проверка на наличие двух подряд идущих символов нижнего подчеркивания
        if (fileName.contains("__")) {
            System.out.println("Два символа нижнего подчеркивания подряд недопустимы.");
            return false;
        }

        System.out.println("Имя архива корректное.");
        return true;
    }
}
