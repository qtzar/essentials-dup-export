package com.qtzar.essentialsexport.model.dup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an EAS class selection for DUP export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSelection {
    
    /**
     * The name of the EAS class
     */
    private String className;
    
    /**
     * The fields selected for export from this class
     */
    private List<FieldSelection> fields;
    
    /**
     * Whether this class is included in the export
     */
    private boolean selected;
}
