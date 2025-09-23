package com.example.instrumentaliza;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * GerenciadorSessao - Gerenciador de sessão local do usuário
 * 
 * Esta classe gerencia o estado da sessão do usuário usando SharedPreferences
 * para armazenamento local. Mantém informações sobre o login, dados do usuário
 * e estado da aplicação entre execuções.
 * 
 * Funcionalidades principais:
 * - Criação e gerenciamento de sessão local
 * - Armazenamento de dados do usuário logado
 * - Verificação de estado de login
 * - Atualização de dados da sessão
 * - Limpeza de sessão no logout
 * 
 * Características técnicas:
 * - Usa SharedPreferences para persistência
 * - Singleton pattern para acesso global
 * - Operações síncronas e assíncronas
 * - Logs detalhados para debugging
 * - Tratamento de valores padrão
 * 
 * @author Jhonata
 * @version 1.0
 */
public class GerenciadorSessao {
    
    // Constantes para SharedPreferences
    private static final String TAG = "GerenciadorSessao";
    private static final String NOME_PREF = "InstrumentalizaSession";
    private static final String CHAVE_ESTA_LOGADO = "isLoggedIn";
    private static final String CHAVE_ID_USUARIO = "userId";
    private static final String CHAVE_NOME = "name";
    private static final String CHAVE_EMAIL = "email";

    // Componentes do SharedPreferences
    private final SharedPreferences preferencias;
    private final SharedPreferences.Editor editor;
    private final Context contexto;

    /**
     * Construtor do gerenciador de sessão
     * 
     * @param contexto Contexto da aplicação para acessar SharedPreferences
     */
    public GerenciadorSessao(Context contexto) {
        this.contexto = contexto;
        preferencias = contexto.getSharedPreferences(NOME_PREF, Context.MODE_PRIVATE);
        editor = preferencias.edit();
    }

    /**
     * Cria uma nova sessão para o usuário
     * 
     * @param usuario Usuário para o qual criar a sessão
     */
    public void createSession(Usuario usuario) {
        Log.d(TAG, "Criando sessão para usuário: " + usuario.getEmail());
        editor.putBoolean(CHAVE_ESTA_LOGADO, true);
        editor.putLong(CHAVE_ID_USUARIO, usuario.getId());
        editor.putString(CHAVE_NOME, usuario.getNome());
        editor.putString(CHAVE_EMAIL, usuario.getEmail());
        editor.commit();
    }

    /**
     * Verifica se o usuário está logado
     * 
     * @return true se o usuário está logado, false caso contrário
     */
    public boolean isLoggedIn() {
        return preferencias.getBoolean(CHAVE_ESTA_LOGADO, false);
    }

    /**
     * Obtém o ID do usuário logado
     * 
     * @return ID do usuário ou -1 se não estiver logado
     */
    public long getUserId() {
        return preferencias.getLong(CHAVE_ID_USUARIO, -1);
    }

    /**
     * Obtém o nome do usuário logado
     * 
     * @return Nome do usuário ou null se não estiver logado
     */
    public String getName() {
        return preferencias.getString(CHAVE_NOME, null);
    }

    /**
     * Obtém o email do usuário logado
     * 
     * @return Email do usuário ou null se não estiver logado
     */
    public String getEmail() {
        return preferencias.getString(CHAVE_EMAIL, null);
    }

    /**
     * Limpa todos os dados da sessão
     */
    public void limparSessao() {
        Log.d(TAG, "Limpando sessão do usuário");
        SharedPreferences.Editor editor = preferencias.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Atualiza os dados da sessão atual
     * 
     * @param nome Novo nome do usuário
     * @param email Novo email do usuário
     */
    public void atualizarSessao(String nome, String email) {
        Log.d(TAG, "Atualizando sessão para: " + email);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString(CHAVE_NOME, nome);
        editor.putString(CHAVE_EMAIL, email);
        editor.apply();
    }

    /**
     * Realiza logout do usuário
     */
    public void sair() {
        Log.d(TAG, "Realizando logout do usuário");
        limparSessao();
    }
} 