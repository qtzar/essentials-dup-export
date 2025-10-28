package com.qtzar.essentialsexport.services;

import com.qtzar.essentialsexport.clients.EASClient;
import com.qtzar.essentialsexport.model.dup.ClassSelection;
import com.qtzar.essentialsexport.model.dup.DUPExportRequest;
import com.qtzar.essentialsexport.model.dup.FieldSelection;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for generating DUP (Data Update Package) export files.
 * Creates jython scripts and packages them with supporting files into a .dup archive.
 */
@Service
@RequiredArgsConstructor
public class DUPExportService {

    private final EASClient easClient;

    /**
     * Generates a DUP export file based on the provided request.
     *
     * @param request The export request containing class and field selections
     * @return A byte array containing the .dup file
     * @throws IOException if there's an error generating the export
     */
    public byte[] generateDUPExport(DUPExportRequest request) throws IOException {

        // Generate the jython script
        String jythonScript = generateJythonScript(request);

        // Package everything into a .dup (zip) file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Add the generated jython script as dup_import_script.py
            addStringToZip(zos, jythonScript);

            // Add all predefined support files from resources/dupsupport
            addDupSupportFiles(zos);
        }


        return baos.toByteArray();
    }

    /**
     * Add all files from resources/dupsupport directory to the zip.
     *
     * @param zos The zip output stream
     * @throws IOException if there's an error reading or adding files
     */
    private void addDupSupportFiles(ZipOutputStream zos) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath:dupsupport/*");

            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    String filename = resource.getFilename();
                    if (filename != null) {
                        try (InputStream is = resource.getInputStream()) {
                            addStreamToZip(zos, is, filename);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new IOException("Failed to add support files", e);
        }
    }

    /**
     * Add a string content as a file to the zip.
     *
     * @param zos     The zip output stream
     * @param content The string content
     * @throws IOException if there's an error adding the content
     */
    private void addStringToZip(ZipOutputStream zos, String content) throws IOException {
        ZipEntry zipEntry = new ZipEntry("dup_import_script.py");
        zos.putNextEntry(zipEntry);

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
    }

    /**
     * Add an InputStream content to the zip.
     *
     * @param zos The zip output stream
     * @param inputStream The input stream to read from
     * @param entryName The name of the entry in the zip file
     * @throws IOException if there's an error adding the content
     */
    private void addStreamToZip(ZipOutputStream zos, InputStream inputStream, String entryName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        zos.closeEntry();
    }

    /**
     * Generates the jython script based on the export request.
     * Fetches all instances for each selected class and generates the import script.
     *
     * @param request The export request
     * @return The generated jython script as a string
     */
    private String generateJythonScript(DUPExportRequest request) {
        // Step 1: Collect all instances from all classes
        Map<String, Map<String, Object>> allInstances = new LinkedHashMap<>();
        Map<String, List<String>> classFieldsMap = new HashMap<>();

        for (ClassSelection classSelection : request.getClassSelections()) {
            if (!classSelection.isSelected() || classSelection.getFields().isEmpty()) {
                continue;
            }

            String className = classSelection.getClassName();
            List<String> selectedFields = classSelection.getFields().stream()
                    .filter(FieldSelection::isSelected)
                    .map(FieldSelection::getFieldName)
                    .collect(Collectors.toList());

            if (selectedFields.isEmpty()) {
                continue;
            }

            classFieldsMap.put(className, selectedFields);

            // Build slots parameter for API call
            List<String> allSlots = new ArrayList<>();
            allSlots.add("id");
            allSlots.add("name");
            allSlots.add("className");

            for (String field : selectedFields) {
                if (!allSlots.contains(field)) {
                    allSlots.add(field);
                }
            }

            String slotsParam = String.join("^", allSlots);

            try {
                var instances = easClient.getAllInstancesAsMap(request.getRepoId(), className, 1, slotsParam);

                for (Map<String, Object> instance : instances) {
                    String instanceId = (String) instance.get("id");
                    if (instanceId != null) {
                        allInstances.put(instanceId, instance);
                    }
                }
            } catch (Exception _) {
            }
        }

        // Step 2: Build ID mapping if prefix is specified
        Map<String, String> idMapping = buildIdMapping(allInstances.keySet(), request.getIdPrefix());

        // Step 3: Generate script with transformed IDs
        StringBuilder script = new StringBuilder();

        // Header with imports
        script.append("# DUP Export Script\n");
        script.append("# Generated by EssentialSync\n");
        script.append("# External Repository: ").append(request.getExternalRepositoryName()).append("\n");
        if (request.getIdPrefix() != null && !request.getIdPrefix().isEmpty()) {
            script.append("# ID Transformation: ").append(request.getIdPrefix()).append("_XXX\n");
        }
        script.append("\n");
        script.append("from java.lang import Boolean\n");
        script.append("from java.lang import Integer\n");
        script.append("from java.lang import Float\n");
        script.append("from java.lang import Double\n\n");

        // Define external repository
        script.append("defineExternalRepository(\"").append(request.getExternalRepositoryName()).append("\", \"\")\n\n");

        // Step 4: Group instances by class and create record variable mapping
        Map<String, List<Map<String, Object>>> instancesByClass = new LinkedHashMap<>();
        Map<String, String> idToRecordVar = new HashMap<>(); // Maps original ID to record variable name
        int recordCounter = 1;

        for (Map<String, Object> instance : allInstances.values()) {
            String className = (String) instance.get("className");
            instancesByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(instance);

            // Create unique record variable for this instance
            String originalId = (String) instance.get("id");
            if (originalId != null) {
                String recordVarName = "Record_" + recordCounter++;
                idToRecordVar.put(originalId, recordVarName);
            }
        }

        // FIRST PASS: Create all instances and set name field
        script.append("# ========================================\n");
        script.append("# FIRST PASS: Create all instances\n");
        script.append("# ========================================\n\n");

        for (Map.Entry<String, List<Map<String, Object>>> entry : instancesByClass.entrySet()) {
            String className = entry.getKey();
            List<Map<String, Object>> instances = entry.getValue();
            List<String> selectedFields = classFieldsMap.get(className);

            if (selectedFields == null || selectedFields.isEmpty()) {
                continue;
            }

            script.append("# Class: ").append(className).append(" (").append(instances.size()).append(" instances)\n");

            for (Map<String, Object> instanceMap : instances) {
                String originalId = (String) instanceMap.get("id");
                String instanceName = (String) instanceMap.get("name");

                if (originalId == null) {
                    continue;
                }

                // Get transformed ID and record variable name
                String transformedId = idMapping.getOrDefault(originalId, originalId);
                String recordVarName = idToRecordVar.get(originalId);

                // Create instance
                script.append(recordVarName).append("=EssentialGetInstance('").append(className).append("', ");
                script.append("u'").append(escapeForJython(transformedId)).append("', ");
                script.append("u'").append(escapeForJython(instanceName != null ? instanceName : "")).append("', ");
                script.append("u'").append(escapeForJython(transformedId)).append("', ");
                script.append("u'").append(request.getExternalRepositoryName()).append("')\n");

                // Immediately set the name field
                script.append("addIfNotThere(").append(recordVarName).append(", 'name', ");
                script.append("u'").append(escapeForJython(instanceName != null ? instanceName : "")).append("')\n");
            }

            script.append("\n");
        }

        // SECOND PASS: Populate all other fields for each instance
        script.append("# ========================================\n");
        script.append("# SECOND PASS: Populate all fields\n");
        script.append("# ========================================\n\n");

        for (Map.Entry<String, List<Map<String, Object>>> entry : instancesByClass.entrySet()) {
            String className = entry.getKey();
            List<Map<String, Object>> instances = entry.getValue();
            List<String> selectedFields = classFieldsMap.get(className);

            if (selectedFields == null || selectedFields.isEmpty()) {
                continue;
            }

            script.append("# Class: ").append(className).append(" - Adding fields\n");
            script.append("# Requested fields: ").append(String.join(", ", selectedFields)).append("\n\n");

            for (Map<String, Object> instanceMap : instances) {
                String originalId = (String) instanceMap.get("id");

                if (originalId == null) {
                    continue;
                }

                String recordVarName = idToRecordVar.get(originalId);

                // Add each selected field (skip 'name' as it was already added in first pass)
                for (String fieldName : selectedFields) {
                    if ("name".equals(fieldName)) {
                        continue; // Skip name field, already added in first pass
                    }

                    Object fieldValue = instanceMap.get(fieldName);

                    if (fieldValue != null) {
                        String valueStr = transformIdsInValue(fieldValue, idMapping, idToRecordVar);
                        script.append("addIfNotThere(").append(recordVarName).append(", '").append(fieldName).append("', ");
                        script.append(valueStr).append(")\n");
                    }
                }
            }

            script.append("\n");
        }

        return script.toString();
    }

    /**
     * Build mapping from original IDs to transformed IDs.
     * IDs already starting with the prefix are preserved.
     * Other IDs are transformed to {prefix}_{sequence}.
     *
     * @param originalIds Set of original instance IDs
     * @param prefix ID prefix (can be null or empty)
     * @return Map from original ID to transformed ID
     */
    private Map<String, String> buildIdMapping(Set<String> originalIds, String prefix) {
        Map<String, String> mapping = new HashMap<>();

        // If no prefix specified, return identity mapping
        if (prefix == null || prefix.trim().isEmpty()) {
            for (String id : originalIds) {
                mapping.put(id, id);
            }
            return mapping;
        }

        String prefixWithUnderscore = prefix.trim() + "_";
        Set<Integer> usedSequences = new HashSet<>();
        Pattern prefixPattern = Pattern.compile("^" + Pattern.quote(prefixWithUnderscore) + "(\\d+)$");

        // First pass: identify IDs that already have the correct prefix and extract used sequences
        for (String id : originalIds) {
            if (id.startsWith(prefixWithUnderscore)) {
                Matcher matcher = prefixPattern.matcher(id);
                if (matcher.matches()) {
                    try {
                        int sequence = Integer.parseInt(matcher.group(1));
                        usedSequences.add(sequence);
                    } catch (NumberFormatException e) {
                        // If parsing fails, just mark this ID as preserved
                    }
                }
                // Preserve IDs that already have the prefix
                mapping.put(id, id);
            }
        }

        // Second pass: transform IDs that don't have the prefix
        int nextSequence = 1;
        for (String id : originalIds) {
            if (!mapping.containsKey(id)) {
                // Find next available sequence number
                while (usedSequences.contains(nextSequence)) {
                    nextSequence++;
                }

                String newId = prefixWithUnderscore + nextSequence;
                mapping.put(id, newId);
                usedSequences.add(nextSequence);
                nextSequence++;
            }
        }

        return mapping;
    }

    /**
     * Transform IDs within a field value (handles strings, lists, maps).
     * When a value is a reference to another instance, uses the record variable instead of ID string.
     *
     * @param value The field value
     * @param idMapping Map from original ID to transformed ID
     * @param idToRecordVar Map from original ID to record variable name
     * @return Jython-formatted string with record variables for instance references
     */
    private String transformIdsInValue(Object value, Map<String, String> idMapping, Map<String, String> idToRecordVar) {
        switch (value) {
            case null -> {
                return "None";
            }
            case String strValue -> {
                // Check if this string is an instance ID that we have a record variable for
                if (idToRecordVar.containsKey(strValue)) {
                    // Return the record variable name directly (no quotes)
                    return idToRecordVar.get(strValue);
                }

                // Otherwise, replace all occurrences of mapped IDs in the string
                for (Map.Entry<String, String> entry : idMapping.entrySet()) {
                    strValue = strValue.replace(entry.getKey(), entry.getValue());
                }
                return "u'" + escapeForJython(strValue) + "'";
            }
            case List list1 -> {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;

                // Special case: if list has exactly one element and it's a reference map,
                // return just the record variable without list brackets
                if (list.size() == 1) {
                    Object singleElement = list.get(0);
                    if (singleElement instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> elementMap = (Map<String, Object>) singleElement;
                        if (elementMap.containsKey("id")) {
                            Object idValue = elementMap.get("id");
                            if (idValue instanceof String refId && idToRecordVar.containsKey(refId)) {
                                // Return just the record variable, no list brackets
                                return idToRecordVar.get(refId);
                            }
                        }
                    } else if (singleElement instanceof String strId && idToRecordVar.containsKey(strId)) {
                        // Single string ID that maps to a record variable
                        return idToRecordVar.get(strId);
                    }
                }

                // Normal case: process list elements
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(transformIdsInValue(list.get(i), idMapping, idToRecordVar));
                }
                sb.append("]");
                return sb.toString();
            }
            case Map map1 -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;

                // Check if this is a reference object with an 'id' field
                // These typically have id, name, and className fields
                if (map.containsKey("id")) {
                    Object idValue = map.get("id");
                    if (idValue instanceof String refId) {
                        // Use record variable if available
                        if (idToRecordVar.containsKey(refId)) {
                            return idToRecordVar.get(refId);
                        }
                        // Otherwise use transformed ID as string
                        String transformedRefId = idMapping.getOrDefault(refId, refId);
                        return "u'" + escapeForJython(transformedRefId) + "'";
                    }
                }

                // Otherwise, format as a map
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append("'").append(escapeForJython(entry.getKey())).append("': ");
                    sb.append(transformIdsInValue(entry.getValue(), idMapping, idToRecordVar));
                }
                sb.append("}");
                return sb.toString();
            }


            // Handle primitives
            case Number number -> {
                return value.toString();
            }
            case Boolean b -> {
                return b ? "True" : "False";
            }
            default -> {
            }
        }

        // Default: convert to string and escape
        return "u'" + escapeForJython(value.toString()) + "'";
    }

    /**
     * Escape string for use in Jython script.
     */
    private String escapeForJython(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
