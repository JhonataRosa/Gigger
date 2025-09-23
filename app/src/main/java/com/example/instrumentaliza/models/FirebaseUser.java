package com.example.instrumentaliza.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

public class FirebaseUser {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String profileImageUri;
    private Date createdAt;

    public FirebaseUser() {
        // Construtor vazio necessário para o Firestore
    }

    public FirebaseUser(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = new Date();
        this.profileImageUri = null;
    }

    // Método para criar a partir de um DocumentSnapshot
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

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUri() {
        return profileImageUri;
    }

    public void setProfileImageUri(String profileImageUri) {
        this.profileImageUri = profileImageUri;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 