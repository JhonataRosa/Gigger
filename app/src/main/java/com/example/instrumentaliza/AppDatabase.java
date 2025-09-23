package com.example.instrumentaliza;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * AppDatabase - Banco de dados local usando Room
 * 
 * Esta classe representa o banco de dados local da aplicação usando Room Database.
 * Contém todas as entidades (Usuario, Instrumento, Reserva) e seus respectivos DAOs.
 * Implementa o padrão Singleton para acesso global ao banco de dados.
 * 
 * Funcionalidades principais:
 * - Gerenciamento de entidades do banco de dados
 * - Migrações de versão do banco
 * - Acesso aos DAOs das entidades
 * - Configuração de conversores de tipos
 * - Padrão Singleton para instância única
 * 
 * Características técnicas:
 * - Room Database com SQLite
 * - Migrações automáticas entre versões
 * - TypeConverters para tipos complexos
 * - Padrão Singleton thread-safe
 * - Logs detalhados para debugging
 * 
 * @author Jhonata
 * @version 1.0
 */
@Database(entities = {Usuario.class, Instrumento.class, Reserva.class}, version = 5, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    // Constantes
    private static final String TAG = "AppDatabase";
    private static AppDatabase instance;

    // DAOs das entidades
    public abstract UsuarioDao usuarioDao();
    public abstract InstrumentoDao instrumentoDao();
    public abstract ReservaDao reservaDao();

    private static final Migration MIGRATION_0_1 = new Migration(0, 1) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migração inicial para criar a tabela de usuários
            database.execSQL("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "nome TEXT NOT NULL, " +
                    "email TEXT NOT NULL, " +
                    "senha TEXT NOT NULL, " +
                    "telefone TEXT NOT NULL)");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migração da versão 1 para 2 (adição da tabela de instrumentos)
            database.execSQL("CREATE TABLE IF NOT EXISTS instrumentos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "idProprietario INTEGER NOT NULL, " +
                    "nome TEXT NOT NULL, " +
                    "descricao TEXT, " +
                    "categoria TEXT NOT NULL, " +
                    "preco REAL NOT NULL, " +
                    "uriImagem TEXT)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migração da versão 2 para 3 (adição da coluna uriImagemPerfil na tabela usuarios)
            database.execSQL("ALTER TABLE usuarios ADD COLUMN uriImagemPerfil TEXT");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migração da versão 3 para 4 (atualização dos tipos de ID para long)
            // Não é necessário fazer alterações no SQLite, pois INTEGER já suporta valores long
            Log.d(TAG, "Migração para tipos long concluída");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migração da versão 4 para 5 (adição da tabela de reservas)
            database.execSQL("CREATE TABLE IF NOT EXISTS reservas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "idUsuario INTEGER NOT NULL, " +
                    "idInstrumento INTEGER NOT NULL, " +
                    "dataInicio INTEGER NOT NULL, " +
                    "dataFim INTEGER NOT NULL, " +
                    "precoTotal REAL NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "dataCriacao INTEGER NOT NULL, " +
                    "FOREIGN KEY (idUsuario) REFERENCES usuarios(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (idInstrumento) REFERENCES instrumentos(id) ON DELETE CASCADE)");
        }
    };

    /**
     * Obtém a instância única do banco de dados (Singleton)
     * 
     * @param context Contexto da aplicação
     * @return Instância única do AppDatabase
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                Log.d(TAG, "Inicializando banco de dados...");
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "instrumentaliza_database"
                )
                .addMigrations(MIGRATION_0_1, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build();
                Log.d(TAG, "Banco de dados inicializado com sucesso");
            } catch (Exception e) {
                Log.e(TAG, "Erro ao inicializar banco de dados: " + e.getMessage());
                throw new RuntimeException("Erro ao inicializar banco de dados", e);
            }
        }
        return instance;
    }
} 