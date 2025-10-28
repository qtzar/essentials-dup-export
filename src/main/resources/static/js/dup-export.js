// DUP Export Builder JavaScript - Redesigned for scalability

let allClasses = [];
let filteredClasses = [];
let selectedClasses = new Map();  // className -> {selected: boolean, fields: Set<fieldName>}
let currentActiveClass = null;
let classMetadata = {};  // Full metadata object from API with nested slots
let repositories = [];  // Available EAS repositories
let selectedRepoId = null;  // Currently selected repository ID

// Virtual scrolling configuration
const ITEMS_PER_PAGE = 30;
let currentPage = 0;
let totalPages = 0;

/**
 * Initialize the page
 */
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('repoName').addEventListener('input', validateForm);
    loadRepositories();
});

/**
 * Load available EAS repositories from the API
 */
async function loadRepositories() {
    try {
        const response = await fetch('/api/dup/repositories');
        if (!response.ok) {
            throw new Error('Failed to fetch repositories');
        }

        repositories = await response.json();
        const repoSelect = document.getElementById('repoSelect');

        // Populate dropdown
        repositories.forEach(repo => {
            const option = document.createElement('option');
            option.value = repo.repoId;
            option.textContent = repo.name;
            repoSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Error loading repositories:', error);
        alert('Failed to load repositories. Please refresh the page.');
    }
}

/**
 * Handle repository selection change
 */
function handleRepositoryChange() {
    const repoSelect = document.getElementById('repoSelect');
    selectedRepoId = repoSelect.value;

    if (selectedRepoId) {
        // Clear any existing class/field selections
        clearSelections();

        // Automatically load classes for the selected repository
        loadAvailableClasses();
    } else {
        clearSelections();
    }
}

/**
 * Clear all class and field selections
 */
function clearSelections() {
    allClasses = [];
    filteredClasses = [];
    selectedClasses.clear();
    currentActiveClass = null;
    classMetadata = {};

    // Clear UI
    document.getElementById('classList').innerHTML = '<div class="empty-state"><p>Select a repository to load classes</p></div>';
    document.getElementById('fieldsList').innerHTML = '<div class="empty-state"><p>👈 Select a class from the left panel to view and select its fields</p></div>';
    document.getElementById('currentClassName').textContent = 'Select a class to view its fields';
    updateSelectedSummary();
}

/**
 * Load available EAS classes from the API
 */
async function loadAvailableClasses() {
    if (!selectedRepoId) {
        alert('Please select a repository first');
        return;
    }

    const classList = document.getElementById('classList');
    classList.innerHTML = '<div class="loading">Loading classes from EAS</div>';

    try {
        const response = await fetch(`/api/dup/classes?repoId=${encodeURIComponent(selectedRepoId)}`);
        if (!response.ok) {
            throw new Error('Failed to fetch classes');
        }

        // Response format: { "classes": { "CLASS_NAME_1": { "slots": { "Slot_1": {...}, ... } }, ... } }
        const data = await response.json();

        // Extract the classes object from the response
        classMetadata = data.classes || data;

        // Validate we have an object
        if (!classMetadata || typeof classMetadata !== 'object') {
            throw new Error('Invalid response format from API');
        }

        // Extract class names from the nested structure
        allClasses = Object.keys(classMetadata).map(className => ({
            name: className,
            slots: classMetadata[className]?.slots || {}
        }));

        filteredClasses = [...allClasses];

        // Initialize selectedClasses map
        selectedClasses.clear();
        allClasses.forEach(cls => {
            selectedClasses.set(cls.name, {
                selected: false,
                fields: new Set()
            });
        });

        renderClassList();
        showStatus(`Loaded ${allClasses.length} classes from EAS`, 'success');

    } catch (error) {
        console.error('Error loading classes:', error);
        classList.innerHTML = `
            <div class="no-classes">
                <p>Error loading classes: ${error.message}</p>
                <button class="btn btn-info btn-small" onclick="loadAvailableClasses()">Retry</button>
            </div>
        `;
        showStatus('Error loading classes from EAS', 'error');
    }
}

/**
 * Render the class list in the left panel with pagination
 */
function renderClassList() {
    const classList = document.getElementById('classList');

    if (filteredClasses.length === 0) {
        classList.innerHTML = '<div class="no-classes"><p>No classes found</p></div>';
        return;
    }

    // Calculate pagination
    totalPages = Math.ceil(filteredClasses.length / ITEMS_PER_PAGE);
    currentPage = Math.min(currentPage, totalPages - 1);
    currentPage = Math.max(currentPage, 0);

    const startIndex = currentPage * ITEMS_PER_PAGE;
    const endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredClasses.length);
    const visibleClasses = filteredClasses.slice(startIndex, endIndex);

    // Clear and add pagination controls at top
    classList.innerHTML = '';

    if (totalPages > 1) {
        const paginationTop = createPaginationControls();
        classList.appendChild(paginationTop);
    }

    // Render visible classes
    visibleClasses.forEach(cls => {
        const className = cls.name;
        const classData = selectedClasses.get(className);

        const item = document.createElement('div');
        item.className = 'class-list-item';
        item.dataset.className = className;

        if (currentActiveClass === className) {
            item.classList.add('active');
        }

        if (classData && classData.fields.size > 0) {
            item.classList.add('has-selection');
        }

        const fieldCount = classData && classData.fields.size > 0
            ? `${classData.fields.size} selected`
            : '';

        item.innerHTML = `
            <div class="class-label" onclick="selectClassForViewing('${className}')">${className.replace(/_/g, ' ')}</div>
            ${fieldCount ? `<span class="field-count">${fieldCount}</span>` : ''}
        `;

        classList.appendChild(item);
    });

    // Add pagination controls at bottom
    if (totalPages > 1) {
        const paginationBottom = createPaginationControls();
        classList.appendChild(paginationBottom);
    }
}

/**
 * Create pagination controls
 */
function createPaginationControls() {
    const pagination = document.createElement('div');
    pagination.className = 'pagination-controls';

    const startIndex = currentPage * ITEMS_PER_PAGE + 1;
    const endIndex = Math.min((currentPage + 1) * ITEMS_PER_PAGE, filteredClasses.length);

    pagination.innerHTML = `
        <button class="pagination-btn" onclick="goToPage(0)" ${currentPage === 0 ? 'disabled' : ''}>
            ««
        </button>
        <button class="pagination-btn" onclick="goToPage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>
            ‹
        </button>
        <span class="pagination-info">
            ${startIndex}-${endIndex} of ${filteredClasses.length}
        </span>
        <button class="pagination-btn" onclick="goToPage(${currentPage + 1})" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>
            ›
        </button>
        <button class="pagination-btn" onclick="goToPage(${totalPages - 1})" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>
            »»
        </button>
    `;

    return pagination;
}

/**
 * Navigate to a specific page
 */
function goToPage(page) {
    if (page >= 0 && page < totalPages) {
        currentPage = page;
        renderClassList();
    }
}

/**
 * Filter classes based on search input
 */
function filterClasses(searchTerm) {
    if (!searchTerm || searchTerm.trim() === '') {
        filteredClasses = [...allClasses];
    } else {
        const term = searchTerm.toLowerCase();
        filteredClasses = allClasses.filter(cls => {
            return cls.name.toLowerCase().includes(term);
        });
    }

    // Reset to first page when filtering
    currentPage = 0;
    renderClassList();
}

/**
 * Select a class to view its fields (from pre-loaded metadata)
 */
function selectClassForViewing(className) {
    currentActiveClass = className;
    renderClassList();

    const currentClassNameDiv = document.getElementById('currentClassName');
    const fieldsList = document.getElementById('fieldsList');

    currentClassNameDiv.textContent = className.replace(/_/g, ' ');

    try {
        // Get slots from the pre-loaded metadata
        const classData = classMetadata[className];
        if (!classData || !classData.slots) {
            fieldsList.innerHTML = '<div class="empty-state"><p>No fields available for this class</p></div>';
            return;
        }

        // Convert slots object to array format and sort alphabetically
        const slots = classData.slots;
        const fields = Object.keys(slots)
            .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
            .map(slotName => ({
                name: slotName,
                ...slots[slotName]
            }));

        renderFieldsList(className, fields);

    } catch (error) {
        console.error('Error loading fields:', error);
        fieldsList.innerHTML = `
            <div class="empty-state">
                <p>Error loading fields: ${error.message}</p>
            </div>
        `;
    }
}

/**
 * Render fields list in the right panel
 */
function renderFieldsList(className, fields) {
    const fieldsList = document.getElementById('fieldsList');
    const classData = selectedClasses.get(className);

    if (!fields || fields.length === 0) {
        fieldsList.innerHTML = '<div class="empty-state"><p>No fields available for this class</p></div>';
        return;
    }

    fieldsList.innerHTML = `
        <div class="fields-header">
            <div>${fields.length} field(s) available</div>
            <div class="fields-actions">
                <button class="btn-small btn-select-all" onclick="selectAllFields('${className}')">
                    Select All
                </button>
                <button class="btn-small btn-deselect-all" onclick="deselectAllFields('${className}')">
                    Deselect All
                </button>
            </div>
        </div>
        <div class="field-grid" id="fieldGrid"></div>
    `;

    const fieldGrid = document.getElementById('fieldGrid');

    fields.forEach(field => {
        const fieldName = field.name;
        const isSelected = classData && classData.fields.has(fieldName);

        const fieldItem = document.createElement('div');
        fieldItem.className = 'field-item';
        if (isSelected) {
            fieldItem.classList.add('selected');
        }

        // Escape field name for safe use in HTML attributes
        const safeFieldName = fieldName.replace(/'/g, "\\'");

        fieldItem.innerHTML = `
            <input type="checkbox"
                   id="field_${className}_${fieldName}"
                   ${isSelected ? 'checked' : ''}
                   onchange="toggleFieldSelection('${className}', '${safeFieldName}', this.checked)">
            <label for="field_${className}_${fieldName}">${fieldName.replace(/_/g, ' ')}</label>
        `;

        fieldGrid.appendChild(fieldItem);
    });
}

/**
 * Toggle field selection
 */
function toggleFieldSelection(className, fieldName, isSelected) {
    const classData = selectedClasses.get(className);

    if (!classData) return;

    if (isSelected) {
        classData.fields.add(fieldName);
    } else {
        classData.fields.delete(fieldName);
    }

    // Update field item styling
    const fieldItem = document.getElementById(`field_${className}_${fieldName}`)?.closest('.field-item');
    if (fieldItem) {
        fieldItem.classList.toggle('selected', isSelected);
    }

    // Update class list to show field count
    renderClassList();
    updateSelectedSummary();
    validateForm();
}

/**
 * Select all fields for current class
 */
function selectAllFields(className) {
    const classData = selectedClasses.get(className);
    if (!classData) return;

    // Get slots from metadata
    const slots = classMetadata[className]?.slots;
    if (!slots) return;

    const fieldNames = Object.keys(slots);
    fieldNames.forEach(fieldName => {
        classData.fields.add(fieldName);

        const checkbox = document.getElementById(`field_${className}_${fieldName}`);
        if (checkbox) checkbox.checked = true;

        const fieldItem = checkbox?.closest('.field-item');
        if (fieldItem) fieldItem.classList.add('selected');
    });

    renderClassList();
    updateSelectedSummary();
    validateForm();
}

/**
 * Deselect all fields for current class
 */
function deselectAllFields(className) {
    const classData = selectedClasses.get(className);
    if (!classData) return;

    classData.fields.clear();

    // Update all checkboxes
    const checkboxes = document.querySelectorAll(`input[id^="field_${className}_"]`);
    checkboxes.forEach(cb => {
        cb.checked = false;
        const fieldItem = cb.closest('.field-item');
        if (fieldItem) fieldItem.classList.remove('selected');
    });

    renderClassList();
    updateSelectedSummary();
    validateForm();
}

/**
 * Remove a class from selection entirely
 */
function removeClassSelection(className) {
    const classData = selectedClasses.get(className);
    if (classData) {
        classData.fields.clear();
    }

    // If this was the active class, refresh the fields view
    if (currentActiveClass === className) {
        selectClassForViewing(className);
    }

    renderClassList();
    updateSelectedSummary();
    validateForm();
}

/**
 * Update the selected classes summary
 */
function updateSelectedSummary() {
    const summaryContent = document.getElementById('selectedSummary');

    const selectedList = Array.from(selectedClasses.entries())
        .filter(([_, data]) => data.fields.size > 0)
        .map(([className, data]) => ({
            className,
            fieldCount: data.fields.size
        }));

    if (selectedList.length === 0) {
        summaryContent.innerHTML = '<p class="no-selection">No classes selected yet</p>';
        return;
    }

    summaryContent.innerHTML = selectedList.map(item => `
        <div class="summary-badge">
            <span>${item.className.replace(/_/g, ' ')}</span>
            <span class="field-count-badge">${item.fieldCount} field${item.fieldCount !== 1 ? 's' : ''}</span>
            <span class="badge-close" onclick="removeClassSelection('${item.className}')" title="Remove">×</span>
        </div>
    `).join('');
}

/**
 * Validate form and enable/disable generate button
 */
function validateForm() {
    const repoName = document.getElementById('repoName').value.trim();
    const hasSelectedFields = Array.from(selectedClasses.values())
        .some(data => data.fields.size > 0);

    const generateBtn = document.getElementById('generateBtn');
    generateBtn.disabled = !(repoName && hasSelectedFields);
}

/**
 * Generate DUP export
 */
async function generateExport() {
    const repoName = document.getElementById('repoName').value.trim();
    const idPrefix = document.getElementById('idPrefix').value.trim();

    if (!selectedRepoId) {
        showStatus('Please select a repository', 'error');
        return;
    }

    if (!repoName) {
        showStatus('Please enter an external repository name', 'error');
        return;
    }

    // Build the export request
    const classSelections = [];
    selectedClasses.forEach((classData, className) => {
        if (classData.fields.size > 0) {
            classSelections.push({
                className: className,
                selected: true,
                fields: Array.from(classData.fields).map(fieldName => ({
                    fieldName: fieldName,
                    selected: true
                }))
            });
        }
    });

    if (classSelections.length === 0) {
        showStatus('Please select at least one class with fields', 'error');
        return;
    }

    const exportRequest = {
        repoId: selectedRepoId,
        externalRepositoryName: repoName,
        idPrefix: idPrefix || null,
        classSelections: classSelections
    };

    // Show progress overlay
    showProgressOverlay(true);
    updateProgress('Preparing export...', `Processing ${classSelections.length} class(es)`);

    // Add class details
    const progressDetails = document.getElementById('progressDetails');
    progressDetails.innerHTML = '';
    classSelections.forEach((cls, index) => {
        const fieldCount = cls.fields.length;
        addProgressItem(`${index + 1}. ${cls.className} (${fieldCount} field${fieldCount !== 1 ? 's' : ''})`, 'processing');
    });

    document.getElementById('generateBtn').disabled = true;

    try {
        updateProgress('Fetching instances from EAS...', 'This may take a moment for large datasets');

        const response = await fetch('/api/dup/export', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(exportRequest)
        });

        if (response.ok) {
            updateProgress('Generating Jython script...', 'Creating DUP package');

            // Download the file
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${repoName}.dup`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            updateProgress('Export Complete!', `Downloaded as ${repoName}.dup`);

            // Hide overlay after success
            setTimeout(() => {
                showProgressOverlay(false);
                showStatus(`✅ Export generated successfully! Downloaded as ${repoName}.dup`, 'success');
            }, 1500);
        } else {
            const errorText = await response.text();
            let errorMessage = 'Unknown error occurred';

            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorJson.error || errorText;
            } catch (e) {
                errorMessage = errorText || response.statusText;
            }

            showProgressOverlay(false);
            showStatus(`❌ Error generating export: ${errorMessage}`, 'error');
            console.error('Export error:', errorMessage);
        }
    } catch (error) {
        showProgressOverlay(false);
        showStatus(`❌ Error generating export: ${error.message}`, 'error');
        console.error('Export error:', error);
    } finally {
        validateForm();
    }
}

/**
 * Show/hide progress overlay
 */
function showProgressOverlay(show) {
    const overlay = document.getElementById('progressOverlay');
    if (show) {
        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    } else {
        overlay.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
}

/**
 * Update progress message
 */
function updateProgress(title, message) {
    document.getElementById('progressTitle').textContent = title;
    document.getElementById('progressMessage').textContent = message;
}

/**
 * Add a progress item to the details list
 */
function addProgressItem(text, status = 'processing') {
    const progressDetails = document.getElementById('progressDetails');
    const item = document.createElement('div');
    item.className = `progress-item ${status}`;
    item.textContent = text;
    progressDetails.appendChild(item);
}

/**
 * Show status message
 */
function showStatus(message, type) {
    const statusDiv = document.getElementById('exportStatus');
    statusDiv.textContent = message;
    statusDiv.className = `export-status ${type}`;

    // Auto-hide success messages after 5 seconds
    if (type === 'success') {
        setTimeout(() => {
            statusDiv.className = 'export-status';
        }, 5000);
    }
}

/**
 * Reset the form
 */
function resetForm() {
    if (confirm('Are you sure you want to reset the form? All selections will be lost.')) {
        document.getElementById('repoName').value = '';
        document.getElementById('classSearchInput').value = '';

        selectedClasses.clear();
        allClasses.forEach(cls => {
            const className = cls.name || cls.className || cls.id;
            selectedClasses.set(className, {
                selected: false,
                fields: new Set()
            });
        });

        currentActiveClass = null;
        filteredClasses = [...allClasses];

        renderClassList();
        document.getElementById('fieldsList').innerHTML = `
            <div class="empty-state">
                <p>👈 Select a class from the left panel to view and select its fields</p>
            </div>
        `;
        document.getElementById('currentClassName').textContent = 'Select a class to view its fields';
        updateSelectedSummary();
        document.getElementById('exportStatus').className = 'export-status';
        validateForm();
    }
}
