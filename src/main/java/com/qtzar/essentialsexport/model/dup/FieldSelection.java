package com.qtzar.essentialsexport.model.dup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a field selection for DUP export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldSelection {
    
    /**
     * The name of the field to export
     */
    private String fieldName;
    
    /**
     * Whether this field is selected for export
     */
    private boolean selected;
}
