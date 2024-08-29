package com.example.fbxchecker;

public class FbxValidator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Пожалуйста, укажите путь к ZIP файлу.");
            return;
        }

        String zipFilePath = args[0];
        FbxFileValidator validator = new FbxFileValidator();

        if(validator.validateZipFile(zipFilePath)) {
            System.out.println("ZIP файл успешно проверен.");
        } else {
            System.out.println("ZIP файл не прошел проверки");
        }
    }
}
