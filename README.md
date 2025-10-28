# Essential DUP Exporter

A Spring Boot application for exporting data from Essential Architecture Studio (EAS) repositories as DUP (Data Update Package) files for migration of specific class instances between EAS instances.

## Overview

Essential DUP Exporter provides a web-based interface to:
- Connect to Essential Architecture Studio repositories
- Select specific classes and fields to export
- Transform instance IDs with custom prefixes
- Generate .dup files containing Jython scripts for data import

The application streamlines the process of migrating data between EAS environments (development, staging, production) by packaging selected architecture data into importable DUP files.

## Features

- **Selective Export**: Choose specific EAS classes and their fields to include in the export
- **Multiple Repository Support**: Configure and switch between multiple EAS repositories
- **ID Transformation**: Apply custom ID prefixes to prevent conflicts during import
  - Preserves existing prefixed IDs
  - Intelligently assigns new sequential IDs for unprefixed instances
- **Smart Reference Handling**: Automatically transforms IDs in related objects and lists
- **Web UI**: User-friendly interface for building exports
- **REST API**: Programmatic access for automation

## Technology Stack

- **Java 25**
- **Spring Boot 4.0**
- **Maven**
- **Lombok**
- **Thymeleaf** (for web UI)

## Prerequisites

- Java 25 or higher
- Maven 3.9+
- Access to an Essential Architecture Studio instance with API credentials

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd essentials-dup-export
mvn clean install
```

### 2. Configure

Edit `src/main/resources/application.yml`:

```yaml
eas:
  endpoint: https://your-essential-instance.essentialintelligence.com/api
  username: your-api-username@yourdomain.com
  password: your-api-password
  apiKey: your-api-key
  repositories:
    - name: "Production"
      repoId: "prod-repo-id"
    - name: "Development"
      repoId: "dev-repo-id"
```

### 3. Run

```bash
# Run with default profile
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Access

Open your browser to: `http://localhost:8080`

## Configuration

### Application Profiles

The application supports multiple Spring profiles for different environments:

- **Default** (`application.yml`): Base configuration
- **Development** (`application-dev.yml`): Development-specific settings
- **Production** (`application-prod.yml`): Production-specific settings

Activate a profile using:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or via environment variable:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

### EAS Connection Settings

Configure your Essential Architecture Studio connection in `application.yml`:

| Property | Description | Example |
|----------|-------------|---------|
| `eas.endpoint` | EAS API base URL | `https://essential.company.com/api` |
| `eas.username` | API username | `apiuser@company.com` |
| `eas.password` | API password | `SecurePassword123` |
| `eas.apiKey` | API key for authentication | `your-api-key-here` |

### Repository Configuration

Define multiple EAS repositories for the application to connect to:

```yaml
eas:
  repositories:
    - name: "Production Repository"
      repoId: "prod-repo-123"
    - name: "Sandbox Repository"
      repoId: "sandbox-repo-456"
    - name: "Development Repository"
      repoId: "dev-repo-789"
```

Each repository configuration includes:
- **name**: Display name shown in the UI
- **repoId**: Unique repository identifier from EAS

### Authentication

The application uses OAuth2 bearer token authentication with automatic token refresh:
- Initial authentication uses username/password
- Tokens are automatically refreshed 60 seconds before expiration
- Refresh tokens are used when available to minimize re-authentication

## Usage

### Web Interface

1. Navigate to `http://localhost:8080`
2. Select a repository from the dropdown
3. Choose the classes you want to export
4. Select specific fields for each class
5. (Optional) Specify an ID prefix for transformation
6. Enter an external repository name
7. Click "Generate DUP" to download the .dup file

### REST API

#### Get Repositories

```bash
GET /api/dup/repositories
```

Returns list of configured repositories.

#### Get Classes

```bash
GET /api/dup/classes?repoId={repoId}
```

Returns class metadata including available fields for a repository.

#### Generate DUP Export

```bash
POST /api/dup/export
Content-Type: application/json

{
  "repoId": "prod-repo-123",
  "externalRepositoryName": "Migration Package",
  "idPrefix": "MIG",
  "classSelections": [
    {
      "className": "Business_Capability",
      "selected": true,
      "fields": [
        {"fieldName": "name", "selected": true},
        {"fieldName": "description", "selected": true}
      ]
    }
  ]
}
```

Returns a .dup file for download.

## Generated DUP File

The exported .dup file is a ZIP archive containing:

- **dup_import_script.py**: Jython script with `EssentialGetInstance` calls for each exported instance
- **standardFunctions.py**: Helper functions for EAS import operations
- **update.info**: Metadata about the update package
- **updatepack.xsd**: XML schema definition

## ID Transformation

When you specify an ID prefix (e.g., "MIG"), the application:

1. **Preserves** IDs already matching the pattern `{prefix}_{number}` (e.g., `MIG_42`)
2. **Identifies** used sequence numbers to avoid conflicts
3. **Transforms** unprefixed IDs to `{prefix}_{next_sequence}` (e.g., `old-id` → `MIG_1`)
4. **Updates** all ID references in field values, including nested objects and lists

This ensures clean data migration without ID conflicts in the target repository.

## Testing

Run the test suite:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DUPExportServiceTest

# Run with coverage report
mvn test jacoco:report
```

The application includes 57+ tests covering:
- Model classes
- Service logic
- Controller endpoints
- Integration scenarios

## Building for Production

Create a production-ready JAR:

```bash
mvn clean package -DskipTests
```

The executable JAR will be in `target/EssentialExport-0.0.1-SNAPSHOT.jar`

Run it with:

```bash
java -jar target/EssentialExport-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Monitoring

Spring Boot Actuator endpoints are available at:

```
http://localhost:8080/actuator
```

Available endpoints include:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information

## Troubleshooting

### Cannot Connect to EAS

- Verify `eas.endpoint` is correct and accessible
- Check that `apiKey`, `username`, and `password` are valid
- Ensure network connectivity to the EAS instance

### Authentication Failures

- Confirm API credentials are correct
- Check that the API user has appropriate permissions in EAS
- Verify the API key is active and not expired

### Empty or Missing Classes

- Ensure the `repoId` is correct for your target repository
- Verify the API user has read access to the repository
- Check EAS instance is running and accessible

### Build Failures

- Ensure Java 25 is installed: `java -version`
- Verify Maven is properly configured: `mvn -version`
- Clear Maven cache: `rm -rf ~/.m2/repository` and rebuild

## Project Structure

```
src/
├── main/
│   ├── java/com/qtzar/essentialsexport/
│   │   ├── clients/          # EAS API client
│   │   ├── configuration/    # Spring configuration
│   │   ├── controllers/      # REST endpoints
│   │   ├── model/            # Data models
│   │   └── services/         # Business logic
│   └── resources/
│       ├── dupsupport/       # DUP package templates
│       ├── static/           # Web UI assets
│       └── application.yml   # Configuration
└── test/                     # Comprehensive test suite
```

## License

[Your License Here]

## Support

For issues or questions, please contact [your-contact-info] or create an issue in the repository.
