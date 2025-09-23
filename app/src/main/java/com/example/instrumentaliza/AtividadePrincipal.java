package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * AtividadePrincipal - Tela inicial do aplicativo Instrumentaliza
 * 
 * Esta é a primeira tela exibida quando o usuário abre o aplicativo.
 * Responsável por:
 * - Verificar se o usuário já está autenticado
 * - Redirecionar usuários logados para a tela de instrumentos
 * - Apresentar opções de login e registro para usuários não autenticados
 * - Inicializar o Firebase antes de qualquer operação
 * 
 * @author Sistema Instrumentaliza
 * @version 1.0
 */
public class AtividadePrincipal extends AppCompatActivity {

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface inicial do aplicativo, inicializa o Firebase,
     * verifica o status de autenticação do usuário e configura os listeners
     * dos botões de navegação.
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "=== MAINACTIVITY INICIADA ===");
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "onCreate iniciado");

        // Inicializar Firebase ANTES de qualquer uso
        GerenciadorFirebase.inicializar(this);

        // Verificar se o usuário já está logado no Firebase
        if (GerenciadorFirebase.usuarioEstaLogado()) {
            Log.d("MainActivity", "Usuário já logado, redirecionando para InstrumentsActivity");
            startActivity(new Intent(this, AtividadeInstrumentos.class));
            finish();
            return;
        }

        Log.d("MainActivity", "Procurando botões...");
        Button botaoLogin = findViewById(R.id.loginButton);
        Button botaoRegistrar = findViewById(R.id.registerButton);

        // Verificar se os botões foram encontrados corretamente
        if (botaoLogin == null) {
            Log.e("MainActivity", "ERRO: botaoLogin é null!");
        } else {
            Log.d("MainActivity", "botaoLogin encontrado com sucesso");
        }

        if (botaoRegistrar == null) {
            Log.e("MainActivity", "ERRO: botaoRegistrar é null!");
        } else {
            Log.d("MainActivity", "botaoRegistrar encontrado com sucesso");
        }

        // Configurar listener do botão de login
        botaoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "Botão LOGIN clicado - método tradicional");
                startActivity(new Intent(AtividadePrincipal.this, AtividadeLogin.class));
            }
        });

        // Configurar listener do botão de registro
        botaoRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "Botão REGISTER clicado - método tradicional");
                startActivity(new Intent(AtividadePrincipal.this, AtividadeRegistrar.class));
            }
        });

        Log.d("MainActivity", "Listeners configurados com sucesso");
    }

    /**
     * Método chamado quando a atividade retorna ao primeiro plano
     * 
     * Verifica novamente o status de autenticação do usuário.
     * Se o usuário estiver logado, redireciona automaticamente para
     * a tela de instrumentos. Isso garante que usuários que fizeram
     * login em outras telas sejam redirecionados corretamente.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Se o usuário fez logout e voltou para esta tela, não redireciona
        if (GerenciadorFirebase.usuarioEstaLogado()) {
            startActivity(new Intent(this, AtividadeInstrumentos.class));
            finish();
        }
    }
}