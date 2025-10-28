# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Essential DUP Exporter** is a Spring Boot 3.5 application that exports data from Essential Architecture Studio (EAS) repositories and packages it as DUP (Data Update Package) files. The application provides a web-based interface for selecting classes and fields from EAS repositories, then generates Jython scripts packaged with support files into a .dup (ZIP) archive for import into other EAS instances.

**Key Technology**: Java 25, Spring Boot 3.5.6, Maven, Lombok, Apache POI, Thymeleaf

## Build & Run Commands

### Maven Operations
```bash
# Build the project
mvn clean install

# Run the application (default profile)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=DUPExportServiceTest

# Run specific test method
mvn test -Dtest=DUPExportServiceTest#testMethodName

# Run with coverage
mvn test jacoco:report

# Package without tests
mvn clean package -DskipTests
```

### Running the Application
- Access web UI at: `http://localhost:8080`
- Swagger API docs at: `http://localhost:8080/swagger-ui.html`
- Spring Boot Actuator: `http://localhost:8080/actuator`

## Architecture

### Three-Layer Architecture

**Controllers** (`controllers/`)
- `DUPExportController`: Main REST API for DUP export operations
  - POST `/api/dup/export` - Generate and download DUP file
  - GET `/api/dup/repositories` - List available EAS repositories
  - GET `/api/dup/classes?repoId={id}` - Get class metadata with slots

**Services** (`services/`)
- `DUPExportService`: Core business logic for DUP generation
  - Fetches instances from EAS via EASClient
  - Generates Jython scripts with ID transformation
  - Packages scripts with support files into .dup (ZIP) archive
  - Handles ID mapping: transforms IDs to `{prefix}_{sequence}` format while preserving existing prefixed IDs

**Clients** (`clients/`)
- `EASClient`: REST client for Essential Architecture Studio API
  - Base URL configured via `eas.endpoint` property in application.yml
  - Implements OAuth token management with refresh token support
  - `getClassesMetadata(repoId)` - Retrieve class definitions and slots
  - `getAllInstancesAsMap(repoId, className, depth, slots)` - Fetch instances with dynamic field access
  - Returns data as `Map<String, Object>` to support arbitrary EAS slots

### Data Flow

1. User selects repository, classes, and fields via web UI
2. `DUPExportController` receives `DUPExportRequest` with:
   - `repoId` - EAS repository identifier
   - `externalRepositoryName` - Target repository name for import
   - `idPrefix` (optional) - Prefix for ID transformation
   - `classSelections[]` - List of selected classes and their fields
3. `DUPExportService.generateDUPExport()`:
   - Fetches all instances for selected classes via `EASClient`
   - Builds ID mapping (original → transformed IDs)
   - Generates Jython script with `EssentialGetInstance` calls
   - Transforms all ID references in field values
   - Packages script with support files from `resources/dupsupport/`
4. Returns .dup file (ZIP format) for download

### Configuration

**Multiple Spring Profiles**:
- `application.yml` - Base configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

**EAS Configuration** (`application.yml`):
```yaml
eas:
  endpoint: https://your-essential-instance.essentialintelligence.com/api
  username: apiuser@yourdomain.com
  password: [credential]
  apiKey: [credential]
  repositories:
    - name: "Production"
      repoId: "repoId"
    - name: "Pre-Production"
      repoId: "repoId"
    - name: "Sandbox"
      repoId: "repoId"
```

Configuration is bound via `@ConfigurationProperties` in `EASRepositoriesProperties`.

### DUP File Structure

The generated .dup file is a ZIP archive containing:
- `dup_import_script.py` - Generated Jython script with instance data
- `standardFunctions.py` - Helper functions for EAS import
- `update.info` - Metadata about the update package
- `updatepack.xsd` - XML schema definition

### ID Transformation Logic

The service implements sophisticated ID transformation in `DUPExportService.buildIdMapping()`:
1. IDs already matching `{prefix}_{number}` pattern are preserved
2. Extracts used sequence numbers from existing prefixed IDs
3. Transforms unprefixed IDs to `{prefix}_{next_available_sequence}`
4. Applies transformation recursively to all field values (strings, lists, maps, reference objects)

### Authentication Flow

`EASClient` manages OAuth authentication:
1. Initial auth: POST `/oauth/token` with username/password
2. Token refresh: POST `/oauth/token` with refresh token
3. Auto-refresh 60 seconds before expiration
4. All API calls include `Authorization: Bearer {token}` header

## Key Dependencies

- **Spring Boot Actuator** - Health checks and metrics
- **Lombok** - Reduces boilerplate code (@Data, @Slf4j, @RequiredArgsConstructor)
- **SpringDoc OpenAPI** - API documentation (Swagger UI)

## Frontend Structure

**Static Resources** (`src/main/resources/static/`):
- `index.html` - Main DUP export builder interface
- `css/dashboard.css` - Base styles
- `css/dup-export.css` - Export builder specific styles
- `js/dup-export.js` - Client-side logic for class/field selection

The UI uses a two-panel layout:
- Left panel: searchable class list
- Right panel: field selection for active class
- Bottom summary: shows all selected classes and field counts

## Development Notes

- Lombok annotation processor configured in Maven compiler plugin
- Spring DevTools enabled for hot reloading in development
- Default logging level for `com.qtzar.essentialexport` is ERROR (configured per profile)
- API uses `u'...'` Python unicode string literals in Jython script generation
- All strings in Jython scripts must be escaped for special characters: `\ ' " \n \r`