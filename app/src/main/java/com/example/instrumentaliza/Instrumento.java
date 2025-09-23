package com.example.instrumentaliza;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * Instrumento - Modelo de dados para instrumentos musicais
 * 
 * Esta classe representa um instrumento musical no sistema, contendo todas
 * as informações necessárias para identificação, categorização e aluguel.
 * É uma entidade Room Database que mantém relacionamento com Usuario.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos do instrumento
 * - Relacionamento com proprietário (Usuario)
 * - Suporte a categorização por tipo
 * - Gerenciamento de preços de aluguel
 * - Armazenamento de URI de imagem
 * 
 * Características técnicas:
 * - Entidade Room Database com chave primária auto-incremento
 * - Foreign Key para Usuario (proprietário)
 * - Índice no campo idProprietario para performance
 * - Cascade delete quando proprietário é removido
 * 
 * @author Jhonata
 * @version 1.0
 */
@Entity(tableName = "instrumentos",
        foreignKeys = @ForeignKey(
                entity = Usuario.class,
                parentColumns = "id",
                childColumns = "idProprietario",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("idProprietario")})
public class Instrumento {
    
    // Chave primária auto-incremento
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    // Dados do instrumento
    private long idProprietario; // ID do usuário dono do instrumento
    private String nome;
    private String descricao;
    private String categoria;
    private double preco;
    private String uriImagem;

    /**
     * Construtor do instrumento
     * 
     * @param idProprietario ID do usuário proprietário do instrumento
     * @param nome Nome do instrumento
     * @param descricao Descrição detalhada do instrumento
     * @param categoria Categoria do instrumento (Cordas, Teclas, etc.)
     * @param preco Preço de aluguel por dia
     * @param uriImagem URI da imagem do instrumento
     */
    public Instrumento(long idProprietario, String nome, String descricao, String categoria, double preco, String uriImagem) {
        this.idProprietario = idProprietario;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.uriImagem = uriImagem;
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único do instrumento
     * @return ID do instrumento
     */
    public long getId() {
        return id;
    }

    /**
     * Define o ID único do instrumento
     * @param id ID do instrumento
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Obtém o ID do proprietário do instrumento
     * @return ID do usuário proprietário
     */
    public long getIdProprietario() {
        return idProprietario;
    }

    /**
     * Define o ID do proprietário do instrumento
     * @param idProprietario ID do usuário proprietário
     */
    public void setIdProprietario(long idProprietario) {
        this.idProprietario = idProprietario;
    }

    /**
     * Obtém o nome do instrumento
     * @return Nome do instrumento
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome do instrumento
     * @param nome Nome do instrumento
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Obtém a descrição do instrumento
     * @return Descrição do instrumento
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Define a descrição do instrumento
     * @param descricao Descrição do instrumento
     */
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Obtém a categoria do instrumento
     * @return Categoria do instrumento
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Define a categoria do instrumento
     * @param categoria Categoria do instrumento
     */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /**
     * Obtém o preço de aluguel do instrumento
     * @return Preço por dia
     */
    public double getPreco() {
        return preco;
    }

    /**
     * Define o preço de aluguel do instrumento
     * @param preco Preço por dia
     */
    public void setPreco(double preco) {
        this.preco = preco;
    }

    /**
     * Obtém a URI da imagem do instrumento
     * @return URI da imagem
     */
    public String getUriImagem() {
        return uriImagem;
    }

    /**
     * Define a URI da imagem do instrumento
     * @param uriImagem URI da imagem
     */
    public void setUriImagem(String uriImagem) {
        this.uriImagem = uriImagem;
    }
} 