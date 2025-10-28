package com.qtzar.essentialsexport.model.dup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for DUP export generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DUPExportRequest {

    /**
     * EAS repository ID to export from
     */
    private String repoId;

    /**
     * External repository name that instances will be associated with
     */
    private String externalRepositoryName;

    /**
     * Optional ID prefix for transforming instance IDs
     * If provided, instance IDs will be transformed to {prefix}_{number}
     * IDs already starting with this prefix will be preserved
     */
    private String idPrefix;

    /**
     * List of class selections for export
     */
    private List<ClassSelection> classSelections;
}
