package com.example.instrumentaliza.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/**
 * FirebaseChat - Modelo de dados para conversas no Firebase Firestore
 * 
 * Esta classe representa uma conversa de chat no Firebase Firestore, contendo
 * informações sobre a conversa entre um locatário e um proprietário de instrumento.
 * É usada para serialização/deserialização de dados do Firestore.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos da conversa
 * - Relacionamento com instrumento, locatário e proprietário
 * - Controle de status da conversa (ativa/arquivada)
 * - Timestamps de criação e última mensagem
 * - Conversão de/para DocumentSnapshot do Firestore
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
public class FirebaseChat {
    
    // Dados básicos da conversa
    private String id;
    private String instrumentId;
    private String locatorId; // Quem quer alugar
    private String ownerId;   // Dono do instrumento
    
    // Timestamps e status
    private Date createdAt;
    private Date lastMessageAt;
    private String status; // "active" ou "archived"

    /**
     * Construtor padrão necessário para Firestore
     * 
     * O Firebase Firestore requer um construtor sem parâmetros
     * para deserialização automática dos dados.
     */
    public FirebaseChat() {}

    /**
     * Construtor completo da conversa
     * 
     * @param instrumentId ID do instrumento sobre o qual é a conversa
     * @param locatorId ID do usuário interessado em alugar
     * @param ownerId ID do proprietário do instrumento
     */
    public FirebaseChat(String instrumentId, String locatorId, String ownerId) {
        this.instrumentId = instrumentId;
        this.locatorId = locatorId;
        this.ownerId = ownerId;
        this.createdAt = new Date();
        this.lastMessageAt = new Date();
        this.status = "active";
    }

    /**
     * Cria um FirebaseChat a partir de um DocumentSnapshot do Firestore
     * 
     * @param document DocumentSnapshot do Firestore
     * @return FirebaseChat populado com os dados do documento
     */
    public static FirebaseChat fromDocument(DocumentSnapshot document) {
        FirebaseChat chat = new FirebaseChat();
        chat.id = document.getId();
        chat.instrumentId = document.getString("idInstrumento");
        chat.locatorId = document.getString("locatorId");
        chat.ownerId = document.getString("ownerId");
        chat.status = document.getString("status");
        
        // Converter Timestamps do Firebase para Date do Java
        if (document.getTimestamp("createdAt") != null) {
            chat.createdAt = document.getTimestamp("createdAt").toDate();
        }
        if (document.getTimestamp("lastMessageAt") != null) {
            chat.lastMessageAt = document.getTimestamp("lastMessageAt").toDate();
        }
        
        return chat;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

    public String getLocatorId() { return locatorId; }
    public void setLocatorId(String locatorId) { this.locatorId = locatorId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
