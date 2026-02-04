package com.projetCloud.app.sync.dto;

import java.util.List;

/**
 * DTO pour la réponse de synchronisation
 */
public class SyncResponseDTO {
    private String status; // "success" ou "partial" ou "error"
    private int totalProcessed;
    private int successCount;
    private int errorCount;
    private List<String> errors;
    private String message;

    // Constructeurs
    public SyncResponseDTO() {}

    public SyncResponseDTO(String status, int totalProcessed, int successCount, 
                          int errorCount, List<String> errors, String message) {
        this.status = status;
        this.totalProcessed = totalProcessed;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.errors = errors;
        this.message = message;
    }

    // Getters & Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
