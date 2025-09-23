package com.example.instrumentaliza;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InstrumentoDao {
    @Query("SELECT * FROM instrumentos")
    List<Instrumento> obterTodos();

    @Query("SELECT * FROM instrumentos WHERE id = :id")
    Instrumento obterPorId(long id);

    @Insert
    long inserir(Instrumento instrumento);

    @Update
    void atualizar(Instrumento instrumento);

    @Delete
    void deletar(Instrumento instrumento);

    @Query("SELECT * FROM instrumentos WHERE categoria = :categoria")
    List<Instrumento> obterPorCategoria(String categoria);

    @Query("SELECT * FROM instrumentos WHERE nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%'")
    List<Instrumento> buscar(String consulta);

    @Query("SELECT * FROM instrumentos WHERE categoria = :categoria AND (nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%')")
    List<Instrumento> buscarPorCategoria(String consulta, String categoria);

    @Query("SELECT * FROM instrumentos ORDER BY preco ASC")
    List<Instrumento> obterTodosOrdenadosPorPrecoAsc();

    @Query("SELECT * FROM instrumentos ORDER BY preco DESC")
    List<Instrumento> obterTodosOrdenadosPorPrecoDesc();

    @Query("SELECT * FROM instrumentos WHERE categoria = :categoria ORDER BY preco ASC")
    List<Instrumento> obterPorCategoriaOrdenadosPorPrecoAsc(String categoria);

    @Query("SELECT * FROM instrumentos WHERE categoria = :categoria ORDER BY preco DESC")
    List<Instrumento> obterPorCategoriaOrdenadosPorPrecoDesc(String categoria);

    @Query("SELECT * FROM instrumentos WHERE nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%' ORDER BY preco ASC")
    List<Instrumento> buscarOrdenadosPorPrecoAsc(String consulta);

    @Query("SELECT * FROM instrumentos WHERE nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%' ORDER BY preco DESC")
    List<Instrumento> buscarOrdenadosPorPrecoDesc(String consulta);

    @Query("SELECT * FROM instrumentos WHERE (nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%') AND categoria = :categoria ORDER BY preco ASC")
    List<Instrumento> buscarPorCategoriaOrdenadosPorPrecoAsc(String consulta, String categoria);

    @Query("SELECT * FROM instrumentos WHERE (nome LIKE '%' || :consulta || '%' OR descricao LIKE '%' || :consulta || '%') AND categoria = :categoria ORDER BY preco DESC")
    List<Instrumento> buscarPorCategoriaOrdenadosPorPrecoDesc(String consulta, String categoria);
} 