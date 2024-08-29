package com.example.fbxchecker;

public class FileNameValidator {
    //проверяем имя файла
    public boolean validateFileName(String filename) {
        if(filename == null || filename.isEmpty()) {
            System.out.println("Имя файла не должно быть пустым");
            return false;
        }

        if(!filename.startsWith("SM_")) {
            System.out.println("Имя файла должно начинаться с 'SM_'");
            return false;
        }


        return true;
    }
}
