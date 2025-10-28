package com.qtzar.essentialsexport.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "scheduling")
@Getter
@Setter
public class SchedulingProperties {

    private boolean enabled = false;
    private Map<String, SyncScheduleConfig> syncs = new HashMap<>();

    @Getter
    @Setter
    public static class SyncScheduleConfig {
        private boolean enabled = false;
        private String cron;
    }
}
