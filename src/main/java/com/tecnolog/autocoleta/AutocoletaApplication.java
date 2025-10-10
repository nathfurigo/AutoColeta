package com.tecnolog.autocoleta;

import com.tecnolog.autocoleta.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableFeignClients
@EnableConfigurationProperties(AppProperties.class)
public class AutocoletaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutocoletaApplication.class, args);
    }

}