package com.projetCloud.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service de gestion du stockage des fichiers sur le disque
 */
@Service
public class FileStorageService {

    private final Path uploadDir;
    private final String baseUrl;

    public FileStorageService(
            @Value("${file.upload-dir:uploads/photos}") String uploadDir,
            @Value("${server.port:8080}") String serverPort) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = "http://localhost:" + serverPort + "/uploads/photos/";
        
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage", e);
        }
    }

    /**
     * Sauvegarde un fichier sur le disque et retourne l'URL d'accès
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Le fichier est vide");
        }

        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Copier le fichier vers le dossier de destination
        Path targetLocation = this.uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Retourner l'URL d'accès au fichier
        return baseUrl + fileName;
    }

    /**
     * Supprime un fichier du disque à partir de son URL
     */
    public void deleteFileByUrl(String fileUrl) throws IOException {
        if (fileUrl == null || !fileUrl.startsWith(baseUrl)) {
            return; // URL invalide ou externe
        }

        String fileName = fileUrl.substring(baseUrl.length());
        Path filePath = this.uploadDir.resolve(fileName);
        Files.deleteIfExists(filePath);
    }

    /**
     * Récupère le chemin absolu du répertoire d'upload
     */
    public Path getUploadDir() {
        return uploadDir;
    }
}
