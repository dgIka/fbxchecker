package com.example.fbxchecker;

import java.io.File;
import java.nio.file.Path;

public class ArchiveStructureValidator {
    //Проверка самого архива и его структуры


    //проверка на отсутствие вложенных папок
    public boolean hasNoNestedDirectories(Path tempDir, ValidationResult result) {
        File[] files = tempDir.toFile().listFiles();
        if(files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    result.addMessage("Проверка на отсутствие вложенных папок: Ошибка: Архив содержит вложенные папки.");
                    return false;
                }
            }
        }
        result.addMessage("Проверка на отсутствие вложенных папок: ОК");
        return true;
    }
    //общий метод для проверки наличия ровно одного файла с указанным расширением
    private boolean hasSingleFileWithExtension(Path tempDir, String extension, ValidationResult result) {
        File[] files = tempDir.toFile().listFiles();
        int count = 0;

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(extension)) {
                    count++;
                }
            }
        }
        if (count < 1) {
            result.addMessage("Ошибка: Файл с расширением " + extension + " отсутствует");
            return false;
        } else if (count > 1) {
            result.addMessage("Ошибка: Файлов с расширением " + extension + " больше одного.");
            return false;
        } else return true;
    }

    public boolean checkFbxFile(Path tempDir, ValidationResult result) {
        return hasSingleFileWithExtension(tempDir, ".fbx", result);
    }

    public boolean checkJsonFile(Path tempDir, ValidationResult result) {
        return hasSingleFileWithExtension(tempDir, ".geojson", result);
    }
}
