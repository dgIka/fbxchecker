package com.example.fbxchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReportGenerator {
    public void generateReportFile(ValidationResult result, String outputPath) throws IOException {
        Path file = Path.of(outputPath);
        Files.writeString(file, result.generateReport());
    }
}