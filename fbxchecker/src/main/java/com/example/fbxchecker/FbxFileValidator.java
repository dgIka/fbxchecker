package com.example.fbxchecker;

import java.io.File;

public class FbxFileValidator {
    //проверяем файлы на соответствие
    private static final long MAX_SIZE_MB = 500 * 1024 * 1024;
    private FileNameValidator fileNameValidator;

    public FbxFileValidator() {
        this.fileNameValidator = new FileNameValidator();
    }

    public boolean validateZipFile(String filepath) {
        File file = new File(filepath);

        //проверка существования файла
        if(!file.exists()) {
            System.out.println("Файл не найден: " + filepath);
            return false;
        }

        // Проверка размера файла
        if (file.length() > MAX_SIZE_MB) {
            System.out.println("Ошибка: размер файла превышает 500 MB.");
            return false;
        }

        //проверка расширения архива
        if(!filepath.endsWith(".zip")) {
            System.out.println("Ошибка: файл не имеет расширения .zip");
            return false;
        }

        if(!fileNameValidator.validateFileName(file.getName())) {
            System.out.println("Ошибка: имя файла не соответствует требованиям.");
            return false;
        }
        System.out.println("Файл проверен успешно.");
        return true;
    }
}
