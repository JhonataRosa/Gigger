package com.example.instrumentaliza.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/**
 * FirebaseMessage - Modelo de dados para mensagens no Firebase Firestore
 * 
 * Esta classe representa uma mensagem de chat no Firebase Firestore, contendo
 * todas as informações necessárias para o sistema de mensagens entre usuários.
 * É usada para serialização/deserialização de dados do Firestore.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos da mensagem
 * - Relacionamento com conversa e remetente
 * - Controle de status de leitura
 * - Suporte a diferentes tipos de mensagem
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
public class FirebaseMessage {
    
    // Dados básicos da mensagem
    private String id;
    private String chatId;
    private String senderId;
    private String content;
    private Date timestamp;
    private String type; // "text", "image", "file"
    private boolean read;

    /**
     * Construtor padrão necessário para Firestore
     * 
     * O Firebase Firestore requer um construtor sem parâmetros
     * para deserialização automática dos dados.
     */
    public FirebaseMessage() {}

    /**
     * Construtor completo da mensagem
     * 
     * @param chatId ID da conversa à qual a mensagem pertence
     * @param senderId ID do usuário que enviou a mensagem
     * @param content Conteúdo da mensagem
     */
    public FirebaseMessage(String chatId, String senderId, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = new Date();
        this.type = "text";
        this.read = false;
    }

    /**
     * Cria um FirebaseMessage a partir de um DocumentSnapshot do Firestore
     * 
     * @param document DocumentSnapshot do Firestore
     * @return FirebaseMessage populado com os dados do documento
     */
    public static FirebaseMessage fromDocument(DocumentSnapshot document) {
        FirebaseMessage message = new FirebaseMessage();
        message.id = document.getId();
        message.chatId = document.getString("chatId");
        message.senderId = document.getString("senderId");
        message.content = document.getString("content");
        message.type = document.getString("type");
        message.read = document.getBoolean("read") != null ? document.getBoolean("read") : false;
        
        // Converter Timestamp do Firebase para Date do Java
        if (document.getTimestamp("timestamp") != null) {
            message.timestamp = document.getTimestamp("timestamp").toDate();
        }
        
        return message;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
