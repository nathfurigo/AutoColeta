package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmMapper;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.pedidos.PedidosApiClient;

import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tecnolog.autocoleta.dtm.DtmJson;

@Service
public class DtmAutomationService {

    private static final Logger LOG = LoggerFactory.getLogger(DtmAutomationService.class);

    private final DtmRepository dtmRepository;
    private final DtmLockRepository lockRepository;
    private final DtmMapper mapper;
    private final PedidosApiClient pedidosApi;

    public DtmAutomationService(DtmRepository dtmRepository, DtmLockRepository lockRepository, DtmMapper mapper,
            PedidosApiClient pedidosApi) {
        this.dtmRepository = dtmRepository;
        this.lockRepository = lockRepository;
        this.mapper = mapper;
        this.pedidosApi = pedidosApi;
    }

    @Transactional
    public Optional<Long> processOne() {
        Optional<DtmPendingRow> opt = dtmRepository.fetchNextPending();
        if (opt.isEmpty()) {
            LOG.info("Nenhuma DTM pendente encontrada.");
            return Optional.empty();
        }

        DtmPendingRow row = opt.get();
        long id = row.getIdDtm();

        try {
            if (!lockRepository.tryLock(id)) {
                LOG.info("DTM {} já está em processamento por outro worker.", id);
                return Optional.empty();
            }

            DtmJson j = mapper.toPedidos(row);
            com.tecnolog.autocoleta.pedidos.payload.DtmJson payload = toPedidosPayload(j);

            Map<String, Object> resp;
            try {
                resp = pedidosApi.inserir(payload);
            } catch (FeignException fe) {
                String body;
                try {
                    body = fe.contentUTF8();
                } catch (Throwable t) {
                    body = fe.getMessage();
                }
                String msg = "Erro HTTP na API Pedidos: status=" + fe.status() + " body=" + body;
                lockRepository.markError(id, msg);
                throw new RuntimeException(msg, fe);
            }

            if (resp == null) {
                lockRepository.markError(id, "Resposta nula da API Pedidos");
                throw new RuntimeException("Resposta nula da API Pedidos");
            }

            if (parseErro(resp)) {
                String msg = parseMensagem(resp);
                lockRepository.markError(id, msg);
                throw new RuntimeException("Falha Pedidos: " + msg);
            }

            lockRepository.markProcessed(id);
            LOG.info("Pedido criado com sucesso para DTM {}. Response: {}", id, resp);
            return Optional.of(id);

        } catch (Exception e) {
            try {
                lockRepository.markError(id, e.getMessage());
            } catch (Exception ignored) {
                LOG.debug("Falha ao marcar erro no lock {}: {}", id, ignored.getMessage());
            }
            LOG.error("Erro ao processar DTM {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public int processBatch(int limit) {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
        int ok = 0;
        for (DtmPendingRow r : pendentes) {
            long id = r.getIdDtm();
            if (!lockRepository.tryLock(id)) {
                continue;
            }
            try {
                DtmJson j = mapper.toPedidos(r);
                com.tecnolog.autocoleta.pedidos.payload.DtmJson payload = toPedidosPayload(j);

                Map<String, Object> resp = pedidosApi.inserir(payload);
                if (resp == null) {
                    lockRepository.markError(id, "Resposta nula da API Pedidos");
                    LOG.warn("DTM {} falhou: resposta nula", id);
                    continue;
                }
                if (parseErro(resp)) {
                    String msg = parseMensagem(resp);
                    lockRepository.markError(id, "API retornou erro: " + msg);
                    LOG.warn("DTM {} falhou: {}", id, msg);
                    continue;
                }
                lockRepository.markProcessed(id);
                ok++;
                LOG.info("DTM {} inserida com sucesso: {}", id, parseMensagem(resp));
            } catch (FeignException e) {
                String body;
                try {
                    body = e.contentUTF8();
                } catch (Throwable t) {
                    body = e.getMessage();
                }
                lockRepository.markError(id, "HTTP " + e.status() + " - " + body);
            } catch (Exception e) {
                lockRepository.markError(id, e.getMessage());
            }
        }
        return ok;
    }

    // ===== Conversão: dtm.DtmJson -> pedidos.payload.DtmJson (com saneamento)
    // =====
    private com.tecnolog.autocoleta.pedidos.payload.DtmJson toPedidosPayload(DtmJson src) {
        if (src == null)
            return null;

        com.tecnolog.autocoleta.pedidos.payload.DtmJson dst = new com.tecnolog.autocoleta.pedidos.payload.DtmJson();

        // Top-level
        dst.setId(src.Id);
        dst.setDtm(src.Dtm);
        dst.setVersao(src.Versao);
        dst.setDtInicio(parseToLocalDateTime(src.DtInicio));
        dst.setDtFim(parseToLocalDateTime(src.DtFim));

        dst.setNivelServico(safeEnum(src.NivelServico));
        dst.setTipoServico(nz(src.TipoServico));
        dst.setTransportadora(nz(src.Transportadora));
        dst.setAnalista(nz(src.Analista));
        dst.setSolicitante(nz(src.Solicitante));

        String ref = src.Referencia;
        if (ref == null || ref.isBlank())
            ref = src.Dtm != null ? String.valueOf(src.Dtm) : "";
        dst.setReferencia(ref);

        dst.setObservacoes(nz(src.Observacoes));
        dst.setValorTotalComImposto(src.ValorTotalComImposto);

        // ---------- ORIGEM ----------
        com.tecnolog.autocoleta.pedidos.payload.DtmJson.Endpoint eo = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Endpoint();
        if (src.Origem != null) {
            eo.Nome = nz(src.Origem.Nome);
            eo.Cep = nz(src.Origem.Cep);
            eo.Endereco = nz(src.Origem.Endereco);
            eo.Numero = nz(src.Origem.Numero);
            eo.Bairro = nz(src.Origem.Bairro);
            eo.Complemento = nz(src.Origem.Complemento);
            eo.Cidade = nz(src.Origem.Cidade);
            eo.UF = nz(src.Origem.UF);

            eo.ContatosOrigem = new ArrayList<>();
            if (src.Origem.ContatosOrigem != null) {
                for (DtmJson.Contato c : src.Origem.ContatosOrigem) {
                    com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato();
                    if (c != null) {
                        cc.Nome = nz(c.Nome);
                        cc.Telefone = nz(c.Telefone);
                        cc.Email = nz(c.Email);
                    }
                    eo.ContatosOrigem.add(cc);
                }
            }

            eo.ContatosDestino = new ArrayList<>();
            if (src.Origem.ContatosDestino != null) {
                for (DtmJson.Contato c : src.Origem.ContatosDestino) {
                    com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato();
                    if (c != null) {
                        cc.Nome = nz(c.Nome);
                        cc.Telefone = nz(c.Telefone);
                        cc.Email = nz(c.Email);
                    }
                    eo.ContatosDestino.add(cc);
                }
            }
        } else {
            eo.Nome = eo.Cidade = eo.UF = eo.Cep = eo.Endereco = eo.Numero = eo.Bairro = eo.Complemento = "";
            eo.ContatosOrigem = new ArrayList<>();
            eo.ContatosDestino = new ArrayList<>();
        }
        dst.setOrigem(eo);

        // ---------- DESTINO ----------
        com.tecnolog.autocoleta.pedidos.payload.DtmJson.Endpoint ed = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Endpoint();
        if (src.Destino != null) {
            ed.Nome = nz(src.Destino.Nome);
            ed.Cep = nz(src.Destino.Cep);
            ed.Endereco = nz(src.Destino.Endereco);
            ed.Numero = nz(src.Destino.Numero);
            ed.Bairro = nz(src.Destino.Bairro);
            ed.Complemento = nz(src.Destino.Complemento);
            ed.Cidade = nz(src.Destino.Cidade);
            ed.UF = nz(src.Destino.UF);

            ed.ContatosOrigem = new ArrayList<>();
            if (src.Destino.ContatosOrigem != null) {
                for (DtmJson.Contato c : src.Destino.ContatosOrigem) {
                    com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato();
                    if (c != null) {
                        cc.Nome = nz(c.Nome);
                        cc.Telefone = nz(c.Telefone);
                        cc.Email = nz(c.Email);
                    }
                    ed.ContatosOrigem.add(cc);
                }
            }

            ed.ContatosDestino = new ArrayList<>();
            if (src.Destino.ContatosDestino != null) {
                for (DtmJson.Contato c : src.Destino.ContatosDestino) {
                    com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Contato();
                    if (c != null) {
                        cc.Nome = nz(c.Nome);
                        cc.Telefone = nz(c.Telefone);
                        cc.Email = nz(c.Email);
                    }
                    ed.ContatosDestino.add(cc);
                }
            }
        } else {
            ed.Nome = ed.Cidade = ed.UF = ed.Cep = ed.Endereco = ed.Numero = ed.Bairro = ed.Complemento = "";
            ed.ContatosOrigem = new ArrayList<>();
            ed.ContatosDestino = new ArrayList<>();
        }
        dst.setDestino(ed);

        // ---------- NOTAS ----------
        List<com.tecnolog.autocoleta.pedidos.payload.DtmJson.Nota> notas = new ArrayList<>();
        if (src.NotasFiscaisRelacionadas != null) {
            for (DtmJson.Nota n : src.NotasFiscaisRelacionadas) {
                com.tecnolog.autocoleta.pedidos.payload.DtmJson.Nota nn = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Nota();
                if (n != null) {
                    nn.Numero = n.Numero;
                    nn.Serie = n.Serie;
                    nn.Subserie = n.Subserie;
                    nn.Valor = n.Valor;
                }
                notas.add(nn);
            }
        }
        dst.setNotasFiscaisRelacionadas(notas);

        // ---------- CARGAS ----------
        List<com.tecnolog.autocoleta.pedidos.payload.DtmJson.Carga> cargas = new ArrayList<>();
        if (src.Cargas != null && !src.Cargas.isEmpty()) {
            for (DtmJson.Carga c : src.Cargas) {
                com.tecnolog.autocoleta.pedidos.payload.DtmJson.Carga cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Carga();
                if (c != null) {
                    cc.Descricao = nz(c.Descricao);
                    cc.Embalagem = nz(c.Embalagem);
                    cc.Quantidade = c.Quantidade != null ? c.Quantidade : 1;
                    cc.Comp = c.Comp;
                    cc.Larg = c.Larg;
                    cc.Alt = c.Alt;
                    cc.PesoBruto = c.PesoBruto;
                    cc.PesoCubado = c.PesoCubado;
                    cc.PesoTaxado = c.PesoTaxado;
                    cc.ValorMaterial = c.ValorMaterial != null ? c.ValorMaterial : 0d;
                }
                cargas.add(cc);
            }
        } else {
            com.tecnolog.autocoleta.pedidos.payload.DtmJson.Carga cc = new com.tecnolog.autocoleta.pedidos.payload.DtmJson.Carga();
            cc.Descricao = "";
            cc.Embalagem = "CAIXA";
            cc.Quantidade = 1;
            cc.ValorMaterial = src.ValorTotalComImposto != null ? src.ValorTotalComImposto : 0d;
            cargas.add(cc);
        }
        dst.setCargas(cargas);

        return dst;
    }

    private static String nz(String s) {
        return (s == null) ? "" : s;
    }

    private static String safeEnum(String s) {
        if (s == null)
            return "";
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents.trim().toUpperCase();
    }

    // Aceita: ISO com offset (…-03:00), ISO com timezone (…[America/Sao_Paulo]) e
    // ISO puro
    private static LocalDateTime parseToLocalDateTime(String s) {
        if (s == null || s.isBlank())
            return null;

        // ISO com offset, ex.: 2025-10-04T22:40:27.156-03:00
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignore) {
        }

        // ISO com timezone nomeado, ex.: 2025-10-04T22:40:27-03:00[America/Sao_Paulo]
        try {
            return ZonedDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignore) {
        }

        // ISO puro sem offset, ex.: 2025-10-04T22:40:27 ou 2025-10-04T22:40:27.156
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
        }

        // Padrões comuns:
        DateTimeFormatter[] patterns = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"), // millis + offset
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"), // sem millis + offset
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"), // millis, sem offset
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") // sem millis, sem offset
        };

