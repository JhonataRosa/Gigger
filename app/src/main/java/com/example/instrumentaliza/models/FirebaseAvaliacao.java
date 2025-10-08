package com.example.instrumentaliza.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo FirebaseAvaliacao - Representa uma avaliação de aluguel
 * 
 * Esta classe representa uma avaliação feita por um locatário sobre um instrumento
 * alugado, incluindo nota (1-5 estrelas), comentário opcional, e informações
 * sobre o aluguel avaliado.
 * 
 * Características:
 * - Nota de 1 a 5 estrelas
 * - Comentário opcional do avaliador
 * - Referência ao aluguel avaliado
 * - Referência ao instrumento e proprietário
 * - Data da avaliação
 * - Informações do avaliador
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FirebaseAvaliacao {
    
    // Constantes
    private static final String TAG = "FirebaseAvaliacao";
    
    // Campos obrigatórios
    private String id;
    private String instrumentoId;
    private String instrumentoNome;
    private String proprietarioId;
    private String proprietarioNome;
    private String locatarioId;
    private String locatarioNome;
    private String reservaId;
    private float nota; // 1-5 estrelas (suporta meias estrelas: 3.5, 4.0, 4.5)
    private String comentario; // Opcional
    private Timestamp dataAvaliacao;
    private Timestamp dataCriacao;
    
    // Construtor vazio (necessário para Firestore)
    public FirebaseAvaliacao() {}
    
    /**
     * Construtor principal para criar nova avaliação
     * 
     * @param instrumentoId ID do instrumento avaliado
     * @param instrumentoNome Nome do instrumento
     * @param proprietarioId ID do proprietário
     * @param proprietarioNome Nome do proprietário
     * @param locatarioId ID do locatário que está avaliando
     * @param locatarioNome Nome do locatário
     * @param reservaId ID da reserva relacionada
     * @param nota Nota de 1 a 5 estrelas
     * @param comentario Comentário opcional
     */
    public FirebaseAvaliacao(String instrumentoId, String instrumentoNome, 
                           String proprietarioId, String proprietarioNome,
                           String locatarioId, String locatarioNome,
                           String reservaId, float nota, String comentario) {
        this.instrumentoId = instrumentoId;
        this.instrumentoNome = instrumentoNome;
        this.proprietarioId = proprietarioId;
        this.proprietarioNome = proprietarioNome;
        this.locatarioId = locatarioId;
        this.locatarioNome = locatarioNome;
        this.reservaId = reservaId;
        this.nota = nota;
        this.comentario = comentario;
        this.dataAvaliacao = Timestamp.now();
        this.dataCriacao = Timestamp.now();
    }
    
    /**
     * Cria uma avaliação a partir de um documento do Firestore
     * 
     * @param document Documento do Firestore
     * @return Instância de FirebaseAvaliacao
     */
    public static FirebaseAvaliacao fromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        
        FirebaseAvaliacao avaliacao = new FirebaseAvaliacao();
        avaliacao.id = document.getId();
        
        // Campos obrigatórios
        avaliacao.instrumentoId = document.getString("instrumentoId");
        avaliacao.instrumentoNome = document.getString("instrumentoNome");
        avaliacao.proprietarioId = document.getString("proprietarioId");
        avaliacao.proprietarioNome = document.getString("proprietarioNome");
        avaliacao.locatarioId = document.getString("locatarioId");
        avaliacao.locatarioNome = document.getString("locatarioNome");
        avaliacao.reservaId = document.getString("reservaId");
        avaliacao.nota = document.getDouble("nota") != null ? document.getDouble("nota").floatValue() : 0.0f;
        
        // Campos opcionais
        avaliacao.comentario = document.getString("comentario");
        avaliacao.dataAvaliacao = document.getTimestamp("dataAvaliacao");
        avaliacao.dataCriacao = document.getTimestamp("dataCriacao");
        
        return avaliacao;
    }
    
    /**
     * Converte a avaliação para um Map para salvar no Firestore
     * 
     * @return Map com os dados da avaliação
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        // Campos obrigatórios
        map.put("instrumentoId", instrumentoId);
        map.put("instrumentoNome", instrumentoNome);
        map.put("proprietarioId", proprietarioId);
        map.put("proprietarioNome", proprietarioNome);
        map.put("locatarioId", locatarioId);
        map.put("locatarioNome", locatarioNome);
        map.put("reservaId", reservaId);
        map.put("nota", (double) nota);
        
        // Campos opcionais
        if (comentario != null && !comentario.trim().isEmpty()) {
            map.put("comentario", comentario.trim());
        }
        
        // Datas
        map.put("dataAvaliacao", dataAvaliacao != null ? dataAvaliacao : Timestamp.now());
        map.put("dataCriacao", dataCriacao != null ? dataCriacao : Timestamp.now());
        
        return map;
    }
    
    /**
     * Valida se a avaliação está completa e válida
     * 
     * @return true se válida, false caso contrário
     */
    public boolean isValid() {
        return instrumentoId != null && !instrumentoId.trim().isEmpty() &&
               instrumentoNome != null && !instrumentoNome.trim().isEmpty() &&
               proprietarioId != null && !proprietarioId.trim().isEmpty() &&
               proprietarioNome != null && !proprietarioNome.trim().isEmpty() &&
               locatarioId != null && !locatarioId.trim().isEmpty() &&
               locatarioNome != null && !locatarioNome.trim().isEmpty() &&
               reservaId != null && !reservaId.trim().isEmpty() &&
               nota >= 1 && nota <= 5;
    }
    
    /**
     * Retorna a nota formatada como string com estrelas
     * 
     * @return String com estrelas (ex: "★★★★☆")
     */
    public String getNotaFormatada() {
        StringBuilder estrelas = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= nota) {
                estrelas.append("★");
            } else {
                estrelas.append("☆");
            }
        }
        return estrelas.toString();
    }
    
    /**
     * Retorna a nota formatada como número decimal
     * 
     * @return String com nota (ex: "4.5")
     */
    public String getNotaDecimal() {
        return String.format("%.1f", (float) nota);
    }
    
    /**
     * Retorna a data formatada para exibição
     * 
     * @return String com data formatada
     */
    public String getDataFormatada() {
        if (dataAvaliacao != null) {
            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return formato.format(dataAvaliacao.toDate());
        }
        return "";
    }
    
    // Getters e Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getInstrumentoId() {
        return instrumentoId;
    }
    
    public void setInstrumentoId(String instrumentoId) {
        this.instrumentoId = instrumentoId;
    }
    
    public String getInstrumentoNome() {
        return instrumentoNome;
    }
    
    public void setInstrumentoNome(String instrumentoNome) {
        this.instrumentoNome = instrumentoNome;
    }
    
    public String getProprietarioId() {
        return proprietarioId;
    }
    
    public void setProprietarioId(String proprietarioId) {
        this.proprietarioId = proprietarioId;
    }
    
    public String getProprietarioNome() {
        return proprietarioNome;
    }
    
    public void setProprietarioNome(String proprietarioNome) {
        this.proprietarioNome = proprietarioNome;
    }
    
    public String getLocatarioId() {
        return locatarioId;
    }
    
    public void setLocatarioId(String locatarioId) {
        this.locatarioId = locatarioId;
    }
    
    public String getLocatarioNome() {
        return locatarioNome;
    }
    
    public void setLocatarioNome(String locatarioNome) {
        this.locatarioNome = locatarioNome;
    }
    
    public String getReservaId() {
        return reservaId;
    }
    
    public void setReservaId(String reservaId) {
        this.reservaId = reservaId;
    }
    
    public float getNota() {
        return nota;
    }
    
    public void setNota(float nota) {
        this.nota = Math.max(1.0f, Math.min(5.0f, nota)); // Garantir que está entre 1 e 5
    }
    
    public String getComentario() {
        return comentario;
    }
    
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
    
    public Timestamp getDataAvaliacao() {
        return dataAvaliacao;
    }
    
    public void setDataAvaliacao(Timestamp dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }
    
    public Timestamp getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(Timestamp dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
