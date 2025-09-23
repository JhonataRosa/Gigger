package com.example.instrumentaliza;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * AtividadeEditarPerfil - Tela de edição de perfil
 * 
 * Esta tela permite que o usuário edite suas informações pessoais,
 * incluindo nome, email, telefone e foto de perfil. Oferece
 * funcionalidades para atualizar dados e alterar imagem de perfil.
 * 
 * Funcionalidades principais:
 * - Edição de dados pessoais (nome, email, telefone)
 * - Alteração de foto de perfil
 * - Seleção de imagem da galeria
 * - Validação de campos obrigatórios
 * - Salvamento de alterações no Firebase
 * - Navegação de volta para perfil
 * 
 * Características técnicas:
 * - ActivityResultLauncher para seleção de imagem
 * - CircleImageView para foto de perfil
 * - TextInputEditText para campos de entrada
 * - Validação de dados antes do salvamento
 * - Upload de imagem para Firebase Storage
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeEditarPerfil extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "EditarPerfil";
    
    // Componentes da interface
    private TextInputEditText campoNome, campoEmail, campoTelefone;
    private CircleImageView imagemPerfil;
    
    // Dados da imagem
    private Uri uriImagemSelecionada;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Launcher para seleção de imagem da galeria
     * 
     * Configurado para capturar o resultado da seleção de imagem
     * e atualizar a interface com a imagem selecionada.
     */
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    uriImagemSelecionada = result.getData().getData();
                    imagemPerfil.setImageURI(uriImagemSelecionada);
                }
            });

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de edição de perfil, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração dos campos de entrada
     * - Carregamento dos dados atuais do usuário
     * - Configuração dos listeners dos botões
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Iniciando EditProfileActivity...");
            setContentView(R.layout.activity_edit_profile);

            // Inicializar Firebase
            GerenciadorFirebase.inicializar(this);
            autenticacao = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase inicializado");

            // Verificar se o usuário está logado
            if (!GerenciadorFirebase.usuarioEstaLogado()) {
                Log.d(TAG, "Usuário não está logado, redirecionando para LoginActivity");
                Toast.makeText(this, getString(R.string.error_auth), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AtividadeLogin.class));
                finish();
                return;
            }

            // Configurar toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getString(R.string.edit_profile));
            }

            // Inicializar views
            campoNome = findViewById(R.id.nameEditText);
            campoEmail = findViewById(R.id.emailEditText);
            campoTelefone = findViewById(R.id.phoneEditText);
            imagemPerfil = findViewById(R.id.profileImageView);
            MaterialButton changePhotoButton = findViewById(R.id.changePhotoButton);
            MaterialButton saveButton = findViewById(R.id.saveButton);

            // Carregar dados do usuário atual
            carregarDadosUsuario();

            // Configurar listeners
            changePhotoButton.setOnClickListener(v -> {
                Log.d(TAG, "Botão de alterar foto clicado");
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImage.launch(intent);
            });

            saveButton.setOnClickListener(v -> salvarPerfil());

            Log.d(TAG, "EditProfileActivity inicializada com sucesso");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar EditProfileActivity: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void carregarDadosUsuario() {
        try {
            FirebaseUser currentUser = autenticacao.getCurrentUser();
            if (currentUser != null) {
                // Exibir dados básicos do Firebase Auth
                campoNome.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
                campoEmail.setText(currentUser.getEmail());
                
                // Se há foto de perfil no Firebase Auth
                if (currentUser.getPhotoUrl() != null) {
                    Glide.with(AtividadeEditarPerfil.this)
                            .load(currentUser.getPhotoUrl())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(imagemPerfil);
                } else {
                    imagemPerfil.setImageResource(R.drawable.ic_profile);
                }
                
                // Tentar carregar dados adicionais do Firestore
                GerenciadorFirebase.obterDadosUsuario(currentUser.getUid())
                        .thenAccept(userData -> {
                            if (userData != null && !userData.isEmpty()) {
                                String displayName = (String) userData.get("name");
                                String profileImageUrl = (String) userData.get("profileImageUrl");
                                String phone = (String) userData.get("phone");
                                
                                runOnUiThread(() -> {
                                    if (displayName != null && !displayName.isEmpty()) {
                                        campoNome.setText(displayName);
                                    }
                                    if (phone != null && !phone.isEmpty()) {
                                        campoTelefone.setText(phone);
                                    } else {
                                        campoTelefone.setText("");
                                    }
                                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                        // Usar Glide para carregar a imagem de forma assíncrona
                                        Glide.with(AtividadeEditarPerfil.this)
                                                .load(profileImageUrl)
                                                .placeholder(R.drawable.ic_profile)
                                                .error(R.drawable.ic_profile)
                                                .into(imagemPerfil);
                                    } else {
                                        imagemPerfil.setImageResource(R.drawable.ic_profile);
                                    }
                                });
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Erro ao carregar dados do Firestore: " + throwable.getMessage(), throwable);
                            return null;
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar dados do usuário: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void salvarPerfil() {
        String name = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String phone = campoTelefone.getText().toString().trim();

        // Validação básica
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_required), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FirebaseUser currentUser = autenticacao.getCurrentUser();
            if (currentUser != null) {
                // Atualizar dados no Firestore
                GerenciadorFirebase.atualizarPerfilUsuario(currentUser.getUid(), name, email, phone)
                        .thenAccept(success -> {
                            if (success) {
                                // Se há uma nova imagem, fazer upload
                                if (uriImagemSelecionada != null) {
                                    String fileName = "profile_" + currentUser.getUid() + ".jpg";
                                    GerenciadorFirebase.enviarImagemPerfil(uriImagemSelecionada, fileName)
                                            .thenCompose(imageUrl -> {
                                                // Atualizar a URL da imagem no perfil
                                                return GerenciadorFirebase.atualizarImagemPerfilUsuario(currentUser.getUid(), imageUrl);
                                            })
                                            .thenAccept(imageSuccess -> {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(this, getString(R.string.success_update), Toast.LENGTH_SHORT).show();
                                                    finish();
                                                });
                                            })
                                            .exceptionally(throwable -> {
                                                Log.e(TAG, "Erro ao fazer upload da imagem: " + throwable.getMessage(), throwable);
                                                runOnUiThread(() -> 
                                                    Toast.makeText(this, getString(R.string.success_update), Toast.LENGTH_SHORT).show()
                                                );
                                                return null;
                                            });
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, getString(R.string.success_update), Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                }
                            } else {
                                runOnUiThread(() -> 
                                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                                );
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Erro ao salvar perfil: " + throwable.getMessage(), throwable);
                            runOnUiThread(() -> 
                                Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show()
                            );
                            return null;
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar perfil: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 