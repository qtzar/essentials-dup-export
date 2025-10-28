package com.qtzar.essentialsexport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EssentialExportApplication {

    static void main(String[] args) {
        SpringApplication.run(EssentialExportApplication.class, args);
    }
}
