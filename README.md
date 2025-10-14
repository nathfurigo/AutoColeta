# 🚚 AutoColeta – Automação de Pedidos de Coleta (DTM)

**AutoColeta** é um microserviço Java 21/Spring Boot responsável por automatizar a geração de **Pedidos de Coleta** a partir de DTMs (Documentos de Transporte de Mercadorias) pendentes, transformando-os em JSON compatível com a API **SalvaColeta**.

---

## 🧭 1. Visão Geral

O AutoColeta conecta o banco DTM ao sistema de coleta, executando automaticamente:

1. Consulta à view `vw_dtm_pedidocoleta_unica` (DTMs prontos para coleta);  
2. Bloqueio da linha (`dtm_automation_lock`) para evitar duplicidade;  
3. Mapeamento da linha SQL → JSON (`DtmToSalvaColetaMapper`);  
4. Envio do JSON para a API **SalvaColeta** via Feign;  
5. Atualização de status (`processing` → `processed` ou erro).

> O objetivo é eliminar o processo manual de disparo de coletas, garantindo controle de concorrência e logs detalhados por DTM.

---

## 🏗️ 2. Arquitetura

```
┌────────────────────────────┐
│ SchedulerService           │  ← Agenda execuções periódicas
└──────────────┬─────────────┘
               │
               ▼
┌────────────────────────────┐
│ DtmAutomationService       │  ← Controla o fluxo geral
│  ├─ DtmRepository           │  ← Lê a view vw_dtm_pedidocoleta_unica
│  ├─ DtmLockRepository       │  ← Garante exclusividade via lock
│  ├─ DtmToSalvaColetaMapper  │  ← Transforma em JSON SalvaColeta
│  └─ SalvarColetaFeignAdapter│  ← Envia à API externa
└────────────────────────────┘
```

Cada DTM processado é logado e bloqueado para não ser reenviado.

---

## 🧩 3. Estrutura de Pacotes

| Pacote | Responsabilidade |
|--------|------------------|
| `dtm` | Entidades e repositórios SQL Server |
| `mapper` | Conversões de DTM → JSON (API coleta) |
| `repository` | Locks, view e SQLs auxiliares |
| `service` | Fluxo principal de automação |
| `scheduler` | Agendamento periódico |
| `feign` | Comunicação HTTP com API externa |
| `model` | Estruturas JSON (SalvaColeta, Dimensões, Monitoramento etc.) |

---

## ⚙️ 4. Tecnologias

- **Java 21**
- **Spring Boot 3.3+**
- **Spring Scheduler / HikariCP / OpenFeign**
- **SQL Server + PostgreSQL (view base)**
- **Jackson** para serialização JSON

---

## 🔄 5. Fluxo de Processamento

1. **SchedulerService** executa o job com base no cron definido.  
2. **DtmRepository** lê as DTMs pendentes da `vw_dtm_pedidocoleta_unica`.  
3. Para cada linha:
   - **DtmLockRepository** cria/atualiza o lock (`processing = true`);
   - **DtmToSalvaColetaMapper** monta o JSON `DtmJson`;
   - **SalvarColetaFeignAdapter** chama a API SalvaColeta;
   - Lock atualizado como `processed = true` ou `error_message` preenchido.
4. Logs detalhados são gravados por DTM (id_dtm).

---

## 🧱 6. Configuração

### `.env` ou variáveis de ambiente

```env
SPRING_PROFILES_ACTIVE=prod
DTM_SQLSERVER_URL=jdbc:sqlserver://host:1433;databaseName=dtm
DTM_SQLSERVER_USERNAME=usuario
DTM_SQLSERVER_PASSWORD=senha
SALVA_COLETA_API_URL=https://api.salvacoleta...
AUTOMATION_INTERVAL_MINUTES=10
LOG_LEVEL=INFO
```

### Propriedades de exemplo (`application-prod.yml`)
```yaml
spring:
  datasource:
    url: ${DTM_SQLSERVER_URL}
    username: ${DTM_SQLSERVER_USERNAME}
    password: ${DTM_SQLSERVER_PASSWORD}
  hikari:
    maximum-pool-size: 10
  main:
    allow-bean-definition-overriding: true
```

---

## 🚀 7. Execução

### Build
```bash
mvn clean package -DskipTests
```

### Run
```bash
java -jar target/autocoleta-1.0.0.jar --spring.profiles.active=prod
```

---

## 🧩 8. View `public.vw_dtm_pedidocoleta_unica`

### Propósito
View usada pelo AutoColeta para listar **DTMs válidos para coleta**, aplicando regras de negócio, agenda fixa e filtros automáticos.

### Regras principais
- Retorna apenas DTMs:
  - `tp_status = true`
  - sem coleta/entrega efetiva
  - sem ocorrência `id_ocorrencia = 2`
  - não processados (`dtm_automation_lock`)
  - com `dt_inicio >= hoje (SP)`
- Calcula `dt_coleta_base` conforme:
  - **EMERGÊNCIA** → hoje  
  - **Agenda Fixa** → próximo dia configurado  
  - **Interior >200 km** → soma +1 dia

### Estrutura JSON (campo `json_pedidocoleta`)
Contém todos os dados prontos para envio à API de coleta:
```json
{
  "TokenHash": null,
  "idPedidoColeta": null,
  "dsRemetente": "RECAP",
  "dsDestinatario": "P-66",
  "dtColeta": "2025-10-13",
  "hrColetaInicio": "08:00",
  "dtEntrega": "2025-10-14",
  "NF": [],
  "Dimensoes": [],
  "Monitoramento": []
}
```

### Permissões
```sql
GRANT SELECT ON TABLE public.vw_dtm_pedidocoleta_unica TO powerbi_user, readaccess;
```

### Índices Recomendados
```sql
CREATE INDEX ix_tbddtmversoes_dtm_versao_desc ON tbddtmversoes (id_dtm, nr_versao DESC);
CREATE INDEX ix_tbddtms_status_datas ON tbddtms (tp_status, dt_coletaefetiva, dt_entregaefetiva);
CREATE UNIQUE INDEX ux_dtm_automation_lock_id_dtm ON dtm_automation_lock (id_dtm);
```

---

## 🪣 9. Logs e Monitoramento

- **INFO** → fluxo normal e início/fim de processamento  
- **WARN** → locks ignorados, DTM já processado  
- **ERROR** → falhas de comunicação ou JSON inválido  

> O status de cada DTM pode ser consultado via `dtm_automation_lock`.

---

## 🧭 10. Troubleshooting

| Problema | Causa Provável | Solução |
|-----------|----------------|----------|
| DTM não aparece na view | Data passada, lock ativo ou ocorrência 2 | Verifique a view e o lock |
| Pedido duplicado | Lock não atualizado corretamente | Confirme commit do lock |
| Falha no envio | API SalvaColeta offline ou payload inválido | Validar JSON gerado |
| Erro de timezone | Execução fora de America/Sao_Paulo | Ajustar timezone do servidor |

---

## 🧠 11. Boas Práticas

- Execute sempre sob **Java 21 LTS**.  
- Mantenha `dtm_automation_lock` limpo — registros antigos processados podem ser arquivados.  
- Use logs por `id_dtm` para rastreabilidade completa.  
- Utilize o profile `dev` com banco local e `prod` para produção.

---

## 🧾 12. Licença

© 2025 — **Desenvolvimento Tecnolog**  
Projeto interno de automação logística – uso restrito.
