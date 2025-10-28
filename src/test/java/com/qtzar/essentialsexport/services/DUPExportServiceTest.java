package com.qtzar.essentialsexport.services;

import com.qtzar.essentialsexport.clients.EASClient;
import com.qtzar.essentialsexport.model.dup.ClassSelection;
import com.qtzar.essentialsexport.model.dup.DUPExportRequest;
import com.qtzar.essentialsexport.model.dup.FieldSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DUPExportServiceTest {

    @Mock
    private EASClient easClient;

    @InjectMocks
    private DUPExportService dupExportService;

    private DUPExportRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new DUPExportRequest();
        testRequest.setRepoId("test-repo");
        testRequest.setExternalRepositoryName("Test Repository");
        testRequest.setIdPrefix("TST");

        FieldSelection field1 = new FieldSelection("description", true);
        FieldSelection field2 = new FieldSelection("owner", true);

        ClassSelection classSelection = new ClassSelection();
        classSelection.setClassName("Business_Capability");
        classSelection.setSelected(true);
        classSelection.setFields(Arrays.asList(field1, field2));

        testRequest.setClassSelections(Collections.singletonList(classSelection));
    }

    @Test
    void testGenerateDUPExport_Success() throws IOException {
        // Arrange
        Map<String, Object> instance1 = new HashMap<>();
        instance1.put("id", "inst1");
        instance1.put("name", "Capability 1");
        instance1.put("className", "Business_Capability");
        instance1.put("description", "Test description");
        instance1.put("owner", "owner1");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance1));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Verify ZIP structure
        Map<String, String> zipContents = readZipContents(result);
        assertTrue(zipContents.containsKey("dup_import_script.py"));
        assertTrue(zipContents.containsKey("standardFunctions.py"));
        assertTrue(zipContents.containsKey("update.info"));
        assertTrue(zipContents.containsKey("updatepack.xsd"));

        String script = zipContents.get("dup_import_script.py");
        assertTrue(script.contains("Business_Capability"));
        assertTrue(script.contains("Test Repository"));
        assertTrue(script.contains("TST_1"));
    }

    @Test
    void testGenerateDUPExport_WithIdPrefix() throws IOException {
        // Arrange
        Map<String, Object> instance1 = new HashMap<>();
        instance1.put("id", "inst1");
        instance1.put("name", "Instance 1");
        instance1.put("className", "Business_Capability");
        instance1.put("description", "Test");

        Map<String, Object> instance2 = new HashMap<>();
        instance2.put("id", "TST_5");
        instance2.put("name", "Instance 2");
        instance2.put("className", "Business_Capability");
        instance2.put("description", "Test");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Arrays.asList(instance1, instance2));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Verify ID transformation
        assertTrue(script.contains("TST_1")); // inst1 transformed
        assertTrue(script.contains("TST_5")); // TST_5 preserved
        assertFalse(script.contains("'inst1'")); // Original ID should not appear in quotes
    }

    @Test
    void testGenerateDUPExport_WithoutIdPrefix() throws IOException {
        // Arrange
        testRequest.setIdPrefix(null);

        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", "Test");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // IDs should remain unchanged
        assertTrue(script.contains("inst1"));
    }

    @Test
    void testGenerateDUPExport_WithReferenceFields() throws IOException {
        // Arrange
        Map<String, Object> refObject = new HashMap<>();
        refObject.put("id", "ref1");

        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", "Test");
        instance.put("owner", refObject); // Reference object

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Reference should be transformed
        assertTrue(script.contains("TST_")); // Both main ID and reference should be transformed
    }

    @Test
    void testGenerateDUPExport_WithListFields() throws IOException {
        // Arrange
        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", Arrays.asList("Desc1", "Desc2", "Desc3"));

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // List should be properly formatted
        assertTrue(script.contains("["));
        assertTrue(script.contains("Desc1"));
        assertTrue(script.contains("Desc2"));
        assertTrue(script.contains("Desc3"));
    }

    @Test
    void testGenerateDUPExport_WithSpecialCharacters() throws IOException {
        // Arrange
        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance's \"Name\" with \n newline");
        instance.put("className", "Business_Capability");
        instance.put("description", "Test with \\ backslash");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Special characters should be escaped
        assertTrue(script.contains("\\'"));
        assertTrue(script.contains("\\\""));
        assertTrue(script.contains("\\n"));
        assertTrue(script.contains("\\\\"));
    }

    @Test
    void testGenerateDUPExport_MultipleClasses() throws IOException {
        // Arrange
        ClassSelection class2 = new ClassSelection();
        class2.setClassName("Application_Service");
        class2.setSelected(true);
        class2.setFields(Collections.singletonList(new FieldSelection("name", true)));

        // Create mutable list
        List<ClassSelection> classSelections = new ArrayList<>(testRequest.getClassSelections());
        classSelections.add(class2);
        testRequest.setClassSelections(classSelections);

        Map<String, Object> instance1 = new HashMap<>();
        instance1.put("id", "inst1");
        instance1.put("name", "Capability 1");
        instance1.put("className", "Business_Capability");
        instance1.put("description", "Test");

        Map<String, Object> instance2 = new HashMap<>();
        instance2.put("id", "inst2");
        instance2.put("name", "Service 1");
        instance2.put("className", "Application_Service");

        when(easClient.getAllInstancesAsMap(eq("test-repo"), eq("Business_Capability"), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance1));
        when(easClient.getAllInstancesAsMap(eq("test-repo"), eq("Application_Service"), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance2));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        assertTrue(script.contains("Business_Capability"));
        assertTrue(script.contains("Application_Service"));
    }

    @Test
    void testGenerateDUPExport_UnselectedClassesIgnored() throws IOException {
        // Arrange
        testRequest.getClassSelections().get(0).setSelected(false);

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Should not contain instance creation calls, only header
        assertFalse(script.contains("EssentialGetInstance"));
    }

    @Test
    void testGenerateDUPExport_UnselectedFieldsIgnored() throws IOException {
        // Arrange
        testRequest.getClassSelections().get(0).getFields().get(0).setSelected(false);

        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", "Should not appear");
        instance.put("owner", "owner1");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Should not contain unselected field
        assertFalse(script.contains("'description'"));
        assertTrue(script.contains("'owner'"));
    }

    @Test
    void testGenerateDUPExport_WithNullValues() throws IOException {
        // Arrange
        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", null);
        instance.put("owner", "owner1");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert - should not throw exception
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Null field should not generate addIfNotThere for that field
        assertTrue(script.contains("'owner'"));
    }

    @Test
    void testGenerateDUPExport_ClientException() {
        // Arrange
        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenThrow(new RuntimeException("EAS API Error"));

        // Act & Assert - should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> {
            byte[] result = dupExportService.generateDUPExport(testRequest);
            assertNotNull(result);
        });
    }

    @Test
    void testGenerateDUPExport_EmptyInstances() throws IOException {
        // Arrange
        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.emptyList());

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Should have header but no instances
        assertTrue(script.contains("defineExternalRepository"));
        assertFalse(script.contains("EssentialGetInstance"));
    }

    @Test
    void testGenerateDUPExport_WithBooleanAndNumberFields() throws IOException {
        // Arrange
        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "inst1");
        instance.put("name", "Instance 1");
        instance.put("className", "Business_Capability");
        instance.put("description", true);
        instance.put("owner", 42);

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Collections.singletonList(instance));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // Boolean should be formatted as True/False
        assertTrue(script.contains("True"));
        // Number should be unquoted
        assertTrue(script.contains("42"));
    }

    @Test
    void testGenerateDUPExport_IdSequenceAvoidance() throws IOException {
        // Arrange
        Map<String, Object> instance1 = new HashMap<>();
        instance1.put("id", "TST_1");
        instance1.put("name", "Instance 1");
        instance1.put("className", "Business_Capability");

        Map<String, Object> instance2 = new HashMap<>();
        instance2.put("id", "inst2");
        instance2.put("name", "Instance 2");
        instance2.put("className", "Business_Capability");

        when(easClient.getAllInstancesAsMap(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(Arrays.asList(instance1, instance2));

        // Act
        byte[] result = dupExportService.generateDUPExport(testRequest);

        // Assert
        assertNotNull(result);
        Map<String, String> zipContents = readZipContents(result);
        String script = zipContents.get("dup_import_script.py");

        // TST_1 should be preserved
        assertTrue(script.contains("TST_1"));
        // inst2 should be transformed to TST_2 (skipping TST_1)
        assertTrue(script.contains("TST_2"));
    }

    private Map<String, String> readZipContents(byte[] zipData) throws IOException {
        Map<String, String> contents = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] buffer = new byte[8192];
                StringBuilder sb = new StringBuilder();
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, bytesRead));
                }
                contents.put(entry.getName(), sb.toString());
                zis.closeEntry();
            }
        }

        return contents;
    }
}
