package com.qtzar.essentialsexport.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EASRepositoriesPropertiesTest {

    @Autowired
    private EASRepositoriesProperties easRepositoriesProperties;

    @Test
    void testPropertiesLoadedFromConfig() {
        // Assert
        assertNotNull(easRepositoriesProperties);
        assertNotNull(easRepositoriesProperties.getRepositories());
        assertFalse(easRepositoriesProperties.getRepositories().isEmpty());
    }

    @Test
    void testRepositoryProperties() {
        // Act
        List<EASRepositoriesProperties.Repository> repositories = easRepositoriesProperties.getRepositories();

        // Assert
        assertTrue(repositories.size() >= 1);
        EASRepositoriesProperties.Repository firstRepo = repositories.get(0);
        assertNotNull(firstRepo.getName());
        assertNotNull(firstRepo.getRepoId());
    }

    @Test
    void testRepositoryGettersAndSetters() {
        // Arrange
        EASRepositoriesProperties.Repository repository = new EASRepositoriesProperties.Repository();

        // Act
        repository.setName("Test Repository");
        repository.setRepoId("test-repo-123");

        // Assert
        assertEquals("Test Repository", repository.getName());
        assertEquals("test-repo-123", repository.getRepoId());
    }

    @Test
    void testSetRepositories() {
        // Arrange
        EASRepositoriesProperties properties = new EASRepositoriesProperties();
        List<EASRepositoriesProperties.Repository> repositories = new ArrayList<>();

        EASRepositoriesProperties.Repository repo1 = new EASRepositoriesProperties.Repository();
        repo1.setName("Repo 1");
        repo1.setRepoId("repo1");

        EASRepositoriesProperties.Repository repo2 = new EASRepositoriesProperties.Repository();
        repo2.setName("Repo 2");
        repo2.setRepoId("repo2");

        repositories.add(repo1);
        repositories.add(repo2);

        // Act
        properties.setRepositories(repositories);

        // Assert
        assertEquals(2, properties.getRepositories().size());
        assertEquals("Repo 1", properties.getRepositories().get(0).getName());
        assertEquals("Repo 2", properties.getRepositories().get(1).getName());
    }

    @Test
    void testEmptyRepositories() {
        // Arrange
        EASRepositoriesProperties properties = new EASRepositoriesProperties();

        // Assert
        assertNotNull(properties.getRepositories());
        assertTrue(properties.getRepositories().isEmpty());
    }

    @Test
    void testRepositoryWithNullValues() {
        // Arrange
        EASRepositoriesProperties.Repository repository = new EASRepositoriesProperties.Repository();

        // Assert
        assertNull(repository.getName());
        assertNull(repository.getRepoId());
    }

    @Test
    void testAddRepositoryDynamically() {
        // Arrange
        EASRepositoriesProperties properties = new EASRepositoriesProperties();
        EASRepositoriesProperties.Repository newRepo = new EASRepositoriesProperties.Repository();
        newRepo.setName("Dynamic Repo");
        newRepo.setRepoId("dynamic-123");

        // Act
        properties.getRepositories().add(newRepo);

        // Assert
        assertEquals(1, properties.getRepositories().size());
        assertEquals("Dynamic Repo", properties.getRepositories().get(0).getName());
    }
}
