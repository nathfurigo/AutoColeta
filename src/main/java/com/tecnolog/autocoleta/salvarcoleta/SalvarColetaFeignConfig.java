package com.tecnolog.autocoleta.salvarcoleta;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableFeignClients(basePackageClasses = SalvarColetaFeign.class)
@Profile("prod")
public class SalvarColetaFeignConfig {}
