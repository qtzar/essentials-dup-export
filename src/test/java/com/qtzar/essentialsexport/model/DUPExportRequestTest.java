package com.qtzar.essentialsexport.model;

import com.qtzar.essentialsexport.model.dup.ClassSelection;
import com.qtzar.essentialsexport.model.dup.DUPExportRequest;
import com.qtzar.essentialsexport.model.dup.FieldSelection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DUPExportRequestTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        FieldSelection field = new FieldSelection("description", true);
        ClassSelection classSelection = new ClassSelection("Business_Capability", Collections.singletonList(field), true);

        // Act
        DUPExportRequest request = new DUPExportRequest(
                "test-repo",
                "Test Repository",
                "TST",
                Collections.singletonList(classSelection)
        );

        // Assert
        assertEquals("test-repo", request.getRepoId());
        assertEquals("Test Repository", request.getExternalRepositoryName());
        assertEquals("TST", request.getIdPrefix());
        assertNotNull(request.getClassSelections());
        assertEquals(1, request.getClassSelections().size());
    }

    @Test
    void testNoArgsConstructor() {
        // Act
        DUPExportRequest request = new DUPExportRequest();

        // Assert
        assertNotNull(request);
        assertNull(request.getRepoId());
        assertNull(request.getExternalRepositoryName());
        assertNull(request.getIdPrefix());
        assertNull(request.getClassSelections());
    }

    @Test
    void testSetters() {
        // Arrange
        DUPExportRequest request = new DUPExportRequest();
        ClassSelection classSelection = new ClassSelection();

        // Act
        request.setRepoId("repo1");
        request.setExternalRepositoryName("External Repo");
        request.setIdPrefix("EXT");
        request.setClassSelections(Collections.singletonList(classSelection));

        // Assert
        assertEquals("repo1", request.getRepoId());
        assertEquals("External Repo", request.getExternalRepositoryName());
        assertEquals("EXT", request.getIdPrefix());
        assertEquals(1, request.getClassSelections().size());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        ClassSelection classSelection = new ClassSelection();
        DUPExportRequest request1 = new DUPExportRequest("repo1", "Repo Name", "PRE", Collections.singletonList(classSelection));
        DUPExportRequest request2 = new DUPExportRequest("repo1", "Repo Name", "PRE", Collections.singletonList(classSelection));

        // Assert
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        // Arrange
        DUPExportRequest request = new DUPExportRequest("repo1", "Test", "TST", Collections.emptyList());

        // Act
        String toString = request.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("repo1"));
        assertTrue(toString.contains("Test"));
        assertTrue(toString.contains("TST"));
    }

    @Test
    void testWithNullIdPrefix() {
        // Arrange & Act
        DUPExportRequest request = new DUPExportRequest("repo1", "Test", null, Collections.emptyList());

        // Assert
        assertNull(request.getIdPrefix());
        assertNotNull(request.getRepoId());
    }

    @Test
    void testWithEmptyClassSelections() {
        // Arrange & Act
        DUPExportRequest request = new DUPExportRequest("repo1", "Test", "TST", Collections.emptyList());

        // Assert
        assertNotNull(request.getClassSelections());
        assertTrue(request.getClassSelections().isEmpty());
    }
}
