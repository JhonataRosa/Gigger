package com.example.instrumentaliza.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/**
 * FirebaseUser - Modelo de dados para usuários no Firebase Firestore
 * 
 * Esta classe representa um usuário no Firebase Firestore, contendo
 * todas as informações necessárias para o sistema de aluguel de instrumentos.
 * É usada para serialização/deserialização de dados do Firestore.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos do usuário
 * - Gerenciamento de informações de perfil
 * - Controle de dados de contato
 * - Conversão de/para DocumentSnapshot do Firestore
 * 
 * Características técnicas:
 * - Modelo POJO para Firebase Firestore
 * - Conversão automática de tipos do Firestore
 * - Suporte a dados de perfil opcionais
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FirebaseUser {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String profileImageUri;
    private Date createdAt;

    /**
     * Construtor vazio necessário para o Firestore
     * 
     * O Firebase Firestore requer um construtor sem parâmetros
     * para deserialização automática dos dados.
     */
    public FirebaseUser() {
        // Construtor vazio necessário para o Firestore
    }

    /**
     * Construtor completo do usuário
     * 
     * @param name Nome do usuário
     * @param email Email do usuário
     * @param phone Telefone do usuário
     */
    public FirebaseUser(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = new Date();
        this.profileImageUri = null;
    }

    /**
     * Cria um FirebaseUser a partir de um DocumentSnapshot do Firestore
     * 
     * @param document DocumentSnapshot do Firestore
     * @return FirebaseUser populado com os dados do documento
     */
    public static FirebaseUser fromDocument(DocumentSnapshot document) {
        FirebaseUser user = new FirebaseUser();
        user.setId(document.getId());
        user.setName((String) document.get("name"));
        user.setEmail((String) document.get("email"));
        user.setPhone((String) document.get("phone"));
        user.setProfileImageUri((String) document.get("profileImageUri"));
        user.setCreatedAt((Date) document.get("createdAt"));
        return user;
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único do usuário
     * @return ID do usuário
     */
    public String getId() {
        return id;
    }

    /**
     * Define o ID único do usuário
     * @param id ID do usuário
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtém o nome do usuário
     * @return Nome do usuário
     */
    public String getName() {
        return name;
    }

    /**
     * Define o nome do usuário
     * @param name Nome do usuário
     */
    public void setName(String name) {
        this.name = name;
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
     * Obtém o telefone do usuário
     * @return Telefone do usuário
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Define o telefone do usuário
     * @param phone Telefone do usuário
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Obtém a URI da imagem de perfil do usuário
     * @return URI da imagem de perfil
     */
    public String getProfileImageUri() {
        return profileImageUri;
    }

    /**
     * Define a URI da imagem de perfil do usuário
     * @param profileImageUri URI da imagem de perfil
     */
    public void setProfileImageUri(String profileImageUri) {
        this.profileImageUri = profileImageUri;
    }

    /**
     * Obtém a data de criação da conta do usuário
     * @return Data de criação
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Define a data de criação da conta do usuário
     * @param createdAt Data de criação
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 