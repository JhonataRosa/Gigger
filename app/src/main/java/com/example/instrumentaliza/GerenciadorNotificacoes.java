package com.example.instrumentaliza;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * GerenciadorNotificacoes - Sistema de notificações para o app
 * 
 * Funcionalidades:
 * - Criação de canal de notificação
 * - Notificações para novas solicitações de reserva
 * - Notificações para mudanças de status
 * - Listener em tempo real para solicitações
 * - Notificações locais sem necessidade de Firebase Cloud Messaging
 * 
 * Características técnicas:
 * - Compatibilidade com Android 8.0+ (API 26+)
 * - Canal de notificação dedicado
 * - Intent para abrir a tela de solicitações
 * - Listener do Firestore para mudanças em tempo real
 * - Gerenciamento automático do ciclo de vida
 * 
 * @author Jhonata
 * @version 1.0
 */
public class GerenciadorNotificacoes {
    
    private static final String TAG = "GerenciadorNotificacoes";
    private static final String CHANNEL_ID = "solicitacoes_channel";
    private static final String CHANNEL_NAME = "Solicitações de Reserva";
    private static final String CHANNEL_DESCRIPTION = "Notificações sobre novas solicitações de reserva";
    
    private static final int NOTIFICATION_ID_NOVA_SOLICITACAO = 1001;
    private static final int NOTIFICATION_ID_STATUS_ALTERADO = 1002;
    
    private Context contexto;
    private NotificationManager notificationManager;
    private ListenerRegistration listenerSolicitacoes;
    private String idUsuarioAtual;
    
    /**
     * Construtor do gerenciador de notificações
     * 
     * @param contexto Contexto da aplicação
     */
    public GerenciadorNotificacoes(Context contexto) {
        this.contexto = contexto;
        this.notificationManager = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
        this.idUsuarioAtual = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        criarCanalNotificacao();
        Log.d(TAG, "GerenciadorNotificacoes inicializado para usuário: " + idUsuarioAtual);
    }
    
    /**
     * Cria o canal de notificação para Android 8.0+
     */
    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Canal de notificação criado: " + CHANNEL_NAME);
        }
    }
    
    /**
     * Inicia o listener para novas solicitações
     * 
     * Monitora a coleção de solicitações em tempo real e envia
     * notificações quando novas solicitações são criadas para o usuário atual.
     */
    public void iniciarListenerSolicitacoes() {
        if (idUsuarioAtual == null) {
            Log.w(TAG, "Usuário não logado - não é possível iniciar listener");
            return;
        }
        
        Log.d(TAG, "Iniciando listener para solicitações do usuário: " + idUsuarioAtual);
        
        listenerSolicitacoes = FirebaseFirestore.getInstance()
                .collection("solicitacoes")
                .whereEqualTo("proprietarioId", idUsuarioAtual)
                .whereEqualTo("status", "PENDENTE")
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null) {
                        Log.e(TAG, "Erro no listener de solicitações: " + erro.getMessage(), erro);
                        return;
                    }
                    
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Log.d(TAG, "Novas solicitações detectadas: " + snapshot.size());
                        
                        // Para cada nova solicitação, enviar notificação
                        for (com.google.firebase.firestore.DocumentChange change : snapshot.getDocumentChanges()) {
                            if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                String nomeInstrumento = change.getDocument().getString("instrumentoNome");
                                String nomeSolicitante = change.getDocument().getString("solicitanteNome");
                                
                                Log.d(TAG, "Nova solicitação: " + nomeInstrumento + " de " + nomeSolicitante);
                                enviarNotificacaoNovaSolicitacao(nomeInstrumento, nomeSolicitante);
                            }
                        }
                    }
                });
    }
    
    /**
     * Para o listener de solicitações
     */
    public void pararListenerSolicitacoes() {
        if (listenerSolicitacoes != null) {
            listenerSolicitacoes.remove();
            listenerSolicitacoes = null;
            Log.d(TAG, "Listener de solicitações parado");
        }
    }
    
    /**
     * Envia notificação para nova solicitação
     * 
     * @param nomeInstrumento Nome do instrumento solicitado
     * @param nomeSolicitante Nome do solicitante
     */
    private void enviarNotificacaoNovaSolicitacao(String nomeInstrumento, String nomeSolicitante) {
        if (nomeInstrumento == null) nomeInstrumento = "Instrumento";
        if (nomeSolicitante == null) nomeSolicitante = "Usuário";
        
        String titulo = "Nova Solicitação de Reserva";
        String mensagem = nomeSolicitante + " solicitou o instrumento \"" + nomeInstrumento + "\"";
        
        // Intent para abrir a tela de solicitações
        Intent intent = new Intent(contexto, AtividadeMeusInstrumentos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                contexto,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(mensagem + "\n\nToque para ver as solicitações pendentes."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});
        
        notificationManager.notify(NOTIFICATION_ID_NOVA_SOLICITACAO, builder.build());
        Log.d(TAG, "Notificação enviada: " + titulo);
    }
    
    /**
     * Envia notificação para mudança de status
     * 
     * @param nomeInstrumento Nome do instrumento
     * @param novoStatus Novo status da solicitação
     */
    public void enviarNotificacaoStatusAlterado(String nomeInstrumento, String novoStatus) {
        if (nomeInstrumento == null) nomeInstrumento = "Instrumento";
        
        String titulo = "Status da Solicitação Atualizado";
        String mensagem = "Sua solicitação para \"" + nomeInstrumento + "\" foi " + novoStatus.toLowerCase();
        
        // Intent para abrir a tela de solicitações
        Intent intent = new Intent(contexto, AtividadeMeusInstrumentos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                contexto,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(contexto, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(mensagem + "\n\nToque para ver os detalhes."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});
        
        notificationManager.notify(NOTIFICATION_ID_STATUS_ALTERADO, builder.build());
        Log.d(TAG, "Notificação de status enviada: " + titulo);
    }
    
    /**
     * Limpa todas as notificações
     */
    public void limparNotificacoes() {
        notificationManager.cancelAll();
        Log.d(TAG, "Todas as notificações foram limpas");
    }
    
    /**
     * Verifica se as notificações estão habilitadas
     * 
     * @return true se as notificações estão habilitadas
     */
    public boolean notificacoesHabilitadas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return notificationManager.areNotificationsEnabled();
        }
        return true; // Para versões anteriores ao Android 7.0
    }
}
