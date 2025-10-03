package com.tecnolog.autocoleta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutocoletaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutocoletaApplication.class, args);
    }
}
