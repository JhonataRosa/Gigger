package com.example.instrumentaliza;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE email = :email AND senha = :senha LIMIT 1")
    Usuario fazerLogin(String email, String senha);

    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario obterPorId(long id);

    @Insert
    long inserir(Usuario usuario);

    @Update
    void atualizar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario buscarPorEmail(String email);
} 