        for (DateTimeFormatter f : patterns) {
            try {
                // quando o pattern tem XXX, devemos usar OffsetDateTime.parse
                String p = f.toString();
                boolean hasOffset = p.contains("XXX");
                if (hasOffset) {
                    return OffsetDateTime.parse(s, f).toLocalDateTime();
                } else {
                    return LocalDateTime.parse(s, f);
                }
            } catch (Exception ignore) {
            }
        }

        // Falhou tudo
        return null;
    }

    private boolean parseErro(Map<String, Object> resp) {
        if (resp == null)
            return true;
        Object raw = resp.get("erro");
        if (raw == null)
            raw = resp.get("error");
        if (raw == null)
            return false;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty())
            return false;
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private String parseMensagem(Map<String, Object> resp) {
        if (resp == null)
            return "Resposta nula da API";
        Object msg = resp.get("mensagem");
        if (msg == null)
            msg = resp.get("message");
        if (msg == null)
            msg = resp.get("response");
        return (msg != null) ? String.valueOf(msg) : String.valueOf(resp);
    }

    private static void fillEndpointDefaults(com.tecnolog.autocoleta.pedidos.payload.DtmJson.Endpoint e,
            boolean isDestino) {
        if (e.Cep == null || e.Cep.isBlank())
            e.Cep = "00000000";
        if (e.Endereco == null || e.Endereco.isBlank())
            e.Endereco = ".";
        if (e.Numero == null || e.Numero.isBlank())
            e.Numero = "S/N";
        if (e.Bairro == null || e.Bairro.isBlank())
            e.Bairro = ".";
        if (e.Cidade == null || e.Cidade.isBlank())
            e.Cidade = ".";
        if (e.UF == null || e.UF.isBlank())
            e.UF = "RJ";
        if (e.Nome == null || e.Nome.isBlank())
            e.Nome = isDestino ? "DESTINO" : "ORIGEM";
    }

    private static String sanitizeObs(String s, int max) {
        if (s == null)
            return "";
        String cleaned = s.replace("\r", " ").replace("\n", " ").replaceAll("\\s{2,}", " ").trim();
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }

    private static <T> List<T> nullIfEmpty(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}
