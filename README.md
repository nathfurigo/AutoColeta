# Projeto AutoColeta

## 1. Visão Geral

O **AutoColeta** é um serviço de automação desenvolvido em Java com Spring Boot. Sua principal responsabilidade é processar Pedidos de Coleta (DTMs) que estão pendentes em um banco de dados.

O serviço opera em lote, buscando registros periodicamente, enviando-os para uma API externa (`SalvaColeta`) e, em caso de sucesso, registrando uma ocorrência de sucesso em uma segunda API (`AddOcorrencia`).

## 2. Tecnologias Utilizadas

- **Java 17+**
- **Spring Boot**: Framework principal da aplicação.
- **Spring Scheduler**: Para execução de tarefas agendadas.
- **Spring Data JPA / JDBC**: Para acesso ao banco de dados (inferido).
- **OpenFeign**: Para a criação de clientes HTTP declarativos para as APIs externas.
- **PostgreSQL**: Banco de dados utilizado pela aplicação.
- **Maven / Gradle**: Gerenciador de dependências e build.

## 3. Fluxo da Automação

O processo é iniciado de duas formas:
- **Automaticamente**: Através de um agendamento (`@Scheduled`) que executa a cada intervalo de tempo definido.
- **Manualmente**: Através de um endpoint REST exposto pelo `AutomationController`.

O fluxo de processamento para cada lote de DTMs é o seguinte:
1.  **Busca de Pendentes**: O sistema consulta o banco de dados em busca de DTMs com status pendente.
2.  **Lock de Registro**: Para cada DTM, um `lock` é aplicado para evitar processamento duplicado em ambientes com múltiplas instâncias.
3.  **Mapeamento de Dados**: O DTM é convertido para o formato esperado pela API de destino (`SalvaColetaModel`).
4.  **Chamada para Salvar Coleta**: Uma requisição `POST` é enviada para a API `SalvaColeta`.
5.  **Registro de Ocorrência**: Se a coleta for salva com sucesso:
    a. O serviço primeiro obtém um token de autorização da API de Autenticação.
    b. Em seguida, envia uma requisição `POST` para a API `AddOcorrencia`, informando o sucesso.
6.  **Atualização de Status**: O DTM no banco de dados local é marcado como `PROCESSADO` ou `ERRO`, dependendo do resultado das chamadas.

## 4. Estrutura do Projeto

O código está organizado nos seguintes pacotes:

- `com.tecnolog.autocoleta`
  - `config`: Classes de configuração do Spring, como `AppProperties` e configurações de Feign.
  - `domain`: Classes de domínio (entidades).
  - `dtm`: Repositórios e classes relacionadas ao acesso a dados dos DTMs.
  - `dto`: (Data Transfer Objects) - Modelos de dados para as respostas das APIs.
  - `salvarcoleta`: Contém os clientes Feign, a interface `SalvarColetaClient` e sua implementação (`Adapter`), além dos DTOs de requisição.
  - `service`: Contém a lógica de negócio principal da aplicação.
  - `web`: Controladores REST que expõem endpoints para interação manual.
- `resources`: Contém os arquivos de configuração `application.yml`.

## 5. Configuração

As principais configurações da aplicação estão no arquivo `src/main/resources/application.yml`. Para executar o projeto, você precisará configurar as seguintes seções, especialmente no `application-prod.yml` ou em variáveis de ambiente:

```yaml
spring:
  datasource:
    url: "jdbc:postgresql://<host>:<port>/<database>"
    username: ""
    password: ""

app:
  salvarColeta:
    baseUrl: "https://"
    tokenHash: ""
  salvarOcorrencia:
    baseUrl: "https://"
    system-token: ""
```

## 6. Como Executar

### Pré-requisitos
- JDK 17 ou superior.
- Maven ou Gradle.
- Acesso a um banco de dados PostgreSQL.

### Build
Navegue até a raiz do projeto e execute o comando de build:
```bash
# Para Maven
mvn clean install
```

### Execução
Após o build, o artefato `.jar` será gerado no diretório `target/`. Para executar a aplicação:
```bash
java -jar target/autocoleta-1.0.0.jar --spring.profiles.active=prod
```
A aplicação iniciará e o scheduler começará a executar conforme configurado.