package com.example.instrumentaliza.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/**
 * FirebaseSolicitacao - Modelo de dados para solicitações de reserva no Firebase Firestore
 * 
 * Esta classe representa uma solicitação de reserva de instrumento no Firebase Firestore,
 * contendo todas as informações necessárias para o processo de solicitação, aprovação
 * e recusa de reservas. É usada para serialização/deserialização de dados do Firestore.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos da solicitação
 * - Relacionamento com usuário solicitante, instrumento e proprietário
 * - Controle de status da solicitação (PENDENTE, ACEITA, RECUSADA)
 * - Gerenciamento de períodos solicitados
 * - Timestamps de criação e atualização
 * 
 * Características técnicas:
 * - Modelo POJO para Firebase Firestore
 * - Conversão automática de tipos do Firestore
 * - Suporte a timestamps Firebase/Java
 * - Construtor padrão para deserialização
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FirebaseSolicitacao {
    
    // Dados básicos da solicitação
    private String id;
    private String solicitanteId;        // ID do usuário que está solicitando
    private String proprietarioId;       // ID do proprietário do instrumento
    private String instrumentoId;        // ID do instrumento solicitado
    private String instrumentoNome;      // Nome do instrumento (para facilitar consultas)
    private String solicitanteNome;      // Nome do solicitante (para facilitar consultas)
    private String solicitanteEmail;     // Email do solicitante
    private String solicitanteTelefone;  // Telefone do solicitante
    
    // Período solicitado
    private Date dataInicio;             // Data de início solicitada
    private Date dataFim;               // Data de fim solicitada
    private double precoTotal;          // Preço total calculado
    
    // Status e controle
    private String status;              // PENDENTE, ACEITA, RECUSADA
    private String motivoRecusa;        // Motivo da recusa (se aplicável)
    private Date dataCriacao;           // Data de criação da solicitação
    private Date dataAtualizacao;       // Data da última atualização
    private String observacoes;         // Observações adicionais do solicitante

    /**
     * Construtor vazio necessário para o Firestore
     * 
     * O Firebase Firestore requer um construtor sem parâmetros
     * para deserialização automática dos dados.
     */
    public FirebaseSolicitacao() {
        // Construtor vazio necessário para o Firestore
    }

    /**
     * Construtor completo da solicitação
     * 
     * @param solicitanteId ID do usuário que está solicitando
     * @param proprietarioId ID do proprietário do instrumento
     * @param instrumentoId ID do instrumento solicitado
     * @param instrumentoNome Nome do instrumento
     * @param solicitanteNome Nome do solicitante
     * @param solicitanteEmail Email do solicitante
     * @param solicitanteTelefone Telefone do solicitante
     * @param dataInicio Data de início solicitada
     * @param dataFim Data de fim solicitada
     * @param precoTotal Preço total calculado
     * @param observacoes Observações adicionais
     */
    public FirebaseSolicitacao(String solicitanteId, String proprietarioId, String instrumentoId,
                              String instrumentoNome, String solicitanteNome, String solicitanteEmail,
                              String solicitanteTelefone, Date dataInicio, Date dataFim,
                              double precoTotal, String observacoes) {
        this.solicitanteId = solicitanteId;
        this.proprietarioId = proprietarioId;
        this.instrumentoId = instrumentoId;
        this.instrumentoNome = instrumentoNome;
        this.solicitanteNome = solicitanteNome;
        this.solicitanteEmail = solicitanteEmail;
        this.solicitanteTelefone = solicitanteTelefone;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.precoTotal = precoTotal;
        this.observacoes = observacoes;
        this.status = "PENDENTE";
        this.dataCriacao = new Date();
        this.dataAtualizacao = new Date();
        this.motivoRecusa = null;
    }

    /**
     * Cria um FirebaseSolicitacao a partir de um DocumentSnapshot do Firestore
     * 
     * Este método converte um documento do Firestore em um objeto FirebaseSolicitacao,
     * tratando conversões de tipos e valores padrão para campos opcionais.
     * 
     * @param document DocumentSnapshot do Firestore
     * @return FirebaseSolicitacao populado com os dados do documento
     */
    public static FirebaseSolicitacao fromDocument(DocumentSnapshot document) {
        FirebaseSolicitacao solicitacao = new FirebaseSolicitacao();
        
        solicitacao.setId(document.getId());
        solicitacao.setSolicitanteId((String) document.get("solicitanteId"));
        solicitacao.setProprietarioId((String) document.get("proprietarioId"));
        solicitacao.setInstrumentoId((String) document.get("instrumentoId"));
        solicitacao.setInstrumentoNome((String) document.get("instrumentoNome"));
        solicitacao.setSolicitanteNome((String) document.get("solicitanteNome"));
        solicitacao.setSolicitanteEmail((String) document.get("solicitanteEmail"));
        solicitacao.setSolicitanteTelefone((String) document.get("solicitanteTelefone"));
        
        // Converter timestamps para Date
        Timestamp timestampInicio = (Timestamp) document.get("dataInicio");
        if (timestampInicio != null) {
            solicitacao.setDataInicio(timestampInicio.toDate());
        }
        
        Timestamp timestampFim = (Timestamp) document.get("dataFim");
        if (timestampFim != null) {
            solicitacao.setDataFim(timestampFim.toDate());
        }
        
        solicitacao.setPrecoTotal(((Number) document.get("precoTotal")).doubleValue());
        
        // Garantir que o status sempre tenha um valor válido
        String statusFromFirebase = (String) document.get("status");
        if (statusFromFirebase == null || statusFromFirebase.isEmpty()) {
            statusFromFirebase = "PENDENTE";
        }
        solicitacao.setStatus(statusFromFirebase);
        
        solicitacao.setMotivoRecusa((String) document.get("motivoRecusa"));
        solicitacao.setObservacoes((String) document.get("observacoes"));
        
        // Converter timestamps de controle
        Timestamp timestampCriacao = (Timestamp) document.get("dataCriacao");
        if (timestampCriacao != null) {
            solicitacao.setDataCriacao(timestampCriacao.toDate());
        }
        
        Timestamp timestampAtualizacao = (Timestamp) document.get("dataAtualizacao");
        if (timestampAtualizacao != null) {
            solicitacao.setDataAtualizacao(timestampAtualizacao.toDate());
        }
        
        return solicitacao;
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único da solicitação
     * @return ID da solicitação
     */
    public String getId() {
        return id;
    }

    /**
     * Define o ID único da solicitação
     * @param id ID da solicitação
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtém o ID do usuário solicitante
     * @return ID do solicitante
     */
    public String getSolicitanteId() {
        return solicitanteId;
    }

    /**
     * Define o ID do usuário solicitante
     * @param solicitanteId ID do solicitante
     */
    public void setSolicitanteId(String solicitanteId) {
        this.solicitanteId = solicitanteId;
    }

    /**
     * Obtém o ID do proprietário do instrumento
     * @return ID do proprietário
     */
    public String getProprietarioId() {
        return proprietarioId;
    }

    /**
     * Define o ID do proprietário do instrumento
     * @param proprietarioId ID do proprietário
     */
    public void setProprietarioId(String proprietarioId) {
        this.proprietarioId = proprietarioId;
    }

    /**
     * Obtém o ID do instrumento solicitado
     * @return ID do instrumento
     */
    public String getInstrumentoId() {
        return instrumentoId;
    }

    /**
     * Define o ID do instrumento solicitado
     * @param instrumentoId ID do instrumento
     */
    public void setInstrumentoId(String instrumentoId) {
        this.instrumentoId = instrumentoId;
    }

    /**
     * Obtém o nome do instrumento
     * @return Nome do instrumento
     */
    public String getInstrumentoNome() {
        return instrumentoNome;
    }

    /**
     * Define o nome do instrumento
     * @param instrumentoNome Nome do instrumento
     */
    public void setInstrumentoNome(String instrumentoNome) {
        this.instrumentoNome = instrumentoNome;
    }

    /**
     * Obtém o nome do solicitante
     * @return Nome do solicitante
     */
    public String getSolicitanteNome() {
        return solicitanteNome;
    }

    /**
     * Define o nome do solicitante
     * @param solicitanteNome Nome do solicitante
     */
    public void setSolicitanteNome(String solicitanteNome) {
        this.solicitanteNome = solicitanteNome;
    }

    /**
     * Obtém o email do solicitante
     * @return Email do solicitante
     */
    public String getSolicitanteEmail() {
        return solicitanteEmail;
    }

    /**
     * Define o email do solicitante
     * @param solicitanteEmail Email do solicitante
     */
    public void setSolicitanteEmail(String solicitanteEmail) {
        this.solicitanteEmail = solicitanteEmail;
    }

    /**
     * Obtém o telefone do solicitante
     * @return Telefone do solicitante
     */
    public String getSolicitanteTelefone() {
        return solicitanteTelefone;
    }

    /**
     * Define o telefone do solicitante
     * @param solicitanteTelefone Telefone do solicitante
     */
    public void setSolicitanteTelefone(String solicitanteTelefone) {
        this.solicitanteTelefone = solicitanteTelefone;
    }

    /**
     * Obtém a data de início solicitada
     * @return Data de início
     */
    public Date getDataInicio() {
        return dataInicio;
    }

    /**
     * Define a data de início solicitada
     * @param dataInicio Data de início
     */
    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    /**
     * Obtém a data de fim solicitada
     * @return Data de fim
     */
    public Date getDataFim() {
        return dataFim;
    }

    /**
     * Define a data de fim solicitada
     * @param dataFim Data de fim
     */
    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }

    /**
     * Obtém o preço total calculado
     * @return Preço total
     */
    public double getPrecoTotal() {
        return precoTotal;
    }

    /**
     * Define o preço total calculado
     * @param precoTotal Preço total
     */
    public void setPrecoTotal(double precoTotal) {
        this.precoTotal = precoTotal;
    }

    /**
     * Obtém o status atual da solicitação
     * @return Status da solicitação
     */
    public String getStatus() {
        if (status == null || status.isEmpty()) {
            return "PENDENTE";
        }
        return status;
    }

    /**
     * Define o status da solicitação
     * @param status Status da solicitação
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Obtém o motivo da recusa
     * @return Motivo da recusa
     */
    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    /**
     * Define o motivo da recusa
     * @param motivoRecusa Motivo da recusa
     */
    public void setMotivoRecusa(String motivoRecusa) {
        this.motivoRecusa = motivoRecusa;
    }

    /**
     * Obtém a data de criação da solicitação
     * @return Data de criação
     */
    public Date getDataCriacao() {
        return dataCriacao;
    }

    /**
     * Define a data de criação da solicitação
     * @param dataCriacao Data de criação
     */
    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    /**
     * Obtém a data da última atualização
     * @return Data de atualização
     */
    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    /**
     * Define a data da última atualização
     * @param dataAtualizacao Data de atualização
     */
    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    /**
     * Obtém as observações adicionais
     * @return Observações
     */
    public String getObservacoes() {
        return observacoes;
    }

    /**
     * Define as observações adicionais
     * @param observacoes Observações
     */
    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    /**
     * Verifica se a solicitação está pendente
     * @return true se pendente, false caso contrário
     */
    public boolean isPendente() {
        return "PENDENTE".equals(status);
    }

    /**
     * Verifica se a solicitação foi aceita
     * @return true se aceita, false caso contrário
     */
    public boolean isAceita() {
        return "ACEITA".equals(status);
    }

    /**
     * Verifica se a solicitação foi recusada
     * @return true se recusada, false caso contrário
     */
    public boolean isRecusada() {
        return "RECUSADA".equals(status);
    }

    @Override
    public String toString() {
        return "FirebaseSolicitacao{" +
                "id='" + id + '\'' +
                ", solicitanteId='" + solicitanteId + '\'' +
                ", proprietarioId='" + proprietarioId + '\'' +
                ", instrumentoId='" + instrumentoId + '\'' +
                ", instrumentoNome='" + instrumentoNome + '\'' +
                ", solicitanteNome='" + solicitanteNome + '\'' +
                ", solicitanteEmail='" + solicitanteEmail + '\'' +
                ", solicitanteTelefone='" + solicitanteTelefone + '\'' +
                ", dataInicio=" + dataInicio +
                ", dataFim=" + dataFim +
                ", precoTotal=" + precoTotal +
                ", status='" + status + '\'' +
                ", observacoes='" + observacoes + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                ", motivoRecusa='" + motivoRecusa + '\'' +
                '}';
    }
}
