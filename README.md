# Projeto AutoColeta

## 1. Visão Geral

O **AutoColeta** é um serviço de automação desenvolvido em Java com Spring Boot. Sua principal responsabilidade é processar Pedidos de Coleta (DTMs) que estão pendentes em um banco de dados, integrando-os com APIs externas.

O serviço opera em lote, buscando registros periodicamente, enviando-os para a API `SalvaColeta` e, em caso de sucesso, registrando uma ocorrência na API `AddOcorrencia`.

## 2. Tecnologias Utilizadas

- **Java 17+**
- **Spring Boot**: Framework principal da aplicação.
- **Spring Scheduler**: Para execução de tarefas agendadas.
- **Spring JDBC**: Para acesso ao banco de dados PostgreSQL.
- **OpenFeign**: Para a criação de clientes HTTP declarativos.
- **PostgreSQL**: Banco de dados utilizado.
- **Maven / Gradle**: Gerenciador de dependências e build.

## 3. Fluxo da Automação

O processo pode ser iniciado de duas formas:
- **Automaticamente**: Através de um agendamento (`@Scheduled`) que executa em intervalos de tempo configuráveis.
- **Manualmente**: Através de um endpoint REST exposto pelo `AutomationController`.

O fluxo de processamento para cada DTM é o seguinte:
1.  **Busca de Pendentes**: O sistema consulta a `VIEW` `vw_dtm_pedidocoleta_unica` em busca de DTMs pendentes.
2.  **Lock de Registro**: Para cada DTM, um `lock` atômico é aplicado na tabela `dtm_automation_lock` para evitar processamento duplicado.
3.  **Mapeamento de Dados**: O DTM é convertido para o formato esperado pela API de destino (`SalvaColetaModel`).
4.  **Chamada para Salvar Coleta**: Uma requisição `POST` é enviada para a API `SalvaColeta`.
5.  **Registro de Ocorrência**: Se a coleta for salva com sucesso:
    a. O serviço primeiro obtém um token de autorização da API de Autenticação (`/api/v1/Auth`).
    b. Em seguida, envia uma requisição `POST` para a API `AddOcorrencia`, informando o sucesso.
6.  **Atualização de Status**: O registro do DTM na tabela `dtm_automation_lock` é marcado como `PROCESSADO` e o número da coleta gerada é armazenado para auditoria. Em caso de falha, a mensagem de erro é registrada.

## 4. Estrutura do Projeto

O código está organizado nos seguintes pacotes:

- `com.tecnolog.autocoleta`
  - `config`: Classes de configuração do Spring (`AppProperties`, configs de Feign).
  - `domain`: Classes de domínio (entidades).
  - `dtm`: Repositórios e classes de acesso a dados dos DTMs e da tabela de lock.
  - `dto`: (Data Transfer Objects) - Modelos para as respostas das APIs.
  - `salvarcoleta`: Clientes Feign, a interface `SalvarColetaClient` e sua implementação (`Adapter`), além dos DTOs de requisição.
  - `service`: Contém a lógica de negócio principal da aplicação.
  - `web`: Controladores REST para interações manuais (endpoints).
- `resources`: Contém os arquivos de configuração `application.yml` e `application-prod.yml`.

## 5. Configuração

As principais configurações da aplicação estão no arquivo `src/main/resources/application.yml`. Para executar o projeto, você precisará configurar as seguintes seções (geralmente em `application-prod.yml` ou via variáveis de ambiente):

```yaml
# Exemplo de configuração
spring:
  datasource:
    url: "jdbc:postgresql://<host>:<port>/<database>"
    username: "<seu-usuario>"
    password: "<sua-senha>"

app:
  salvarColeta:
    baseUrl: "[https://url.da.api.de.coleta](https://url.da.api.de.coleta)"
    tokenHash: "seu-token-hash-aqui"
  salvarOcorrencia:
    baseUrl: "[https://url.da.api.de.ocorrencia](https://url.da.api.de.ocorrencia)"
    system-token: "seu-system-token-aqui"
```

## 6. Como Executar

### Pré-requisitos
- JDK 17 ou superior.
- Maven ou Gradle instalado.
- Acesso a um banco de dados PostgreSQL.

### Build
Navegue até a raiz do projeto e execute o comando de build:
```bash
# Para Maven
mvn clean install
```

### Execução
Após o build, o artefato `.jar` será gerado no diretório `target/`. Para executar a aplicação ativando o perfil de produção:
```bash
java -jar target/autocoleta-1.0.0.jar --spring.profiles.active=prod
```
A aplicação iniciará e o scheduler começará a executar conforme configurado.