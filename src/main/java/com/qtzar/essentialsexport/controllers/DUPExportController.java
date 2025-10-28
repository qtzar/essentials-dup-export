package com.qtzar.essentialsexport.controllers;

import com.qtzar.essentialsexport.clients.EASClient;
import com.qtzar.essentialsexport.configuration.EASRepositoriesProperties;
import com.qtzar.essentialsexport.model.dup.DUPExportRequest;
import com.qtzar.essentialsexport.services.DUPExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller for DUP (Data Update Package) export operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/dup")
@RequiredArgsConstructor
public class DUPExportController {

    private final DUPExportService dupExportService;
    private final EASClient easClient;
    private final EASRepositoriesProperties easRepositoriesProperties;

    /**
     * Generates and downloads a DUP export file.
     *
     * @param request The export request containing class and field selections
     * @return ResponseEntity containing the .dup file for download
     */
    @PostMapping("/export")
    public ResponseEntity<ByteArrayResource> generateExport(@RequestBody DUPExportRequest request) {

        try {
            byte[] dupFile = dupExportService.generateDUPExport(request);

            String filename = sanitizeFilename(request.getExternalRepositoryName()) + ".dup";
            ByteArrayResource resource = new ByteArrayResource(dupFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(dupFile.length)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get list of available EAS repositories.
     *
     * @return List of repository configurations
     */
    @GetMapping("/repositories")
    public ResponseEntity<List<EASRepositoriesProperties.Repository>> getRepositories() {
        return ResponseEntity.ok(easRepositoriesProperties.getRepositories());
    }

    /**
     * Get all available EAS classes metadata with nested slots for a specific repository.
     * Returns a map structure where keys are class names and values contain slot information.
     *
     * @param repoId The repository ID to query
     * @return Map of class metadata with nested slots
     */
    @GetMapping("/classes")
    public ResponseEntity<Object> getClasses(@RequestParam String repoId) {
        try {
            log.debug("Fetching classes metadata for repository: {}", repoId);
            Object classes = easClient.getClassesMetadata(repoId);
            log.debug("Successfully fetched classes metadata");
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            log.error("Error fetching classes metadata for repository {}: {}", repoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error fetching classes: " + e.getMessage());
        }
    }

    /**
     * Sanitizes a string to make it safe for use as a filename.
     *
     * @param input The input string
     * @return A sanitized filename
     */
    private String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "export";
        }
        return input.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
