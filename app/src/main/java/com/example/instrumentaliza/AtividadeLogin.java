package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.CompletableFuture;

/**
 * AtividadeLogin - Tela de autenticação do usuário
 * 
 * Responsável por permitir que usuários existentes façam login no sistema
 * utilizando email e senha. Integra com Firebase Authentication para
 * validar as credenciais e gerenciar a sessão do usuário.
 * 
 * Funcionalidades:
 * - Validação de campos obrigatórios
 * - Autenticação via Firebase
 * - Tratamento de erros de login
 * - Redirecionamento automático para usuários já logados
 * - Prevenção de múltiplos logins simultâneos
 * 
 * @author Sistema Instrumentaliza
 * @version 1.0
 */
public class AtividadeLogin extends AppCompatActivity {
    private static final String TAG = "Login";
    
    // Componentes da interface
    private EditText campoEmail;
    private EditText campoSenha;
    private Button botaoLogin;
    private TextView linkCriarConta;
    
    // Controle de estado
    private boolean estaFazendoLogin = false;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de login, inicializa o Firebase,
     * verifica se o usuário já está autenticado e configura
     * os listeners dos componentes.
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Garantir que o Firebase está inicializado
        GerenciadorFirebase.inicializar(this);

        // Inicializar componentes da interface
        campoEmail = findViewById(R.id.emailEditText);
        campoSenha = findViewById(R.id.passwordEditText);
        botaoLogin = findViewById(R.id.loginButton);
        linkCriarConta = findViewById(R.id.registerLink);

        // Verificar se o usuário já está logado
        if (GerenciadorFirebase.usuarioEstaLogado()) {
            // Reiniciar notificações para usuário já logado
            InstrumentalizaApplication.getInstance().reiniciarNotificacoes();
            startActivity(new Intent(AtividadeLogin.this, AtividadeInstrumentos.class));
            finish();
            return;
        }

        // Configurar listener do botão de login
        botaoLogin.setOnClickListener(v -> tentarLogin());
        
        // Configurar listener do link criar conta
        linkCriarConta.setOnClickListener(v -> {
            startActivity(new Intent(AtividadeLogin.this, AtividadeRegistrar.class));
            finish();
        });
    }

    /**
     * Método responsável por processar a tentativa de login
     * 
     * Valida os campos de entrada, verifica se não há login em andamento,
     * desabilita o botão para evitar múltiplas tentativas e chama o
     * GerenciadorFirebase para autenticar o usuário.
     * 
     * Trata os seguintes cenários:
     * - Campos vazios: exibe mensagem de validação
     * - Login em andamento: ignora tentativas adicionais
     * - Sucesso: redireciona para tela de instrumentos
     * - Erro: exibe mensagem específica e reabilita botão
     */
    private void tentarLogin() {
        // Verificar se já há um login em andamento
        if (estaFazendoLogin) {
            Log.d(TAG, "Login já em andamento");
            return;
        }

        // Obter dados dos campos de entrada
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString().trim();

        // Validar se os campos não estão vazios
        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Marcar que o login está em andamento e desabilitar botão
        estaFazendoLogin = true;
        botaoLogin.setEnabled(false);

        // Fazer login com Firebase Authentication
        GerenciadorFirebase.entrarComEmailESenha(email, senha)
                .thenAccept(firebaseUser -> {
                    // Executar na thread principal para atualizar UI
                    runOnUiThread(() -> {
                        Log.d(TAG, "Login bem-sucedido: " + firebaseUser.getEmail());
                        Toast.makeText(this, getString(R.string.success_login), Toast.LENGTH_SHORT).show();
                        
                        // Solicitar permissão de notificações se necessário
                        UtilitarioPermissoes.solicitarPermissaoNotificacoes(AtividadeLogin.this);
                        
                        // Reiniciar notificações após login
                        InstrumentalizaApplication.getInstance().reiniciarNotificacoes();
                        
                        // Redirecionar para tela de instrumentos
                        Intent intent = new Intent(AtividadeLogin.this, AtividadeInstrumentos.class);
                        startActivity(intent);
                        finish();
                    });
                })
                .exceptionally(throwable -> {
                    // Executar na thread principal para atualizar UI
                    runOnUiThread(() -> {
                        Log.e(TAG, "Erro no login: " + throwable.getMessage());
                        
                        // Determinar mensagem de erro específica
                        String errorMessage = getString(R.string.error_auth);
                        if (throwable.getMessage() != null && throwable.getMessage().contains("password")) {
                            errorMessage = "Senha incorreta";
                        } else if (throwable.getMessage() != null && throwable.getMessage().contains("user")) {
                            errorMessage = getString(R.string.error_user_not_found);
                        }
                        
                        // Exibir erro e reabilitar botão
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        estaFazendoLogin = false;
                        botaoLogin.setEnabled(true);
                    });
            return null;
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (UtilitarioPermissoes.verificarResultadoPermissao(requestCode, permissions, grantResults)) {
            Toast.makeText(this, "Notificações habilitadas com sucesso!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notificações podem não funcionar corretamente", Toast.LENGTH_LONG).show();
        }
    }
}