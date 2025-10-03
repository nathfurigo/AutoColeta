package com.tecnolog.autocoleta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableFeignClients(basePackages = "com.tecnolog.autocoleta")
public class AutocoletaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutocoletaApplication.class, args);
    }
}
