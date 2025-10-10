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

view 

CREATE OR REPLACE VIEW public.vw_dtm_pedidocoleta_unica AS
/*
--------------------------------------------------------------------------------
OBJETIVO GERAL DA VIEW
Esta VIEW seleciona DTMs (Documentos de Transporte de Materiais) ATIVAS e
PENDENTES de coleta. Ela consolida dados da versão mais recente, aplica regras
de agendamento fixo, prioridade e cálculos de data/hora (principalmente para
o horário de São Paulo e casos de EMERGÊNCIA), formatando o resultado
para sistemas de agendamento logístico ou dashboards.
--------------------------------------------------------------------------------
*/
WITH prioridade AS (
    -- CTE 1: Define a ordem de prioridade para o ORDER BY final, mapeando o texto do serviço a um número.
    SELECT 'EMERGÊNCIA'::text AS nivel, 1 AS ord
    UNION ALL SELECT 'EXPRESSO'::text, 2
    UNION ALL SELECT 'NORMAL'::text, 3
),
agenda_fixa AS (
    -- CTE 2: Tabela de agendamentos fixos para certas origens (local_ref) e seus dias de coleta (agendamento_dow - ISO DOW: 1=Segunda, 5=Sexta).
    SELECT t.local_ref, t.agendamento, t.agendamento_dow
    FROM ( VALUES
        ('RECAP'::text,'SEGUNDA'::text,1), ('RNEST'::text,'TERÇA'::text,2),
        ('REDUC'::text,'TERÇA'::text,2), ('RPBC'::text,'TERÇA'::text,2),
        ('REFAP'::text,'QUARTA'::text,3), ('REVAP'::text,'QUARTA'::text,3),
        ('ARM-MCE'::text,'QUARTA'::text,3), ('UTE PIRATININGA'::text,'QUARTA'::text,3),
        ('BASE TAQUIPE'::text,'QUARTA'::text,3), ('REPAR'::text,'QUARTA'::text,3),
        ('REGAP'::text,'QUINTA'::text,4), ('REPLAN'::text,'QUINTA'::text,4),
        ('UTG'::text,'QUINTA'::text,4), ('UTE TRÊS LAGOAS'::text,'QUINTA'::text,4),
        ('REFAP'::text,'SEXTA'::text,5), ('ARM-MCE'::text,'SEXTA'::text,5),
        ('LUBNOR'::text,'SEXTA'::text,5)
    ) t(local_ref, agendamento, agendamento_dow)
),
versao_mais_recente AS (
    -- CTE 3: Seleciona a versão mais recente (maior nr_versao) de cada DTM (id_dtm) e extrai informações essenciais.
    SELECT
        v.id_versao, v.id_dtm, v.nr_versao, v.ds_nivel_servico, v.ds_tipo_servico,
        v.dt_inicio, v.dt_fim,
        v.ds_origem_nome, v.ds_origem_cep, v.ds_origem_endereco, v.ds_origem_numero,
        v.ds_origem_complemento, v.ds_origem_bairro, v.ds_origem_cidade, v.ds_origem_uf,
        (((v.ds_json -> 'origem') -> 'contatos') -> 0) ->> 'contato' AS ds_origem_contato_nome, -- Extração do nome do contato do JSON
        COALESCE(NULLIF((((v.ds_json -> 'origem') -> 'contatos') -> 0) ->> 'telefone',''),
                 (((v.ds_json -> 'origem') -> 'contatos') -> 0) ->> 'celular') AS ds_origem_contato_telefone, -- Telefone ou Celular de contato
        v.ds_destino_nome, v.ds_destino_cep, v.ds_destino_endereco, v.ds_destino_numero,
        v.ds_destino_bairro, v.ds_destino_complemento, v.ds_destino_cidade, v.ds_destino_uf,
        v.ds_solicitante, v.ds_analista, v.ds_operador, v.ds_referencia,
        v.vl_total_valor, v.ds_observacoes,
        row_number() OVER (PARTITION BY v.id_dtm ORDER BY v.nr_versao DESC) AS rn -- Identifica a versão mais recente (rn = 1)
    FROM public.tbddtmversoes v
),
base AS (
    -- CTE 4: Junta a DTM principal (tbddtms) com a Versão mais Recente e aplica filtros de exclusão.
    SELECT
        dtm.id_dtm,
        -- Campos da Versão (vmr)
        vmr.id_versao, vmr.nr_versao,
        vmr.ds_nivel_servico, vmr.ds_tipo_servico,
        vmr.dt_inicio AS dt_coleta_prevista,  -- Data/Hora de Início da Versão
        vmr.dt_fim AS dt_entrega_prevista,  -- Data/Hora de Fim da Versão (LINHA CORRIGIDA)

        -- ORIGEM e DESTINO (Campos renomeados para DTM)
        vmr.ds_origem_nome AS dtm_ds_origem_nome,
        vmr.ds_origem_cep AS dtm_cd_origem_cep,
        vmr.ds_origem_endereco AS dtm_ds_origem_endereco,
        vmr.ds_origem_numero AS dtm_ds_origem_numero,
        vmr.ds_origem_bairro AS dtm_ds_origem_bairro,
        vmr.ds_origem_complemento AS dtm_ds_origem_complemento,
        vmr.ds_origem_cidade AS dtm_ds_origem_cidade,
        vmr.ds_origem_uf AS dtm_ds_origem_uf,
        vmr.ds_origem_contato_nome,
        vmr.ds_origem_contato_telefone,
        vmr.ds_destino_nome AS dtm_ds_destino_nome,
        vmr.ds_destino_cep AS dtm_cd_destino_cep,
        vmr.ds_destino_endereco AS dtm_ds_destino_endereco,
        vmr.ds_destino_numero AS dtm_ds_destino_numero,
        vmr.ds_destino_bairro AS dtm_ds_destino_bairro,
        vmr.ds_destino_complemento AS dtm_ds_destino_complemento,
        vmr.ds_destino_cidade AS dtm_ds_destino_cidade,
        vmr.ds_destino_uf AS dtm_ds_destino_uf,

        -- OPERACIONAIS
        vmr.ds_operador, vmr.ds_analista, vmr.ds_solicitante, vmr.ds_referencia,
        vmr.vl_total_valor AS dtm_vl_total_valor,
        vmr.ds_observacoes AS dtm_ds_observacoes,

        -- JSONs agregados
        COALESCE(nf.json_notas_fiscais, '[]'::jsonb) AS json_notas_fiscais_raw,
        COALESCE(dim.json_dimensoes, '[]'::jsonb) AS json_dimensoes_raw,
        dim.ds_natureza_carga, dim.ds_embalagem
    FROM public.tbddtms dtm
    JOIN versao_mais_recente vmr
      ON vmr.id_dtm = dtm.id_dtm AND vmr.rn = 1
    LEFT JOIN public.dtm_automation_lock lck ON lck.id_dtm = dtm.id_dtm

    /* LEFT JOIN LATERAL para agregar Notas Fiscais da Versão em JSON */
    LEFT JOIN LATERAL (
        SELECT jsonb_agg(
                        jsonb_build_object(
                            'Numero', n.nr_nota, 'Serie', n.nr_serie,
                            'Subserie', n.nr_subserie, 'Valor', n.vl_valor
                        )
                        ORDER BY n.nr_nota, n.nr_serie
                    ) AS json_notas_fiscais
        FROM public.tbddtmversaonotasfiscais n
        WHERE n.id_versao = vmr.id_versao
    ) nf ON true

    /* LEFT JOIN LATERAL para agregar Dimensões da Versão em JSON */
    LEFT JOIN LATERAL (
        WITH linhas AS (
            SELECT d.id_dtmversaodimensao, d.qt_quantidade, d.nr_comp, d.nr_larg, d.nr_alt,
                    d.nr_peso_bruto, d.nr_peso_cubado, d.nr_peso_taxado, d.vl_material_valor,
                    d.ds_descricao, d.ds_embalagem
            FROM public.tbddtmversaodimensoes d
            WHERE d.id_versao = vmr.id_versao
            ORDER BY d.id_dtmversaodimensao
        )
        SELECT
            jsonb_agg(
                jsonb_build_object(
                    'Descricao', l1.ds_descricao, 'Embalagem', l1.ds_embalagem, 'Quantidade', l1.qt_quantidade,
                    'Comp', round(l1.nr_comp::numeric, 4), 'Larg', round(l1.nr_larg::numeric, 4), 'Alt', round(l1.nr_alt::numeric, 4),
                    'PesoBruto', l1.nr_peso_bruto, 'PesoCubado', l1.nr_peso_cubado, 'PesoTaxado', l1.nr_peso_taxado, 'ValorMaterial', l1.vl_material_valor
                )
            ) AS json_dimensoes,
            (SELECT l2.ds_descricao FROM linhas l2 LIMIT 1) AS ds_natureza_carga, -- Usa a descrição do primeiro item como Natureza da Carga
            (SELECT l2.ds_embalagem FROM linhas l2 LIMIT 1) AS ds_embalagem          -- Usa a embalagem do primeiro item
        FROM linhas l1
    ) dim ON true

    WHERE dtm.tp_status = true                 -- FILTRO CRÍTICO: Apenas DTMs ativas
      AND dtm.dt_coletaefetiva IS NULL         -- FILTRO CRÍTICO: Coleta ainda não foi realizada
      AND dtm.dt_entregaefetiva IS NULL        -- FILTRO: Entrega não realizada (garante que não foi concluída)
      AND NOT EXISTS (SELECT 1 FROM public.tbddtmocorrencias o2 -- FILTRO: Exclui DTMs que tiveram a 'Ocorrência 2' (geralmente Coleta Gerada ou Cancelamento/Bloqueio)
                      WHERE o2.id_dtm = dtm.id_dtm AND o2.id_ocorrencia = 2)
      AND COALESCE(lck.coleta_gerada, '') = '' -- FILTRO: Não bloqueado por automação (flag 'coleta_gerada')
      AND COALESCE(lck.processing, false) = false -- FILTRO: Não está em processamento de automação
),
loc AS (
    -- CTE 5: Converte datas/horas para o fuso horário local e identifica localidades com regras especiais.
    SELECT
        b.*,
        timezone('America/Sao_Paulo', b.dt_coleta_prevista::timestamptz) AS dt_coleta_local,  -- Converte previsão para fuso de SP
        timezone('America/Sao_Paulo', b.dt_entrega_prevista::timestamptz) AS dt_entrega_local, -- Converte previsão para fuso de SP
        CASE
            WHEN b.dtm_ds_origem_nome ILIKE '%LUBNOR%' THEN TRUE
            WHEN b.dtm_ds_origem_nome ILIKE '%REGAP%'  THEN TRUE
            WHEN b.dtm_ds_origem_nome ILIKE '%REPLAN%' THEN TRUE
            ELSE FALSE
        END AS is_interior_location -- REGRA: Flag para locais que podem exigir postergação de data
    FROM base b
),
agora_sp AS (
    -- CTE 6: Obtém o timestamp atual no fuso horário de SP, usado para o cálculo de EMERGÊNCIA.
    SELECT timezone('America/Sao_Paulo', now()) AS ts_now
),
final_date_calc AS (
    -- CTE 7: Calcula a data de coleta final, aplicando a lógica de EMERGÊNCIA e Agendamento Fixo.
    SELECT
        l.*,
        af.local_ref, af.agendamento, af.agendamento_dow,
        (CASE
            WHEN l.ds_nivel_servico = 'EMERGÊNCIA' THEN (SELECT ts_now FROM agora_sp) -- REGRA: EMERGÊNCIA usa a hora atual (para ser processada imediatamente)
            WHEN af.local_ref IS NOT NULL THEN                                       -- REGRA: Aplica lógica de Agendamento Fixo
                CASE
                    WHEN date_part('isodow', (SELECT ts_now FROM agora_sp)) >= af.agendamento_dow
                        THEN (SELECT ts_now FROM agora_sp) + make_interval(days => 7 - date_part('isodow', (SELECT ts_now FROM agora_sp))::int + af.agendamento_dow) -- Próxima semana
                    ELSE  (SELECT ts_now FROM agora_sp) + make_interval(days => af.agendamento_dow - date_part('isodow', (SELECT ts_now FROM agora_sp))::int)                 -- Esta semana
                END
            ELSE l.dt_coleta_local::timestamptz -- REGRA: Usa a data/hora prevista se não for EMERGÊNCIA ou Agendamento Fixo
        END) AS dt_coleta_bruta
    FROM loc l
    LEFT JOIN LATERAL (
        -- Associa a DTM ao Agendamento Fixo se o nome da origem contiver o 'local_ref'
        SELECT af1.local_ref, af1.agendamento, af1.agendamento_dow
        FROM agenda_fixa af1
        WHERE l.dtm_ds_origem_nome ILIKE ('%' || af1.local_ref || '%') LIMIT 1
    ) af ON true
)
SELECT
    l.id_dtm AS "DTM",
    l.dtm_ds_origem_nome AS "Remetente",
    l.dtm_ds_destino_nome AS "Destinatario",
    COALESCE(l.dtm_ds_origem_nome, l.dtm_ds_destino_nome) AS "Pagador",    -- REGRA: Usa origem como pagador (simplificação)
    l.dtm_ds_origem_nome AS "Local Coleta",

    ((l.dt_coleta_bruta::date) +
        CASE WHEN l.is_interior_location THEN INTERVAL '1 day' ELSE INTERVAL '0 day' END
    )::date AS "Data Coleta", -- REGRA: Postergada em 1 dia se for local de 'interior'

    CASE
        WHEN l.ds_nivel_servico = 'EMERGÊNCIA' THEN
            to_char(
                date_trunc('minute', (SELECT ts_now FROM agora_sp))
                + make_interval(mins => (5 - date_part('minute', (SELECT ts_now FROM agora_sp))::int % 5) % 5), -- REGRA: Arredonda a hora atual para o próximo múltiplo de 5 minutos
                'HH24:MI'
            )
        ELSE to_char(l.dt_coleta_local, 'HH24:MI') -- Hora prevista ou hora fixa agendada
    END AS "Hora Coleta",

    l.ds_nivel_servico AS "Tipo de Coleta",
    l.ds_tipo_servico AS "Modal",
    l.ds_operador AS "Filial Responsável",
    l.ds_analista AS "Agente",
    l.dtm_cd_origem_cep AS "CEP",
    l.dtm_ds_origem_endereco AS "Endereço",
    l.dtm_ds_origem_numero AS "Nº",
    l.dtm_ds_origem_bairro AS "Bairro",
    l.dtm_ds_origem_complemento AS "Complemento",
    (l.dtm_ds_origem_cidade || '/' || l.dtm_ds_origem_uf) AS "Cidade/UF",
    l.ds_solicitante AS "Solicitante",
    l.ds_origem_contato_nome AS "Procurar Por",
    l.ds_origem_contato_telefone AS "Telefone",
    l.id_dtm::text AS "Nº Referencia",
    l.id_dtm::text AS "Pedido Cliente",
    l.dt_entrega_local::date AS "Data Entrega",
    l.ds_natureza_carga AS "Natureza da Carga",
    l.ds_embalagem AS "Embalagem",
    l.dtm_vl_total_valor AS "Valor Total",
    l.json_notas_fiscais_raw::text AS "Notas Fiscais",
    l.json_dimensoes_raw::text AS "Dimensões",
    regexp_replace(l.dtm_ds_observacoes::text, '[[:cntrl:]]', ' ', 'g') AS "Comentários", -- REGRA: Limpeza de caracteres de controle
    COALESCE(p.ord, 9) AS prioridade_ordem,
    l.agendamento AS "Agendamento",
    l.agendamento_dow AS "Agendamento_DiaSemana",

    /* Geração do JSON de Agregação 'json_pedidocoleta' (para integração com sistemas externos) */
    jsonb_build_object(
        'dtColeta',
              to_char(
                  ((l.dt_coleta_bruta::date)
                      + CASE WHEN l.is_interior_location THEN INTERVAL '1 day' ELSE INTERVAL '0 day' END
                  )::timestamptz, 'YYYY-MM-DD'
              ),
        'hrColetaInicio',
             CASE
                 WHEN l.ds_nivel_servico = 'EMERGÊNCIA' THEN
                     to_char(
                         date_trunc('minute', (SELECT ts_now FROM agora_sp))
                         + make_interval(mins => (5 - date_part('minute', (SELECT ts_now FROM agora_sp))::int % 5) % 5),
                         'HH24:MI'
                     )
                 ELSE to_char(l.dt_coleta_local, 'HH24:MI')
             END,
        'hrColetaFim',
             CASE
                 WHEN l.dt_entrega_local IS NOT NULL THEN to_char(l.dt_entrega_local, 'HH24:MI')
                 ELSE NULL
             END,
        'dtEntrega',
             CASE
                 WHEN l.dt_entrega_local IS NOT NULL THEN to_char(l.dt_entrega_local, 'YYYY-MM-DD')
                 ELSE NULL
             END,
        'dsEndereco', l.dtm_ds_origem_endereco,
        'nrEnderecoNR', l.dtm_ds_origem_numero,
        'dsEnderecoBairro', l.dtm_ds_origem_bairro,
        'dsEnderecoComplento', l.dtm_ds_origem_complemento,
        'cdEnderecoCEP', l.dtm_cd_origem_cep,
        'dsSolicitante', l.ds_solicitante,
        'dsProcurarPor', l.ds_origem_contato_nome,
        'nrTelefone', l.ds_origem_contato_telefone,
        'nrReferencia', l.id_dtm::text,
        'nrPedidoCliente', l.id_dtm::text,
        'NF',
             COALESCE((
                 SELECT jsonb_agg(
                     jsonb_build_object(
                         'nr',
                             CASE
                                 WHEN COALESCE(n.value->>'Serie','') <> '' THEN (n.value->>'Numero') || '-' || (n.value->>'Serie')
                                 ELSE (n.value->>'Numero')
                             END,
                         'vl', (n.value->>'Valor')::numeric,
                         'chave', NULL
                     )
                 )
                 FROM jsonb_array_elements(l.json_notas_fiscais_raw) n(value)
             ), '[]'::jsonb),
        'Dimensoes',
             COALESCE((
                 SELECT jsonb_agg(
                     jsonb_build_object(
                         'comp', (d.value->>'Comp')::numeric,
                         'larg', (d.value->>'Larg')::numeric,
                         'alt', (d.value->>'Alt')::numeric,
                         'qt', (d.value->>'Quantidade')::int,
                         'kg', (d.value->>'PesoBruto')::numeric
                     )
                 )
                 FROM jsonb_array_elements(l.json_dimensoes_raw) d(value)
             ), '[]'::jsonb),
        'Monitoramento',
             jsonb_build_array(jsonb_build_object(
                 'nome', l.ds_origem_contato_nome,
                 'telefone', l.ds_origem_contato_telefone,
                 'email', NULL
             ))
    ) AS json_pedidocoleta,

    l.id_dtm, l.dt_coleta_local, l.dt_entrega_local, l.agendamento_dow AS agendamento_diasemana
