package com.projetCloud.app.sync.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour un signalement venant de Firebase
 */
public class FirebaseReportDTO {
    private String id;
    private String uid;
    private String description;
    private String type;
    private BigDecimal lat;
    private BigDecimal lng;
    private String status;
    private BigDecimal surfaceM2;
    private BigDecimal budgetEstimated;
    private String companyName;
    private LocalDateTime createdAt;

    // Constructeurs
    public FirebaseReportDTO() {}

    public FirebaseReportDTO(String id, String uid, String description, String type,
                            BigDecimal lat, BigDecimal lng, String status,
                            BigDecimal surfaceM2, BigDecimal budgetEstimated,
                            String companyName, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.description = description;
        this.type = type;
        this.lat = lat;
        this.lng = lng;
        this.status = status;
        this.surfaceM2 = surfaceM2;
        this.budgetEstimated = budgetEstimated;
        this.companyName = companyName;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSurfaceM2() { return surfaceM2; }
    public void setSurfaceM2(BigDecimal surfaceM2) { this.surfaceM2 = surfaceM2; }

    public BigDecimal getBudgetEstimated() { return budgetEstimated; }
    public void setBudgetEstimated(BigDecimal budgetEstimated) { this.budgetEstimated = budgetEstimated; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
