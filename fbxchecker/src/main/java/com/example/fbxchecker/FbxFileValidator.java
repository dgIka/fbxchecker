package com.example.fbxchecker;

import java.io.File;

public class FbxFileValidator {
    //проверяем файлы на соответствие
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
