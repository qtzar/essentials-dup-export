package com.qtzar.essentialsexport.model;

import com.qtzar.essentialsexport.model.dup.ClassSelection;
import com.qtzar.essentialsexport.model.dup.FieldSelection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ClassSelectionTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        FieldSelection field1 = new FieldSelection("name", true);
        FieldSelection field2 = new FieldSelection("description", false);

        // Act
        ClassSelection classSelection = new ClassSelection(
                "Business_Capability",
                Arrays.asList(field1, field2),
                true
        );

        // Assert
        assertEquals("Business_Capability", classSelection.getClassName());
        assertNotNull(classSelection.getFields());
        assertEquals(2, classSelection.getFields().size());
        assertTrue(classSelection.isSelected());
    }

    @Test
    void testNoArgsConstructor() {
        // Act
        ClassSelection classSelection = new ClassSelection();

        // Assert
        assertNotNull(classSelection);
        assertNull(classSelection.getClassName());
        assertNull(classSelection.getFields());
        assertFalse(classSelection.isSelected());
    }

    @Test
    void testSetters() {
        // Arrange
        ClassSelection classSelection = new ClassSelection();
        FieldSelection field = new FieldSelection("test", true);

        // Act
        classSelection.setClassName("TestClass");
        classSelection.setFields(Collections.singletonList(field));
        classSelection.setSelected(true);

        // Assert
        assertEquals("TestClass", classSelection.getClassName());
        assertEquals(1, classSelection.getFields().size());
        assertTrue(classSelection.isSelected());
    }

    @Test
    void testIsSelected() {
        // Arrange
        ClassSelection classSelection = new ClassSelection();

        // Act & Assert
        assertFalse(classSelection.isSelected());
        classSelection.setSelected(true);
        assertTrue(classSelection.isSelected());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        FieldSelection field = new FieldSelection("name", true);
        ClassSelection selection1 = new ClassSelection("Class1", Collections.singletonList(field), true);
        ClassSelection selection2 = new ClassSelection("Class1", Collections.singletonList(field), true);

        // Assert
        assertEquals(selection1, selection2);
        assertEquals(selection1.hashCode(), selection2.hashCode());
    }

    @Test
    void testToString() {
        // Arrange
        ClassSelection classSelection = new ClassSelection("TestClass", Collections.emptyList(), true);

        // Act
        String toString = classSelection.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("TestClass"));
    }

    @Test
    void testWithEmptyFields() {
        // Arrange & Act
        ClassSelection classSelection = new ClassSelection("TestClass", Collections.emptyList(), true);

        // Assert
        assertNotNull(classSelection.getFields());
        assertTrue(classSelection.getFields().isEmpty());
    }

    @Test
    void testWithMultipleFields() {
        // Arrange
        FieldSelection field1 = new FieldSelection("field1", true);
        FieldSelection field2 = new FieldSelection("field2", false);
        FieldSelection field3 = new FieldSelection("field3", true);

        // Act
        ClassSelection classSelection = new ClassSelection(
                "TestClass",
                Arrays.asList(field1, field2, field3),
                true
        );

        // Assert
        assertEquals(3, classSelection.getFields().size());
        assertEquals("field1", classSelection.getFields().get(0).getFieldName());
        assertEquals("field2", classSelection.getFields().get(1).getFieldName());
        assertEquals("field3", classSelection.getFields().get(2).getFieldName());
    }
}
