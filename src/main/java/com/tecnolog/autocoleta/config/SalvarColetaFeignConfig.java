package com.tecnolog.autocoleta.config;

import feign.RequestInterceptor;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Primary
public class SalvarColetaFeignConfig {

    // Força Accept JSON (e fallback)
    @Bean
    public RequestInterceptor acceptHeader() {
        return t -> t.header("Accept", "application/json, */*;q=0.8");
    }

    // Permite desserializar JSON mesmo quando o servidor manda text/html ou text/plain
    @Bean
    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringDecoder(() -> {
            HttpMessageConverters base = messageConverters.getObject();
            List<HttpMessageConverter<?>> convs = new ArrayList<>(base.getConverters());
            for (HttpMessageConverter<?> c : convs) {
                if (c instanceof MappingJackson2HttpMessageConverter jackson) {
                    List<MediaType> mts = new ArrayList<>(jackson.getSupportedMediaTypes());
                    mts.add(MediaType.TEXT_HTML);
                    mts.add(MediaType.TEXT_PLAIN);
                    jackson.setSupportedMediaTypes(mts);
                }
            }
            return new HttpMessageConverters(false, convs);
        });
    }
}
