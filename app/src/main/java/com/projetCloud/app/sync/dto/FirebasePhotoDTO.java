package com.projetCloud.app.sync.dto;

import java.time.LocalDateTime;

/**
 * DTO pour une photo venant de Firebase
 */
public class FirebasePhotoDTO {
    private String id;
    private String reportId;
    private String uid;
    private String imgbbUrl;
    private LocalDateTime uploadedAt;

    // Constructeurs
    public FirebasePhotoDTO() {}

    public FirebasePhotoDTO(String id, String reportId, String uid, String imgbbUrl, LocalDateTime uploadedAt) {
        this.id = id;
        this.reportId = reportId;
        this.uid = uid;
        this.imgbbUrl = imgbbUrl;
        this.uploadedAt = uploadedAt;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getImgbbUrl() { return imgbbUrl; }
    public void setImgbbUrl(String imgbbUrl) { this.imgbbUrl = imgbbUrl; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
