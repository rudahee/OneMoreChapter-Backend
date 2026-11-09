package org.rudahee.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class UnCapituloMasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnCapituloMasBackendApplication.class, args);
    }

}
