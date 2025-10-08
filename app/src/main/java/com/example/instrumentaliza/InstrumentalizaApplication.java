package com.example.instrumentaliza;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * InstrumentalizaApplication - Classe principal da aplicação
 * 
 * Gerencia o estado global da aplicação e inicializa componentes
 * essenciais como Firebase e sistema de notificações.
 * 
 * Funcionalidades:
 * - Inicialização do Firebase
 * - Gerenciamento global de notificações
 * - Controle do ciclo de vida da aplicação
 * - Verificação de autenticação
 * 
 * @author Jhonata
 * @version 1.0
 */
public class InstrumentalizaApplication extends Application {
    
    private static final String TAG = "InstrumentalizaApp";
    private static InstrumentalizaApplication instance;
    private GerenciadorNotificacoes gerenciadorNotificacoes;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Log.d(TAG, "Aplicação inicializada");
        
        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        
        // Inicializar gerenciador de notificações
        gerenciadorNotificacoes = new GerenciadorNotificacoes(this);
        
        // Verificar se usuário está logado e iniciar notificações
        verificarEAutenticarNotificacoes();
    }
    
    /**
     * Obtém a instância singleton da aplicação
     * 
     * @return Instância da aplicação
     */
    public static InstrumentalizaApplication getInstance() {
        return instance;
    }
    
    /**
     * Obtém o gerenciador de notificações
     * 
     * @return Gerenciador de notificações
     */
    public GerenciadorNotificacoes getGerenciadorNotificacoes() {
        return gerenciadorNotificacoes;
    }
    
    /**
     * Verifica se o usuário está logado e inicia as notificações
     */
    private void verificarEAutenticarNotificacoes() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            Log.d(TAG, "Usuário logado: " + usuario.getUid() + " - Iniciando notificações");
            gerenciadorNotificacoes.iniciarListenerSolicitacoes();
        } else {
            Log.d(TAG, "Usuário não logado - Notificações não iniciadas");
        }
    }
    
    /**
     * Reinicia as notificações (usado após login)
     */
    public void reiniciarNotificacoes() {
        if (gerenciadorNotificacoes != null) {
            gerenciadorNotificacoes.pararListenerSolicitacoes();
            verificarEAutenticarNotificacoes();
        }
    }
    
    /**
     * Para as notificações (usado durante logout)
     */
    public void pararNotificacoes() {
        if (gerenciadorNotificacoes != null) {
            gerenciadorNotificacoes.pararListenerSolicitacoes();
            gerenciadorNotificacoes.limparNotificacoes();
        }
    }
}
