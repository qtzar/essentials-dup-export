package com.qtzar.essentialsexport.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "eas")
@Getter
@Setter
public class EASRepositoriesProperties {

    private List<Repository> repositories = new ArrayList<>();

    @Getter
    @Setter
    public static class Repository {
        private String name;
        private String repoId;
    }
}
