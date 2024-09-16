package com.example.fbxchecker;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<String> messages = new ArrayList<>();

    public void addMessage(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean hasErrors() {
        return messages.stream().anyMatch(msg -> msg.contains("Ошибка"));
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        messages.forEach(msg -> report.append(msg).append("\n"));
        return report.toString();
    }

    public void addSeparator() {
        messages.add("\r\n");
        messages.add("-------------------------------------------------");
        messages.add("\r\n");
    }
}
