package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeRegistrar - Tela de cadastro de novos usuários
 * 
 * Responsável por permitir que novos usuários se cadastrem no sistema
 * fornecendo nome, email, telefone e senha. Integra com Firebase Authentication
 * para criar a conta e Firebase Firestore para armazenar dados adicionais.
 * 
 * Funcionalidades:
 * - Validação de campos obrigatórios
 * - Validação de confirmação de senha
 * - Validação de tamanho mínimo da senha
 * - Criação de conta no Firebase Auth
 * - Criação de documento no Firestore
 * - Tratamento de erros específicos
 * - Prevenção de múltiplos registros simultâneos
 * - Redirecionamento automático após sucesso
 * 
 * @author Sistema Instrumentaliza
 * @version 1.0
 */
public class AtividadeRegistrar extends AppCompatActivity {
    private static final String TAG = "Registrar";
    
    // Componentes da interface
    private TextInputEditText campoNome, campoEmail, campoTelefone, campoSenha, campoConfirmarSenha;
    private Button botaoRegistrar;
    
    // Controle de estado
    private boolean estaRegistrando = false;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de registro, inicializa o Firebase,
     * configura os componentes da interface e define os listeners.
     * Inclui tratamento de exceções para garantir estabilidade.
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Iniciando RegisterActivity...");
            setContentView(R.layout.activity_register);

            // Garantir que o Firebase está inicializado
            GerenciadorFirebase.inicializar(this);

            // Inicializar componentes da interface
            campoNome = findViewById(R.id.nameEditText);
            campoEmail = findViewById(R.id.emailEditText);
            campoTelefone = findViewById(R.id.phoneEditText);
            campoSenha = findViewById(R.id.passwordEditText);
            campoConfirmarSenha = findViewById(R.id.confirmPasswordEditText);
            botaoRegistrar = findViewById(R.id.registerButton);

            // Configurar listener do botão de registro
            botaoRegistrar.setOnClickListener(v -> registrar());

            Log.d(TAG, "RegisterActivity inicializada com sucesso");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar RegisterActivity: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Método responsável por processar o registro do usuário
     * 
     * Valida todos os campos de entrada, verifica se não há registro em andamento,
     * desabilita o botão para evitar múltiplas tentativas e chama o
     * GerenciadorFirebase para criar a conta e o documento do usuário.
     * 
     * Processo de validação:
     * - Campos obrigatórios não vazios
     * - Senha e confirmação de senha idênticas
     * - Senha com pelo menos 6 caracteres
     * 
     * Processo de criação:
     * - Criação da conta no Firebase Auth
     * - Criação do documento no Firestore
     * - Verificação do login automático
     * - Redirecionamento para tela principal
     * 
     * Tratamento de erros:
     * - Email já cadastrado
     * - Senha muito fraca
     * - Problemas de rede
     * - Erros genéricos
     */
    private void registrar() {
        // Verificar se já há um registro em andamento
        if (estaRegistrando) {
            Log.d(TAG, "Registro já está em andamento");
            return;
        }

        try {
            Log.d(TAG, "Iniciando processo de registro...");
            
            // Obter dados dos campos de entrada
            String nome = campoNome.getText().toString().trim();
            String email = campoEmail.getText().toString().trim();
            String telefone = campoTelefone.getText().toString().trim();
            String senha = campoSenha.getText().toString();
            String confirmarSenha = campoConfirmarSenha.getText().toString();

            Log.d(TAG, "Dados coletados - Nome: " + nome + ", Email: " + email + ", Telefone: " + telefone);

            // Validação de campos obrigatórios
            if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Log.d(TAG, "Campos vazios detectados");
                Toast.makeText(this, getString(R.string.validation_required), Toast.LENGTH_SHORT).show();
                return;
            }

            // Validação de confirmação de senha
            if (!senha.equals(confirmarSenha)) {
                Log.d(TAG, "Senhas não coincidem");
                Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show();
                return;
            }

            // Validação de tamanho mínimo da senha
            if (senha.length() < 6) {
                Log.d(TAG, "Senha muito curta");
                Toast.makeText(this, getString(R.string.validation_password_min), Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Validações passaram, iniciando criação do usuário...");

            // Marcar que o registro está em andamento e desabilitar botão
            estaRegistrando = true;
            botaoRegistrar.setEnabled(false);

            // Criar usuário no Firebase Authentication
            Log.d(TAG, "Chamando GerenciadorFirebase.criarUsuarioComEmailESenha...");
            GerenciadorFirebase.criarUsuarioComEmailESenha(email, senha, nome)
                    .thenCompose(firebaseUser -> {
                        Log.d(TAG, "Usuário criado no Auth, criando documento no Firestore...");
                        // Criar documento do usuário no Firestore com dados adicionais
                        return GerenciadorFirebase.criarDocumentoUsuario(firebaseUser, nome, telefone);
                    })
                    .thenAccept(aVoid -> {
                        Log.d(TAG, "Documento criado com sucesso, redirecionando...");
                        runOnUiThread(() -> {
                            Toast.makeText(this, getString(R.string.success_register), Toast.LENGTH_SHORT).show();
                            
                            // Aguardar um pouco para garantir que o Firebase Auth seja atualizado
                            new android.os.Handler().postDelayed(() -> {
                                Log.d(TAG, "Verificando se usuário está logado: " + GerenciadorFirebase.usuarioEstaLogado());
                                if (GerenciadorFirebase.usuarioEstaLogado()) {
                                    Log.d(TAG, "Usuário logado com sucesso, redirecionando para InstrumentsActivity");
                                    startActivity(new Intent(AtividadeRegistrar.this, AtividadeInstrumentos.class));
                                    finishAffinity(); // Fecha todas as activities anteriores
                                } else {
                                    Log.e(TAG, "ERRO: Usuário não está logado após criação!");
                                    Toast.makeText(this, getString(R.string.error_auth), Toast.LENGTH_LONG).show();
                                    botaoRegistrar.setEnabled(true);
                                    estaRegistrando = false;
                                }
                            }, 1000); // Aguardar 1 segundo
                        });
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Erro no processo de registro: " + throwable.getMessage(), throwable);
                        runOnUiThread(() -> {
                            // Determinar mensagem de erro específica
                            String errorMessage = getString(R.string.error_generic);
                            if (throwable.getMessage() != null && throwable.getMessage().contains("email")) {
                                errorMessage = getString(R.string.email_already_registered);
                            } else if (throwable.getMessage() != null && throwable.getMessage().contains("password")) {
                                errorMessage = getString(R.string.validation_password_min);
                            } else if (throwable.getMessage() != null && throwable.getMessage().contains("network")) {
                                errorMessage = getString(R.string.error_network);
                            }
                            
                            // Exibir erro e reabilitar botão
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            botaoRegistrar.setEnabled(true);
                            estaRegistrando = false;
                        });
                        return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar registro: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            botaoRegistrar.setEnabled(true);
            estaRegistrando = false;
        }
    }
} 