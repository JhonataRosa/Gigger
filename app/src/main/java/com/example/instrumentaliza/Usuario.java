package com.example.instrumentaliza;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Usuario - Modelo de dados para usuários do sistema
 * 
 * Esta classe representa um usuário no sistema, contendo todas as informações
 * necessárias para identificação, autenticação e perfil pessoal.
 * É uma entidade Room Database que serve como referência para outras entidades.
 * 
 * Funcionalidades:
 * - Armazenamento de dados pessoais do usuário
 * - Gerenciamento de credenciais de acesso
 * - Suporte a imagem de perfil
 * - Relacionamento com outras entidades (Instrumento, Reserva)
 * 
 * Características técnicas:
 * - Entidade Room Database com chave primária auto-incremento
 * - Tabela "usuarios" no banco de dados local
 * - Referenciada por outras entidades via Foreign Key
 * 
 * @author Jhonata
 * @version 1.0
 */
@Entity(tableName = "usuarios")
public class Usuario {
    
    // Chave primária auto-incremento
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    // Dados pessoais do usuário
    private String nome;
    private String email;
    private String senha; 
    private String telefone;
    private String uriImagemPerfil;

    /**
     * Construtor do usuário
     * 
     * @param nome Nome completo do usuário
     * @param email Email do usuário (usado para login)
     * @param senha Senha do usuário (em produção seria criptografada)
     * @param telefone Telefone de contato do usuário
     */
    public Usuario(String nome, String email, String senha, String telefone) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único do usuário
     * @return ID do usuário
     */
    public long getId() {
        return id;
    }

    /**
     * Define o ID único do usuário
     * @param id ID do usuário
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Obtém o nome do usuário
     * @return Nome do usuário
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome do usuário
     * @param nome Nome do usuário
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Obtém o email do usuário
     * @return Email do usuário
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define o email do usuário
     * @param email Email do usuário
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtém a senha do usuário
     * @return Senha do usuário
     */
    public String getSenha() {
        return senha;
    }

    /**
     * Define a senha do usuário
     * @param senha Senha do usuário
     */
    public void setSenha(String senha) {
        this.senha = senha;
    }

    /**
     * Obtém o telefone do usuário
     * @return Telefone do usuário
     */
    public String getTelefone() {
        return telefone;
    }

    /**
     * Define o telefone do usuário
     * @param telefone Telefone do usuário
     */
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    /**
     * Obtém a URI da imagem de perfil do usuário
     * @return URI da imagem de perfil
     */
    public String getUriImagemPerfil() {
        return uriImagemPerfil;
    }

    /**
     * Define a URI da imagem de perfil do usuário
     * @param uriImagemPerfil URI da imagem de perfil
     */
    public void setUriImagemPerfil(String uriImagemPerfil) {
        this.uriImagemPerfil = uriImagemPerfil;
    }
} 