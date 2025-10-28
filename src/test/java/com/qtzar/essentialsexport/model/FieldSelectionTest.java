package com.qtzar.essentialsexport.model;

import com.qtzar.essentialsexport.model.dup.FieldSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldSelectionTest {

    @Test
    void testConstructorAndGetters() {
        // Act
        FieldSelection fieldSelection = new FieldSelection("description", true);

        // Assert
        assertEquals("description", fieldSelection.getFieldName());
        assertTrue(fieldSelection.isSelected());
    }

    @Test
    void testNoArgsConstructor() {
        // Act
        FieldSelection fieldSelection = new FieldSelection();

        // Assert
        assertNotNull(fieldSelection);
        assertNull(fieldSelection.getFieldName());
        assertFalse(fieldSelection.isSelected());
    }

    @Test
    void testSetters() {
        // Arrange
        FieldSelection fieldSelection = new FieldSelection();

        // Act
        fieldSelection.setFieldName("testField");
        fieldSelection.setSelected(true);

        // Assert
        assertEquals("testField", fieldSelection.getFieldName());
        assertTrue(fieldSelection.isSelected());
    }

    @Test
    void testIsSelected() {
        // Arrange
        FieldSelection fieldSelection = new FieldSelection("test", false);

        // Act & Assert
        assertFalse(fieldSelection.isSelected());
        fieldSelection.setSelected(true);
        assertTrue(fieldSelection.isSelected());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        FieldSelection field1 = new FieldSelection("name", true);
        FieldSelection field2 = new FieldSelection("name", true);

        // Assert
        assertEquals(field1, field2);
        assertEquals(field1.hashCode(), field2.hashCode());
    }

    @Test
    void testNotEquals() {
        // Arrange
        FieldSelection field1 = new FieldSelection("name", true);
        FieldSelection field2 = new FieldSelection("description", true);
        FieldSelection field3 = new FieldSelection("name", false);

        // Assert
        assertNotEquals(field1, field2);
        assertNotEquals(field1, field3);
    }

    @Test
    void testToString() {
        // Arrange
        FieldSelection fieldSelection = new FieldSelection("description", true);

        // Act
        String toString = fieldSelection.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("description"));
    }

    @Test
    void testWithNullFieldName() {
        // Act
        FieldSelection fieldSelection = new FieldSelection(null, true);

        // Assert
        assertNull(fieldSelection.getFieldName());
        assertTrue(fieldSelection.isSelected());
    }

    @Test
    void testWithEmptyFieldName() {
        // Act
        FieldSelection fieldSelection = new FieldSelection("", false);

        // Assert
        assertEquals("", fieldSelection.getFieldName());
        assertFalse(fieldSelection.isSelected());
    }
}
