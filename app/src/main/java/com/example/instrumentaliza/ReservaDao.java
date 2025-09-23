package com.example.instrumentaliza;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface ReservaDao {
    // Os parâmetros do tipo Date são suportados pelo Room devido ao TypeConverter registrado em AppDatabase
    @Query("SELECT * FROM reservas")
    List<Reserva> obterTodas();

    @Query("SELECT * FROM reservas WHERE id = :id")
    Reserva obterPorId(long id);

    @Query("SELECT * FROM reservas WHERE idUsuario = :idUsuario")
    List<Reserva> obterPorIdUsuario(long idUsuario);

    @Query("SELECT * FROM reservas WHERE idInstrumento = :idInstrumento")
    List<Reserva> obterPorIdInstrumento(long idInstrumento);

    @Query("SELECT * FROM reservas WHERE status = :status")
    List<Reserva> obterPorStatus(String status);

    @Query("SELECT * FROM reservas WHERE idUsuario = :idUsuario AND status = :status")
    List<Reserva> obterPorIdUsuarioEStatus(long idUsuario, String status);

    @Query("SELECT * FROM reservas WHERE idInstrumento = :idInstrumento AND status = :status")
    List<Reserva> obterPorIdInstrumentoEStatus(long idInstrumento, String status);

    @Query("SELECT * FROM reservas WHERE idInstrumento = :idInstrumento AND " +
           "((dataInicio <= :dataFim AND dataFim >= :dataInicio) OR " +
           "(dataInicio >= :dataInicio AND dataInicio <= :dataFim) OR " +
           "(dataFim >= :dataInicio AND dataFim <= :dataFim))")
    List<Reserva> obterReservasSobrepostas(long idInstrumento, Date dataInicio, Date dataFim);

    @Insert
    long inserir(Reserva reserva);

    @Update
    void atualizar(Reserva reserva);

    @Delete
    void deletar(Reserva reserva);
} 