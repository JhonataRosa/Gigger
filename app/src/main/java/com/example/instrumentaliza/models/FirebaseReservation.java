package com.example.instrumentaliza.models;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.Date;

public class FirebaseReservation {
    private String id;
    private String userId;
    private String instrumentId;
    private Date startDate;
    private Date endDate;
    private double totalPrice;
    private String status;
    private Date createdAt;

    public FirebaseReservation() {
        // Construtor vazio necessário para o Firestore
    }

    public FirebaseReservation(String userId, String instrumentId, Date startDate, Date endDate, double totalPrice) {
        this.userId = userId;
        this.instrumentId = instrumentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = "PENDING";
        this.createdAt = new Date();
    }

    // Método para criar a partir de um DocumentSnapshot
    public static FirebaseReservation fromDocument(DocumentSnapshot document) {
        FirebaseReservation reservation = new FirebaseReservation();
        reservation.setId(document.getId());
        reservation.setUserId((String) document.get("userId"));
        reservation.setInstrumentId((String) document.get("instrumentId"));
        
        // Converter Timestamp para Date
        Timestamp startTimestamp = document.getTimestamp("startDate");
        if (startTimestamp != null) {
            reservation.setStartDate(startTimestamp.toDate());
        }
        
        Timestamp endTimestamp = document.getTimestamp("endDate");
        if (endTimestamp != null) {
            reservation.setEndDate(endTimestamp.toDate());
        }
        
        Timestamp createdAtTimestamp = document.getTimestamp("createdAt");
        if (createdAtTimestamp != null) {
            reservation.setCreatedAt(createdAtTimestamp.toDate());
        }
        
        Number totalPrice = (Number) document.get("totalPrice");
        if (totalPrice != null) {
            reservation.setTotalPrice(totalPrice.doubleValue());
        }
        
        reservation.setStatus((String) document.get("status"));
        return reservation;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 