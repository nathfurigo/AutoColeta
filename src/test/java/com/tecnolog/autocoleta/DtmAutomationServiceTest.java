package com.tecnolog.autocoleta;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tecnolog.autocoleta.service.DtmAutomationService;

@SpringBootTest
public class DtmAutomationServiceTest {

    @Autowired
    private DtmAutomationService service;

    @Test
    void deveProcessarUmaDTM() {
        Optional<Long> id = service.processOne();
        System.out.println("Processada DTM: " + id);
    }
}
