package com.qtzar.essentialsexport.controllers;

import com.qtzar.essentialsexport.clients.EASClient;
import com.qtzar.essentialsexport.configuration.EASRepositoriesProperties;
import com.qtzar.essentialsexport.model.dup.ClassSelection;
import com.qtzar.essentialsexport.model.dup.DUPExportRequest;
import com.qtzar.essentialsexport.model.dup.FieldSelection;
import com.qtzar.essentialsexport.services.DUPExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DUPExportControllerSimpleTest {

    @Mock
    private DUPExportService dupExportService;

    @Mock
    private EASClient easClient;

    @Mock
    private EASRepositoriesProperties easRepositoriesProperties;

    @InjectMocks
    private DUPExportController dupExportController;

    private DUPExportRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new DUPExportRequest();
        testRequest.setRepoId("test-repo");
        testRequest.setExternalRepositoryName("Test Repository");
        testRequest.setIdPrefix("TST");

        FieldSelection field1 = new FieldSelection("description", true);
        ClassSelection classSelection = new ClassSelection();
        classSelection.setClassName("Business_Capability");
        classSelection.setSelected(true);
        classSelection.setFields(Collections.singletonList(field1));

        testRequest.setClassSelections(Collections.singletonList(classSelection));
    }

    @Test
    void testGenerateExport_Success() throws IOException {
        // Arrange
        byte[] mockDupFile = "mock dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition.contains("Test_Repository.dup"));
        assertEquals(mockDupFile.length, response.getBody().contentLength());
    }

    @Test
    void testGenerateExport_WithSpecialCharactersInFilename() throws IOException {
        // Arrange
        testRequest.setExternalRepositoryName("Test Repository! @#$%");
        byte[] mockDupFile = "mock dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition.contains(".dup"));
        // Special characters should be replaced with underscores
    }

    @Test
    void testGenerateExport_WithNullRepositoryName() throws IOException {
        // Arrange
        testRequest.setExternalRepositoryName(null);
        byte[] mockDupFile = "mock dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition.contains("export.dup"));
    }

    @Test
    void testGenerateExport_WithBlankRepositoryName() throws IOException {
        // Arrange
        testRequest.setExternalRepositoryName("   ");
        byte[] mockDupFile = "mock dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(contentDisposition.contains("export.dup"));
    }

    @Test
    void testGenerateExport_ServiceThrowsIOException() throws IOException {
        // Arrange
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class)))
                .thenThrow(new IOException("Failed to generate DUP"));

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetRepositories_Success() {
        // Arrange
        List<EASRepositoriesProperties.Repository> repositories = new ArrayList<>();
        EASRepositoriesProperties.Repository repo1 = new EASRepositoriesProperties.Repository();
        repo1.setName("Production");
        repo1.setRepoId("prod-repo");

        EASRepositoriesProperties.Repository repo2 = new EASRepositoriesProperties.Repository();
        repo2.setName("Development");
        repo2.setRepoId("dev-repo");

        repositories.add(repo1);
        repositories.add(repo2);

        when(easRepositoriesProperties.getRepositories()).thenReturn(repositories);

        // Act
        ResponseEntity<List<EASRepositoriesProperties.Repository>> response = dupExportController.getRepositories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Production", response.getBody().get(0).getName());
        assertEquals("prod-repo", response.getBody().get(0).getRepoId());
    }

    @Test
    void testGetRepositories_EmptyList() {
        // Arrange
        when(easRepositoriesProperties.getRepositories()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<EASRepositoriesProperties.Repository>> response = dupExportController.getRepositories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetClasses_Success() {
        // Arrange
        Map<String, Object> classMetadata = new HashMap<>();
        Map<String, Object> businessCapability = new HashMap<>();
        businessCapability.put("className", "Business_Capability");
        businessCapability.put("slots", Arrays.asList("name", "description", "owner"));

        classMetadata.put("Business_Capability", businessCapability);

        when(easClient.getClassesMetadata(eq("test-repo"))).thenReturn(classMetadata);

        // Act
        ResponseEntity<Object> response = dupExportController.getClasses("test-repo");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> bodyMap = (Map<String, Object>) response.getBody();
        assertTrue(bodyMap.containsKey("Business_Capability"));
    }

    @Test
    void testGetClasses_ClientException() {
        // Arrange
        when(easClient.getClassesMetadata(eq("test-repo")))
                .thenThrow(new RuntimeException("EAS API Error"));

        // Act
        ResponseEntity<Object> response = dupExportController.getClasses("test-repo");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGenerateExport_EmptyClassSelections() throws IOException {
        // Arrange
        testRequest.setClassSelections(Collections.emptyList());
        byte[] mockDupFile = "empty dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGenerateExport_WithoutIdPrefix() throws IOException {
        // Arrange
        testRequest.setIdPrefix(null);
        byte[] mockDupFile = "mock dup content".getBytes();
        when(dupExportService.generateDUPExport(any(DUPExportRequest.class))).thenReturn(mockDupFile);

        // Act
        ResponseEntity<ByteArrayResource> response = dupExportController.generateExport(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
