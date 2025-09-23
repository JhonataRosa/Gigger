package com.example.instrumentaliza;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

import java.util.Date;

/**
 * Reserva - Modelo de dados para reservas de instrumentos
 * 
 * Esta classe representa uma reserva de instrumento no sistema, contendo
 * todas as informações necessárias para gerenciar o processo de aluguel.
 * É uma entidade Room Database com relacionamentos para Usuario e Instrumento.
 * 
 * Funcionalidades:
 * - Gerenciamento de períodos de aluguel
 * - Controle de status da reserva
 * - Cálculo de preços totais
 * - Relacionamentos com usuário e instrumento
 * - Auditoria com data de criação
 * 
 * Características técnicas:
 * - Entidade Room Database com chave primária auto-incremento
 * - Foreign Keys para Usuario e Instrumento
 * - Índices para performance em consultas
 * - Cascade delete quando entidades relacionadas são removidas
 * 
 * @author Jhonata
 * @version 1.0
 */
@Entity(tableName = "reservas",
        foreignKeys = {
            @ForeignKey(
                entity = Usuario.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.CASCADE
            ),
            @ForeignKey(
                entity = Instrumento.class,
                parentColumns = "id",
                childColumns = "idInstrumento",
                onDelete = ForeignKey.CASCADE
            )
        },
        indices = {@Index("idUsuario"), @Index("idInstrumento")})
public class Reserva {
    
    // Chave primária auto-incremento
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    // Dados da reserva
    private long idUsuario; // ID do usuário que está alugando
    private long idInstrumento; // ID do instrumento alugado
    private Date dataInicio; // Data de início do aluguel
    private Date dataFim; // Data de término do aluguel
    private double precoTotal; // Preço total do aluguel
    private String status; // Status da reserva (PENDING, CONFIRMED, CANCELLED, COMPLETED)
    private Date dataCriacao; // Data de criação da reserva

    /**
     * Construtor da reserva
     * 
     * @param idUsuario ID do usuário que está fazendo a reserva
     * @param idInstrumento ID do instrumento a ser reservado
     * @param dataInicio Data de início do período de aluguel
     * @param dataFim Data de término do período de aluguel
     * @param precoTotal Preço total calculado para o período
     * @param status Status inicial da reserva
     */
    public Reserva(long idUsuario, long idInstrumento, Date dataInicio, Date dataFim, double precoTotal, String status) {
        this.idUsuario = idUsuario;
        this.idInstrumento = idInstrumento;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.precoTotal = precoTotal;
        this.status = status;
        this.dataCriacao = new Date();
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único da reserva
     * @return ID da reserva
     */
    public long getId() {
        return id;
    }

    /**
     * Define o ID único da reserva
     * @param id ID da reserva
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Obtém o ID do usuário que fez a reserva
     * @return ID do usuário
     */
    public long getIdUsuario() {
        return idUsuario;
    }

    /**
     * Define o ID do usuário que fez a reserva
     * @param idUsuario ID do usuário
     */
    public void setIdUsuario(long idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * Obtém o ID do instrumento reservado
     * @return ID do instrumento
     */
    public long getIdInstrumento() {
        return idInstrumento;
    }

    /**
     * Define o ID do instrumento reservado
     * @param idInstrumento ID do instrumento
     */
    public void setIdInstrumento(long idInstrumento) {
        this.idInstrumento = idInstrumento;
    }

    /**
     * Obtém a data de início do aluguel
     * @return Data de início
     */
    public Date getDataInicio() {
        return dataInicio;
    }

    /**
     * Define a data de início do aluguel
     * @param dataInicio Data de início
     */
    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    /**
     * Obtém a data de término do aluguel
     * @return Data de término
     */
    public Date getDataFim() {
        return dataFim;
    }

    /**
     * Define a data de término do aluguel
     * @param dataFim Data de término
     */
    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }

    /**
     * Obtém o preço total da reserva
     * @return Preço total
     */
    public double getPrecoTotal() {
        return precoTotal;
    }

    /**
     * Define o preço total da reserva
     * @param precoTotal Preço total
     */
    public void setPrecoTotal(double precoTotal) {
        this.precoTotal = precoTotal;
    }

    /**
     * Obtém o status atual da reserva
     * @return Status da reserva (PENDING, CONFIRMED, CANCELLED, COMPLETED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Define o status da reserva
     * @param status Status da reserva
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Obtém a data de criação da reserva
     * @return Data de criação
     */
    public Date getDataCriacao() {
        return dataCriacao;
    }

    /**
     * Define a data de criação da reserva
     * @param dataCriacao Data de criação
     */
    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
} 