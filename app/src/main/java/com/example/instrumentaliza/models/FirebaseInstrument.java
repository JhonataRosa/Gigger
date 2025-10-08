package com.example.instrumentaliza.models;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * FirebaseInstrument - Modelo de dados para instrumentos no Firebase Firestore
 * 
 * Esta classe representa um instrumento musical no Firebase Firestore, contendo
 * todas as informações necessárias para o sistema de aluguel de instrumentos.
 * É usada para serialização/deserialização de dados do Firestore.
 * 
 * Funcionalidades:
 * - Armazenamento de dados básicos do instrumento
 * - Gerenciamento de disponibilidade
 * - Controle de períodos indisponíveis
 * - Conversão de/para DocumentSnapshot do Firestore
 * - Suporte a timestamps do Firebase
 * 
 * Características técnicas:
 * - Modelo POJO para Firebase Firestore
 * - Conversão automática de tipos do Firestore
 * - Suporte a listas complexas (unavailableRanges)
 * - Tratamento de timestamps Firebase/Java
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FirebaseInstrument {
    
    // Dados básicos do instrumento
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String category;
    private double price;
    private String imageUri;
    
    // Metadados e controle de disponibilidade
    private Date createdAt;
    private boolean available;
    private List<Map<String, Object>> unavailableRanges;
    
    // Sistema de avaliações
    private double notaMedia;
    private long totalAvaliacoes;

    /**
     * Construtor vazio necessário para o Firestore
     * 
     * O Firebase Firestore requer um construtor sem parâmetros
     * para deserialização automática dos dados.
     */
    public FirebaseInstrument() {
        // Construtor vazio necessário para o Firestore
    }

    /**
     * Construtor completo do instrumento
     * 
     * @param ownerId ID do proprietário do instrumento
     * @param name Nome do instrumento
     * @param description Descrição do instrumento
     * @param category Categoria do instrumento
     * @param price Preço de aluguel por dia
     * @param imageUri URI da imagem do instrumento
     */
    public FirebaseInstrument(String ownerId, String name, String description, String category, double price, String imageUri) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageUri = imageUri;
        this.createdAt = new Date();
        this.available = true;
        this.unavailableRanges = new ArrayList<>();
    }

    /**
     * Cria um FirebaseInstrument a partir de um DocumentSnapshot do Firestore
     * 
     * Este método converte um documento do Firestore em um objeto FirebaseInstrument,
     * tratando conversões de tipos e valores padrão para campos opcionais.
     * 
     * @param document DocumentSnapshot do Firestore
     * @return FirebaseInstrument populado com os dados do documento
     */
    public static FirebaseInstrument fromDocument(DocumentSnapshot document) {
        FirebaseInstrument instrument = new FirebaseInstrument();
        
        // Definir ID do documento
        instrument.setId(document.getId());
        
        // Carregar dados básicos
        instrument.setOwnerId((String) document.get("ownerId"));
        instrument.setName((String) document.get("name"));
        instrument.setDescription((String) document.get("description"));
        instrument.setCategory((String) document.get("category"));
        instrument.setPrice(((Number) document.get("price")).doubleValue());
        instrument.setImageUri((String) document.get("imageUri"));
        
        // Converter Timestamp do Firebase para Date do Java
        Object createdAtObj = document.get("createdAt");
        if (createdAtObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) createdAtObj;
            instrument.setCreatedAt(timestamp.toDate());
        } else if (createdAtObj instanceof Date) {
            instrument.setCreatedAt((Date) createdAtObj);
        } else {
            instrument.setCreatedAt(new Date()); // Fallback para data atual
        }
        
        // Carregar status de disponibilidade
        instrument.setAvailable((Boolean) document.get("available"));
        
        // Carregar faixas de indisponibilidade
        Object rangesObj = document.get("unavailableRanges");
        if (rangesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ranges = (List<Map<String, Object>>) rangesObj;
            instrument.setUnavailableRanges(ranges);
        } else {
            instrument.setUnavailableRanges(new ArrayList<>());
        }
        
        // Carregar dados de avaliações
        Object notaMediaObj = document.get("notaMedia");
        if (notaMediaObj instanceof Number) {
            instrument.setNotaMedia(((Number) notaMediaObj).doubleValue());
        } else {
            instrument.setNotaMedia(0.0);
        }
        
        Object totalAvaliacoesObj = document.get("totalAvaliacoes");
        if (totalAvaliacoesObj instanceof Number) {
            instrument.setTotalAvaliacoes(((Number) totalAvaliacoesObj).longValue());
        } else {
            instrument.setTotalAvaliacoes(0);
        }
        
        return instrument;
    }

    // ==================== GETTERS E SETTERS ====================
    
    /**
     * Obtém o ID único do instrumento
     * @return ID do instrumento
     */
    public String getId() {
        return id;
    }

    /**
     * Define o ID único do instrumento
     * @param id ID do instrumento
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtém o ID do proprietário do instrumento
     * @return ID do proprietário
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Define o ID do proprietário do instrumento
     * @param ownerId ID do proprietário
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Obtém o nome do instrumento
     * @return Nome do instrumento
     */
    public String getName() {
        return name;
    }

    /**
     * Define o nome do instrumento
     * @param name Nome do instrumento
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtém a descrição do instrumento
     * @return Descrição do instrumento
     */
    public String getDescription() {
        return description;
    }

    /**
     * Define a descrição do instrumento
     * @param description Descrição do instrumento
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Obtém a categoria do instrumento
     * @return Categoria do instrumento
     */
    public String getCategory() {
        return category;
    }

    /**
     * Define a categoria do instrumento
     * @param category Categoria do instrumento
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Obtém o preço de aluguel do instrumento
     * @return Preço por dia
     */
    public double getPrice() {
        return price;
    }

    /**
     * Define o preço de aluguel do instrumento
     * @param price Preço por dia
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Obtém a URI da imagem do instrumento
     * @return URI da imagem
     */
    public String getImageUri() {
        return imageUri;
    }

    /**
     * Define a URI da imagem do instrumento
     * @param imageUri URI da imagem
     */
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    /**
     * Obtém a data de criação do instrumento
     * @return Data de criação
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Define a data de criação do instrumento
     * @param createdAt Data de criação
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Verifica se o instrumento está disponível para aluguel
     * @return true se disponível, false caso contrário
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Define se o instrumento está disponível para aluguel
     * @param available true se disponível, false caso contrário
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Obtém as faixas de datas indisponíveis
     * @return Lista de faixas indisponíveis
     */
    public List<Map<String, Object>> getUnavailableRanges() {
        return unavailableRanges;
    }

    /**
     * Define as faixas de datas indisponíveis
     * @param unavailableRanges Lista de faixas indisponíveis
     */
    public void setUnavailableRanges(List<Map<String, Object>> unavailableRanges) {
        this.unavailableRanges = unavailableRanges;
    }
    
    /**
     * Obtém a nota média do instrumento
     * @return Nota média de 0.0 a 5.0
     */
    public double getNotaMedia() {
        return notaMedia;
    }
    
    /**
     * Define a nota média do instrumento
     * @param notaMedia Nota média de 0.0 a 5.0
     */
    public void setNotaMedia(double notaMedia) {
        this.notaMedia = Math.max(0.0, Math.min(5.0, notaMedia));
    }
    
    /**
     * Obtém o total de avaliações recebidas
     * @return Número total de avaliações
     */
    public long getTotalAvaliacoes() {
        return totalAvaliacoes;
    }
    
    /**
     * Define o total de avaliações recebidas
     * @param totalAvaliacoes Número total de avaliações
     */
    public void setTotalAvaliacoes(long totalAvaliacoes) {
        this.totalAvaliacoes = Math.max(0, totalAvaliacoes);
    }
    
    /**
     * Retorna a nota média formatada como string
     * @return String com nota formatada (ex: "4.5")
     */
    public String getNotaMediaFormatada() {
        return String.format("%.1f", notaMedia);
    }
    
    /**
     * Retorna as estrelas formatadas para exibição
     * @return String com estrelas (ex: "★★★★☆")
     */
    public String getEstrelasFormatadas() {
        StringBuilder estrelas = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= notaMedia) {
                estrelas.append("★");
            } else if (i - 0.5 <= notaMedia) {
                estrelas.append("★"); // Meia estrela
            } else {
                estrelas.append("☆");
            }
        }
        return estrelas.toString();
    }
    
    /**
     * Verifica se o instrumento possui avaliações
     * @return true se possui avaliações, false caso contrário
     */
    public boolean possuiAvaliacoes() {
        return totalAvaliacoes > 0;
    }
} 