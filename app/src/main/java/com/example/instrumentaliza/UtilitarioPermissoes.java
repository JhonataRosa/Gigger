package com.example.instrumentaliza;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * UtilitarioPermissoes - Gerenciador de permissões do app
 * 
 * Funcionalidades:
 * - Solicitação de permissões necessárias
 * - Verificação de status de permissões
 * - Gerenciamento de permissões de notificação
 * - Compatibilidade com diferentes versões do Android
 * 
 * @author Jhonata
 * @version 1.0
 */
public class UtilitarioPermissoes {
    
    private static final String TAG = "UtilitarioPermissoes";
    public static final int CODIGO_PERMISSAO_NOTIFICACOES = 1001;
    
    /**
     * Verifica se as permissões de notificação estão concedidas
     * 
     * @param activity Atividade atual
     * @return true se as permissões estão concedidas
     */
    public static boolean verificarPermissaoNotificacoes(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, 
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Para versões anteriores ao Android 13, não é necessária permissão
    }
    
    /**
     * Solicita permissão de notificações se necessário
     * 
     * @param activity Atividade atual
     */
    public static void solicitarPermissaoNotificacoes(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!verificarPermissaoNotificacoes(activity)) {
                Log.d(TAG, "Solicitando permissão de notificações");
                ActivityCompat.requestPermissions(activity, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        CODIGO_PERMISSAO_NOTIFICACOES);
            } else {
                Log.d(TAG, "Permissão de notificações já concedida");
            }
        }
    }
    
    /**
     * Verifica o resultado da solicitação de permissão
     * 
     * @param requestCode Código da solicitação
     * @param permissions Permissões solicitadas
     * @param grantResults Resultados das permissões
     * @return true se a permissão foi concedida
     */
    public static boolean verificarResultadoPermissao(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODIGO_PERMISSAO_NOTIFICACOES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de notificações concedida");
                return true;
            } else {
                Log.d(TAG, "Permissão de notificações negada");
                return false;
            }
        }
        return false;
    }
}
