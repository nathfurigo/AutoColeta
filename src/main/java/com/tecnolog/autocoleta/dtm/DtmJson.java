package com.tecnolog.autocoleta.dtm;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DtmJson {
    public String Id;
    public Long Dtm;
    public Integer Versao;
    public String DtInicio;
    public String DtFim;

    public String NivelServico;
    public String TipoServico;
    public String Transportadora;
    public String Analista;
    public String Solicitante;
    public String Referencia;
    public String Observacoes;
    public Double ValorTotalComImposto;

    public Endpoint Origem;
    public Endpoint Destino;

    public List<Nota> NotasFiscaisRelacionadas;
    public List<Carga> Cargas;

    public static class Endpoint {
        public String Nome;
        public String Cep;
        public String Endereco;
        public String Numero;
        public String Bairro;
        public String Complemento;
        public String Cidade;
        public String UF;
        public List<Contato> ContatosOrigem;
        public List<Contato> ContatosDestino;
        public String getNome() { return Nome; }
        public void setNome(String nome) { Nome = nome; }
        public String getCep() { return Cep; }
        public void setCep(String cep) { Cep = cep; }
        public String getEndereco() { return Endereco; }
        public void setEndereco(String endereco) { Endereco = endereco; }
        public String getNumero() { return Numero; }
        public void setNumero(String numero) { Numero = numero; }
        public String getBairro() { return Bairro; }
        public void setBairro(String bairro) { Bairro = bairro; }
        public String getComplemento() { return Complemento; }
        public void setComplemento(String complemento) { Complemento = complemento; }
        public String getCidade() { return Cidade; }
        public void setCidade(String cidade) { Cidade = cidade; }
        public String getUF() { return UF; }
        public void setUF(String uF) { UF = uF; }
        public List<Contato> getContatosOrigem() { return ContatosOrigem; }
        public void setContatosOrigem(List<Contato> contatosOrigem) { ContatosOrigem = contatosOrigem; }
        public List<Contato> getContatosDestino() { return ContatosDestino; }
        public void setContatosDestino(List<Contato> contatosDestino) { ContatosDestino = contatosDestino; }
    }

    public static class Contato {
        public String Nome;
        public String Telefone;
        public String Email;
        public String getNome() { return Nome; }
        public void setNome(String nome) { Nome = nome; }
        public String getTelefone() { return Telefone; }
        public void setTelefone(String telefone) { Telefone = telefone; }
        public String getEmail() { return Email; }
        public void setEmail(String email) { Email = email; }
    }

    public static class Nota {
        public Integer Numero;
        public Integer Serie;
        public Integer Subserie;
        public Double Valor;
        public Integer getNumero() { return Numero; }
        public void setNumero(Integer numero) { Numero = numero; }
        public Integer getSerie() { return Serie; }
        public void setSerie(Integer serie) { Serie = serie; }
        public Integer getSubserie() { return Subserie; }
        public void setSubserie(Integer subserie) { Subserie = subserie; }
        public Double getValor() { return Valor; }
        public void setValor(Double valor) { Valor = valor; }
    }

    public static class Carga {
        public String Descricao;
        public String Embalagem;
        public Integer Quantidade;
        public Double Comp;
        public Double Larg;
        public Double Alt;
        public Double PesoBruto;
        public Double PesoCubado;
        public Double PesoTaxado;
        public Double ValorMaterial;

        public String getDescricao() { return Descricao; }
        public void setDescricao(String descricao) { Descricao = descricao; }
        public String getEmbalagem() { return Embalagem; }
        public void setEmbalagem(String embalagem) { Embalagem = embalagem; }
        public Integer getQuantidade() { return Quantidade; }
        public void setQuantidade(Integer quantidade) { Quantidade = quantidade; }
        public Double getComp() { return Comp; }
        public void setComp(Double comp) { Comp = comp; }
        public Double getLarg() { return Larg; }
        public void setLarg(Double larg) { Larg = larg; }
        public Double getAlt() { return Alt; }
        public void setAlt(Double alt) { Alt = alt; }
        public Double getPesoBruto() { return PesoBruto; }
        public void setPesoBruto(Double pesoBruto) { PesoBruto = pesoBruto; }
        public Double getPesoCubado() { return PesoCubado; }
        public void setPesoCubado(Double pesoCubado) { PesoCubado = pesoCubado; }
        public Double getPesoTaxado() { return PesoTaxado; }
        public void setPesoTaxado(Double pesoTaxado) { PesoTaxado = pesoTaxado; }
        public Double getValorMaterial() { return ValorMaterial; }
        public void setValorMaterial(Double valorMaterial) { ValorMaterial = valorMaterial; }
    }

    public String getId() { return Id; }
    public void setId(String id) { Id = id; }
    public Long getDtm() { return Dtm; }
    public void setDtm(Long dtm) { Dtm = dtm; }
    public Integer getVersao() { return Versao; }
    public void setVersao(Integer versao) { Versao = versao; }
    public String getDtInicio() { return DtInicio; }
    public void setDtInicio(String dtInicio) { DtInicio = dtInicio; }
    public String getDtFim() { return DtFim; }
    public void setDtFim(String dtFim) { DtFim = dtFim; }
    public String getNivelServico() { return NivelServico; }
    public void setNivelServico(String nivelServico) { NivelServico = nivelServico; }
    public String getTipoServico() { return TipoServico; }
    public void setTipoServico(String tipoServico) { TipoServico = tipoServico; }
    public String getTransportadora() { return Transportadora; }
    public void setTransportadora(String transportadora) { Transportadora = transportadora; }
    public String getAnalista() { return Analista; }
    public void setAnalista(String analista) { Analista = analista; }
    public String getSolicitante() { return Solicitante; }
    public void setSolicitante(String solicitante) { Solicitante = solicitante; }
    public String getReferencia() { return Referencia; }
    public void setReferencia(String referencia) { Referencia = referencia; }
    public String getObservacoes() { return Observacoes; }
    public void setObservacoes(String observacoes) { Observacoes = observacoes; }
    public Double getValorTotalComImposto() { return ValorTotalComImposto; }
    public void setValorTotalComImposto(Double valorTotalComImposto) { ValorTotalComImposto = valorTotalComImposto; }
    public Endpoint getOrigem() { return Origem; }
    public void setOrigem(Endpoint origem) { Origem = origem; }
    public Endpoint getDestino() { return Destino; }
    public void setDestino(Endpoint destino) { Destino = destino; }
    public List<Nota> getNotasFiscaisRelacionadas() { return NotasFiscaisRelacionadas; }
    public void setNotasFiscaisRelacionadas(List<Nota> notasFiscaisRelacionadas) { NotasFiscaisRelacionadas = notasFiscaisRelacionadas; }
    public List<Carga> getCargas() { return Cargas; }
    public void setCargas(List<Carga> cargas) { Cargas = cargas; }
}
