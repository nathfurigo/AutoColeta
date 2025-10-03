package com.tecnolog.autocoleta;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AutocoletaApplicationTests {

	@MockBean
    private com.tecnolog.autocoleta.pedidos.PedidosApiClient pedidosApiClient;

    @Test
    void contextLoads() { }

}