FROM final_date_calc l
LEFT JOIN prioridade p ON l.ds_nivel_servico = p.nivel
ORDER BY COALESCE(p.ord, 9),                                              -- ORDENAÇÃO 1: Prioridade (Emergência no topo)
          (l.dt_coleta_bruta::date),                                       -- ORDENAÇÃO 2: Data de Coleta Calculada
          to_char(l.dt_coleta_local, 'HH24:MI');                           -- ORDENAÇÃO 3: Hora de Coleta (prevista/agendada)

/*
--------------------------------------------------------------------------------
DEFINIÇÃO DE PERMISSÕES
Define o proprietário e as permissões de acesso para a View.
--------------------------------------------------------------------------------
*/
ALTER TABLE public.vw_dtm_pedidocoleta_unica OWNER TO postgres;
GRANT ALL ON TABLE public.vw_dtm_pedidocoleta_unica TO postgres;
GRANT SELECT ON TABLE public.vw_dtm_pedidocoleta_unica TO powerbi_user;
GRANT SELECT ON TABLE public.vw_dtm_pedidocoleta_unica TO readaccess;

-- View: public.vw_dtm_pedidocoleta

-- DROP VIEW public.vw_dtm_pedidocoleta;

CREATE OR REPLACE VIEW public.vw_dtm_pedidocoleta
 AS
 WITH prioridade AS (
         SELECT 'EMERGÊNCIA'::text AS nivel,
            1 AS ord
        UNION ALL
         SELECT 'EXPRESSO'::text AS text,
            2
        UNION ALL
         SELECT 'NORMAL'::text AS text,
            3
        ), agenda_fixa AS (
         SELECT t.local_ref,
            t.agendamento,
            t.agendamento_dow
           FROM ( VALUES ('RECAP'::text,'SEGUNDA'::text,1), ('RNEST'::text,'TERÇA'::text,2), ('REDUC'::text,'TERÇA'::text,2), ('RPBC'::text,'TERÇA'::text,2), ('REFAP'::text,'QUARTA'::text,3), ('REVAP'::text,'QUARTA'::text,3), ('ARM-MCE'::text,'QUARTA'::text,3), ('UTE PIRATININGA'::text,'QUARTA'::text,3), ('BASE TAQUIPE'::text,'QUARTA'::text,3), ('REPAR'::text,'QUARTA'::text,3), ('REGAP'::text,'QUINTA'::text,4), ('REPLAN'::text,'QUINTA'::text,4), ('UTG'::text,'QUINTA'::text,4), ('UTE TRÊS LAGOAS'::text,'QUINTA'::text,4), ('REFAP'::text,'SEXTA'::text,5), ('ARM-MCE'::text,'SEXTA'::text,5), ('LUBNOR'::text,'SEXTA'::text,5)) t(local_ref, agendamento, agendamento_dow)
        ), versao_mais_recente AS (
         SELECT v.id_versao,
            v.id_dtm,
            v.nr_versao,
            v.ds_nivel_servico,
            v.ds_tipo_servico,
            v.dt_inicio,
            v.dt_fim,
            v.ds_origem_nome,
            v.ds_origem_cep,
            v.ds_origem_endereco,
            v.ds_origem_numero,
            v.ds_origem_complemento,
            v.ds_origem_bairro,
            v.ds_origem_cidade,
            v.ds_origem_uf,
            (((v.ds_json -> 'origem'::text) -> 'contatos'::text) -> 0) ->> 'contato'::text AS ds_origem_contato_nome,
            COALESCE(NULLIF((((v.ds_json -> 'origem'::text) -> 'contatos'::text) -> 0) ->> 'telefone'::text, ''::text), (((v.ds_json -> 'origem'::text) -> 'contatos'::text) -> 0) ->> 'celular'::text) AS ds_origem_contato_telefone,
            v.ds_destino_nome,
            v.ds_destino_cep,
            v.ds_destino_endereco,
            v.ds_destino_numero,
            v.ds_destino_bairro,
            v.ds_destino_complemento,
            v.ds_destino_cidade,
            v.ds_destino_uf,
            v.ds_solicitante,
            v.ds_analista,
            v.ds_operador,
            v.ds_referencia,
            v.vl_total_valor,
            v.ds_observacoes,
            row_number() OVER (PARTITION BY v.id_dtm ORDER BY v.nr_versao DESC) AS rn
           FROM tbddtmversoes v
        ), base AS (
         SELECT dtm.id_dtm,
            vmr.id_versao,
            vmr.nr_versao,
            vmr.ds_nivel_servico,
            vmr.ds_tipo_servico,
            vmr.dt_inicio AS dt_coleta_prevista,
            vmr.dt_fim AS dt_entrega_prevista,
            vmr.ds_origem_nome AS dtm_ds_origem_nome,
            vmr.ds_origem_cep AS dtm_cd_origem_cep,
            vmr.ds_origem_endereco AS dtm_ds_origem_endereco,
            vmr.ds_origem_numero AS dtm_ds_origem_numero,
            vmr.ds_origem_bairro AS dtm_ds_origem_bairro,
            vmr.ds_origem_complemento AS dtm_ds_origem_complemento,
            vmr.ds_origem_cidade AS dtm_ds_origem_cidade,
            vmr.ds_origem_uf AS dtm_ds_origem_uf,
            vmr.ds_origem_contato_nome,
            vmr.ds_origem_contato_telefone,
            vmr.ds_destino_nome AS dtm_ds_destino_nome,
            vmr.ds_destino_cep AS dtm_cd_destino_cep,
            vmr.ds_destino_endereco AS dtm_ds_destino_endereco,
            vmr.ds_destino_numero AS dtm_ds_destino_numero,
            vmr.ds_destino_bairro AS dtm_ds_destino_bairro,
            vmr.ds_destino_complemento AS dtm_ds_destino_complemento,
            vmr.ds_destino_cidade AS dtm_ds_destino_cidade,
            vmr.ds_destino_uf AS dtm_ds_destino_uf,
            vmr.ds_operador,
            vmr.ds_analista,
            vmr.ds_solicitante,
            vmr.ds_referencia,
            vmr.vl_total_valor AS dtm_vl_total_valor,
            vmr.ds_observacoes AS dtm_ds_observacoes,
            COALESCE(nf.json_notas_fiscais, '[]'::jsonb) AS json_notas_fiscais_raw,
            COALESCE(dim.json_dimensoes, '[]'::jsonb) AS json_dimensoes_raw,
            dim.ds_natureza_carga,
            dim.ds_embalagem
           FROM tbddtms dtm
             JOIN versao_mais_recente vmr ON vmr.id_dtm = dtm.id_dtm AND vmr.rn = 1
             LEFT JOIN dtm_automation_lock lck ON lck.id_dtm = dtm.id_dtm
             LEFT JOIN LATERAL ( SELECT jsonb_agg(jsonb_build_object('Numero', n.nr_nota, 'Serie', n.nr_serie, 'Subserie', n.nr_subserie, 'Valor', n.vl_valor) ORDER BY n.nr_nota, n.nr_serie) AS json_notas_fiscais
                   FROM tbddtmversaonotasfiscais n
                  WHERE n.id_versao = vmr.id_versao) nf ON true
             LEFT JOIN LATERAL ( WITH linhas AS (
                         SELECT d.id_dtmversaodimensao,
                            d.qt_quantidade,
                            d.nr_comp,
                            d.nr_larg,
                            d.nr_alt,
                            d.nr_peso_bruto,
                            d.nr_peso_cubado,
                            d.nr_peso_taxado,
                            d.vl_material_valor,
                            d.ds_descricao,
                            d.ds_embalagem
                           FROM tbddtmversaodimensoes d
                          WHERE d.id_versao = vmr.id_versao
                          ORDER BY d.id_dtmversaodimensao
                        )
                 SELECT jsonb_agg(jsonb_build_object('Descricao', l1.ds_descricao, 'Embalagem', l1.ds_embalagem, 'Quantidade', l1.qt_quantidade, 'Comp', round(l1.nr_comp::numeric, 4), 'Larg', round(l1.nr_larg::numeric, 4), 'Alt', round(l1.nr_alt::numeric, 4), 'PesoBruto', l1.nr_peso_bruto, 'PesoCubado', l1.nr_peso_cubado, 'PesoTaxado', l1.nr_peso_taxado, 'ValorMaterial', l1.vl_material_valor)) AS json_dimensoes,
                    ( SELECT l2.ds_descricao
                           FROM linhas l2
                         LIMIT 1) AS ds_natureza_carga,
                    ( SELECT l2.ds_embalagem
                           FROM linhas l2
                         LIMIT 1) AS ds_embalagem
                   FROM linhas l1) dim ON true
          WHERE dtm.tp_status = true AND dtm.dt_coletaefetiva IS NULL AND dtm.dt_entregaefetiva IS NULL AND NOT (EXISTS ( SELECT 1
                   FROM tbddtmocorrencias o2
                  WHERE o2.id_dtm = dtm.id_dtm AND o2.id_ocorrencia = 2)) AND COALESCE(lck.processing, false) = false AND COALESCE(lck.processed, false) = false AND timezone('America/Sao_Paulo'::text, vmr.dt_inicio::timestamp with time zone)::date >= timezone('America/Sao_Paulo'::text, now())::date
        ), loc AS (
         SELECT b.id_dtm,
            b.id_versao,
            b.nr_versao,
            b.ds_nivel_servico,
            b.ds_tipo_servico,
            b.dt_coleta_prevista,
            b.dt_entrega_prevista,
            b.dtm_ds_origem_nome,
            b.dtm_cd_origem_cep,
            b.dtm_ds_origem_endereco,
            b.dtm_ds_origem_numero,
            b.dtm_ds_origem_bairro,
            b.dtm_ds_origem_complemento,
            b.dtm_ds_origem_cidade,
            b.dtm_ds_origem_uf,
            b.ds_origem_contato_nome,
            b.ds_origem_contato_telefone,
            b.dtm_ds_destino_nome,
            b.dtm_cd_destino_cep,
            b.dtm_ds_destino_endereco,
            b.dtm_ds_destino_numero,
            b.dtm_ds_destino_bairro,
            b.dtm_ds_destino_complemento,
            b.dtm_ds_destino_cidade,
            b.dtm_ds_destino_uf,
            b.ds_operador,
            b.ds_analista,
            b.ds_solicitante,
            b.ds_referencia,
            b.dtm_vl_total_valor,
            b.dtm_ds_observacoes,
            b.json_notas_fiscais_raw,
            b.json_dimensoes_raw,
            b.ds_natureza_carga,
            b.ds_embalagem,
            timezone('America/Sao_Paulo'::text, b.dt_coleta_prevista::timestamp with time zone) AS dt_coleta_local,
            timezone('America/Sao_Paulo'::text, b.dt_entrega_prevista::timestamp with time zone) AS dt_entrega_local
           FROM base b
        ), agora_sp AS (
         SELECT timezone('America/Sao_Paulo'::text, now()) AS ts_now
        ), coleta_base AS (
         SELECT l.id_dtm,
            l.id_versao,
            l.nr_versao,
            l.ds_nivel_servico,
            l.ds_tipo_servico,
            l.dt_coleta_prevista,
            l.dt_entrega_prevista,
            l.dtm_ds_origem_nome,
            l.dtm_cd_origem_cep,
            l.dtm_ds_origem_endereco,
            l.dtm_ds_origem_numero,
            l.dtm_ds_origem_bairro,
            l.dtm_ds_origem_complemento,
            l.dtm_ds_origem_cidade,
            l.dtm_ds_origem_uf,
            l.ds_origem_contato_nome,
            l.ds_origem_contato_telefone,
            l.dtm_ds_destino_nome,
            l.dtm_cd_destino_cep,
            l.dtm_ds_destino_endereco,
            l.dtm_ds_destino_numero,
            l.dtm_ds_destino_bairro,
            l.dtm_ds_destino_complemento,
            l.dtm_ds_destino_cidade,
            l.dtm_ds_destino_uf,
            l.ds_operador,
            l.ds_analista,
            l.ds_solicitante,
            l.ds_referencia,
            l.dtm_vl_total_valor,
            l.dtm_ds_observacoes,
            l.json_notas_fiscais_raw,
            l.json_dimensoes_raw,
            l.ds_natureza_carga,
            l.ds_embalagem,
            l.dt_coleta_local,
            l.dt_entrega_local,
            af.agendamento,
            af.agendamento_dow,
                CASE
                    WHEN l.ds_nivel_servico::text = 'EMERGÊNCIA'::text THEN ( SELECT agora_sp.ts_now::date AS ts_now
                       FROM agora_sp)
                    WHEN af.local_ref IS NOT NULL THEN ((( SELECT agora_sp.ts_now
                       FROM agora_sp)) + make_interval(days => (af.agendamento_dow + 7 - date_part('isodow'::text, ( SELECT agora_sp.ts_now
                       FROM agora_sp))::integer) % 7 +
                    CASE
                        WHEN date_part('isodow'::text, ( SELECT agora_sp.ts_now
                           FROM agora_sp))::integer = af.agendamento_dow THEN 7
                        ELSE 0
                    END))::date
                    ELSE l.dt_coleta_local::date
                END AS dt_coleta_base
           FROM loc l
             LEFT JOIN LATERAL ( SELECT af1.local_ref,
                    af1.agendamento,
                    af1.agendamento_dow
                   FROM agenda_fixa af1
                  WHERE l.dtm_ds_origem_nome::text ~~* (('%'::text || af1.local_ref) || '%'::text)
                  ORDER BY (length(af1.local_ref)) DESC
                 LIMIT 1) af ON true
        ), interiorizacao AS (
         SELECT cb_1.id_dtm,
            COALESCE(d.interior_gt_200km, false) AS interior_gt_200km
           FROM coleta_base cb_1
             LEFT JOIN tb_origem_aeroporto_dist d ON unaccent(upper(d.origem_nome)) = unaccent(upper(cb_1.dtm_ds_origem_nome::text))
        )
 SELECT cb.id_dtm AS "DTM",
    cb.dtm_ds_origem_nome AS "Remetente",
    cb.dtm_ds_destino_nome AS "Destinatario",
    COALESCE(cb.dtm_ds_origem_nome, cb.dtm_ds_destino_nome) AS "Pagador",
    cb.dtm_ds_origem_nome AS "Local Coleta",
    cb.dt_coleta_local::date AS "Data Coleta",
    to_char(cb.dt_coleta_local, 'HH24:MI'::text) AS "Hora Coleta",
    cb.ds_nivel_servico AS "Tipo de Coleta",
    cb.ds_tipo_servico AS "Modal",
    cb.ds_operador AS "Filial Responsável",
    cb.ds_analista AS "Agente",
    cb.dtm_cd_origem_cep AS "CEP",
    cb.dtm_ds_origem_endereco AS "Endereço",
    cb.dtm_ds_origem_numero AS "Nº",
    cb.dtm_ds_origem_bairro AS "Bairro",
    cb.dtm_ds_origem_complemento AS "Complemento",
    (cb.dtm_ds_origem_cidade::text || '/'::text) || cb.dtm_ds_origem_uf::text AS "Cidade/UF",
    cb.ds_solicitante AS "Solicitante",
    cb.ds_origem_contato_nome AS "Procurar Por",
    cb.ds_origem_contato_telefone AS "Telefone",
    cb.id_dtm::text AS "Nº Referencia",
    cb.id_dtm::text AS "Pedido Cliente",
    cb.dt_entrega_local::date AS "Data Entrega",
    cb.ds_natureza_carga AS "Natureza da Carga",
    cb.ds_embalagem AS "Embalagem",
    cb.dtm_vl_total_valor AS "Valor Total",
    cb.json_notas_fiscais_raw::text AS "Notas Fiscais",
    cb.json_dimensoes_raw::text AS "Dimensões",
    regexp_replace(cb.dtm_ds_observacoes::text, '[[:cntrl:]]'::text, ' '::text, 'g'::text) AS "Comentários",
    COALESCE(p.ord, 9) AS prioridade_ordem,
    cb.agendamento AS "Agendamento",
    cb.agendamento_dow AS "Agendamento_DiaSemana",
    jsonb_build_object('TokenHash', NULL::unknown, 'idPedidoColeta', NULL::unknown, 'idRemetente', NULL::unknown, 'idDestinatario', NULL::unknown, 'idTomador', NULL::unknown, 'idFilialResposavel', NULL::unknown, 'idLocalColeta', NULL::unknown, 'dtColeta', to_char((cb.dt_coleta_base +
        CASE
            WHEN i.interior_gt_200km THEN '1 day'::interval
            ELSE '00:00:00'::interval
        END)::date::timestamp with time zone, 'YYYY-MM-DD'::text), 'hrColetaInicio',
        CASE
            WHEN cb.ds_nivel_servico::text = 'EMERGÊNCIA'::text THEN to_char(date_trunc('minute'::text, ( SELECT agora_sp.ts_now
               FROM agora_sp)) + make_interval(mins => (5 - date_part('minute'::text, ( SELECT agora_sp.ts_now
               FROM agora_sp))::integer % 5) % 5), 'HH24:MI'::text)
            ELSE NULL::text
        END, 'hrColetaFim', NULL::unknown, 'dtEntrega',
        CASE
            WHEN cb.dt_entrega_local IS NOT NULL THEN to_char(cb.dt_entrega_local::date::timestamp with time zone, 'YYYY-MM-DD'::text)
            ELSE NULL::text
        END, 'tpModal', NULL::unknown, 'dsEndereco', cb.dtm_ds_origem_endereco, 'nrEnderecoNR', cb.dtm_ds_origem_numero, 'dsEnderecoBairro', cb.dtm_ds_origem_bairro, 'dsEnderecoComplento', cb.dtm_ds_origem_complemento, 'cdEnderecoCEP', cb.dtm_cd_origem_cep, 'idEnderecoCidade', NULL::unknown, 'dsSolicitante', cb.ds_solicitante, 'dsProcurarPor', cb.ds_origem_contato_nome, 'nrTelefone', cb.ds_origem_contato_telefone, 'idTipoColeta', NULL::unknown, 'idAgente', NULL::unknown, 'idEmbalagem', NULL::unknown, 'idNaturezaCarga', NULL::unknown, 'nrReferencia', cb.id_dtm::text, 'nrPedidoCliente', cb.id_dtm::text, 'NF', COALESCE(( SELECT jsonb_agg(jsonb_build_object('id', NULL::unknown, 'nr', ((n.value ->> 'Numero'::text) || '-'::text) || COALESCE(NULLIF(n.value ->> 'Serie'::text, ''::text), '1'::text), 'vl',
                CASE
                    WHEN (n.value ->> 'Valor'::text) ~ '^\s*\d+(\.\d+)?\s*$'::text THEN (n.value ->> 'Valor'::text)::numeric
                    ELSE NULL::numeric
                END, 'chave', NULL::unknown)) AS jsonb_agg
           FROM jsonb_array_elements(cb.json_notas_fiscais_raw) n(value)), '[]'::jsonb), 'Dimensoes', COALESCE(( SELECT jsonb_agg(jsonb_build_object('id', NULL::unknown, 'comp',
                CASE
                    WHEN (d.value ->> 'Comp'::text) ~ '^\s*\d+(\.\d+)?\s*$'::text THEN (d.value ->> 'Comp'::text)::numeric
                    ELSE NULL::numeric
                END, 'larg',
                CASE
                    WHEN (d.value ->> 'Larg'::text) ~ '^\s*\d+(\.\d+)?\s*$'::text THEN (d.value ->> 'Larg'::text)::numeric
                    ELSE NULL::numeric
                END, 'alt',
                CASE
                    WHEN (d.value ->> 'Alt'::text) ~ '^\s*\d+(\.\d+)?\s*$'::text THEN (d.value ->> 'Alt'::text)::numeric
                    ELSE NULL::numeric
                END, 'qt',
                CASE
                    WHEN (d.value ->> 'Quantidade'::text) ~ '^\s*\d+\s*$'::text THEN (d.value ->> 'Quantidade'::text)::integer
                    ELSE NULL::integer
                END, 'kg',
                CASE
                    WHEN (d.value ->> 'PesoBruto'::text) ~ '^\s*\d+(\.\d+)?\s*$'::text THEN (d.value ->> 'PesoBruto'::text)::numeric
                    ELSE NULL::numeric
                END)) AS jsonb_agg
           FROM jsonb_array_elements(cb.json_dimensoes_raw) d(value)), '[]'::jsonb), 'Monitoramento', jsonb_build_array(jsonb_build_object('nome', cb.ds_origem_contato_nome, 'telefone', cb.ds_origem_contato_telefone, 'email', NULL::unknown)), 'dsComentarios', regexp_replace(cb.dtm_ds_observacoes::text, '[[:cntrl:]]'::text, ' '::text, 'g'::text)) AS json_pedidocoleta,
    cb.id_dtm,
    cb.dt_coleta_local,
    cb.dt_entrega_local,
    cb.agendamento_dow AS agendamento_diasemana
   FROM coleta_base cb
     LEFT JOIN prioridade p ON cb.ds_nivel_servico::text = p.nivel
     LEFT JOIN interiorizacao i ON i.id_dtm = cb.id_dtm
  ORDER BY (COALESCE(p.ord, 9)), (cb.dt_coleta_local::date), (cb.dt_coleta_local::time without time zone);

ALTER TABLE public.vw_dtm_pedidocoleta
    OWNER TO postgres;

GRANT ALL ON TABLE public.vw_dtm_pedidocoleta TO postgres;
GRANT SELECT ON TABLE public.vw_dtm_pedidocoleta TO powerbi_user;
GRANT SELECT ON TABLE public.vw_dtm_pedidocoleta TO readaccess;

