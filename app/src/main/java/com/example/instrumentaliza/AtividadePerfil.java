package com.example.instrumentaliza;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * AtividadePerfil - Tela de perfil do usuário
 * 
 * Esta tela exibe as informações do perfil do usuário logado e oferece
 * funcionalidades relacionadas ao gerenciamento da conta. Utiliza um
 * sistema de abas para organizar diferentes seções do perfil.
 * 
 * Funcionalidades principais:
 * - Exibição de dados pessoais (nome, email, foto)
 * - Sistema de abas para organizar informações
 * - Edição de perfil através de FAB
 * - Carregamento de dados do Firebase Auth e Firestore
 * - Navegação para outras funcionalidades (reservas, logout)
 * 
 * Estrutura de abas:
 * - Aba 1: Dados pessoais (FragmentoDadosPerfil)
 * - Aba 2: Avaliações (FragmentoAvaliacoesPerfil)
 * 
 * Características técnicas:
 * - Integração com Firebase Auth para dados básicos
 * - Integração com Firestore para dados estendidos
 * - Carregamento assíncrono de imagens com Glide
 * - Sistema de ViewPager2 com TabLayout
 * - Tratamento robusto de erros
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadePerfil extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "Perfil";
    
    // Componentes da interface
    private TextView textoNome, textoEmail;
    private CircleImageView imagemPerfil;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de perfil do usuário, incluindo:
     * - Verificação de autenticação do usuário
     * - Configuração do sistema de abas (ViewPager2 + TabLayout)
     * - Inicialização dos componentes da interface
     * - Configuração do botão de edição de perfil
     * - Carregamento dos dados do usuário
     * 
     * Sistema de abas:
     * - Aba 0: Dados pessoais do usuário
     * - Aba 1: Avaliações e feedback
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Iniciando ProfileActivity...");
            setContentView(R.layout.activity_profile);

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

            // Inicializar componentes da interface
            textoNome = findViewById(R.id.nameTextView);
            textoEmail = findViewById(R.id.emailTextView);
            imagemPerfil = findViewById(R.id.profileImageView);
            FloatingActionButton botaoEditarPerfil = findViewById(R.id.editProfileFab);

            // Configurar sistema de abas (ViewPager2 + TabLayout)
            ViewPager2 viewPager = findViewById(R.id.profileViewPager);
            TabLayout tabLayout = findViewById(R.id.profileTabLayout);
            
            Log.d(TAG, "ViewPager2 encontrado: " + (viewPager != null ? "SIM" : "NÃO"));
            Log.d(TAG, "TabLayout encontrado: " + (tabLayout != null ? "SIM" : "NÃO"));
            
            // Configurar adaptador das abas
            AdaptadorTabsPerfil adaptadorTabs = new AdaptadorTabsPerfil(this);
            viewPager.setAdapter(adaptadorTabs);
            
            Log.d(TAG, "Adapter configurado no ViewPager2");
            
            // Conectar TabLayout com ViewPager2 e configurar títulos das abas
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText(getString(R.string.my_data));
                        Log.d(TAG, "Tab 0 configurado: Meus Dados");
                        break;
                    case 1:
                        tab.setText(getString(R.string.my_ratings));
                        Log.d(TAG, "Tab 1 configurado: Minhas Avaliações");
                        break;
                }
            }).attach();
            
            Log.d(TAG, "TabLayoutMediator configurado");

            // Configurar listener do botão de edição de perfil
            botaoEditarPerfil.setOnClickListener(v -> {
                Log.d(TAG, "Botão de edição de perfil clicado");
                startActivity(new Intent(this, AtividadeEditarPerfil.class));
            });

            // Carregar dados do usuário
            carregarDadosUsuario();

            Log.d(TAG, "ProfileActivity inicializada com sucesso");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar ProfileActivity: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Método chamado quando a atividade retorna ao primeiro plano
     * 
     * Recarrega os dados do usuário para garantir que as informações
     * estejam atualizadas, especialmente após edições de perfil.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar dados do usuário quando voltar da edição
        carregarDadosUsuario();
    }

    /**
     * Carrega e exibe os dados do usuário logado
     * 
     * Este método implementa uma estratégia de carregamento em duas etapas:
     * 1. Carrega dados básicos do Firebase Auth (nome, email, foto)
     * 2. Carrega dados estendidos do Firestore (nome personalizado, foto customizada)
     * 
     * Prioridade dos dados:
     * - Nome: Firestore > Firebase Auth > "Usuário"
     * - Foto: Firestore > Firebase Auth > ícone padrão
     * - Email: Firebase Auth (única fonte)
     * 
     * Características técnicas:
     * - Carregamento assíncrono com CompletableFuture
     * - Atualização da UI na thread principal
     * - Tratamento de erros robusto
     * - Fallback para dados padrão em caso de falha
     */
    private void carregarDadosUsuario() {
        try {
            FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
            if (usuarioAtual != null) {
                // Exibir dados básicos do Firebase Auth imediatamente
                textoNome.setText(usuarioAtual.getDisplayName() != null ? usuarioAtual.getDisplayName() : getString(R.string.user_default_name));
                textoEmail.setText(usuarioAtual.getEmail());
                
                // Carregar foto de perfil do Firebase Auth se disponível
                if (usuarioAtual.getPhotoUrl() != null) {
                    Glide.with(this)
                            .load(usuarioAtual.getPhotoUrl())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(imagemPerfil);
                } else {
                    imagemPerfil.setImageResource(R.drawable.ic_profile);
                }
                
                // Carregar dados adicionais do Firestore (sobrescreve dados básicos se disponíveis)
                GerenciadorFirebase.obterDadosUsuario(usuarioAtual.getUid())
                        .thenAccept(userData -> {
                            if (userData != null && !userData.isEmpty()) {
                                String displayName = (String) userData.get("name");
                                String profileImageUrl = (String) userData.get("profileImageUrl");
                                
                                runOnUiThread(() -> {
                                    // Atualizar nome se disponível no Firestore
                                    if (displayName != null && !displayName.isEmpty()) {
                                        textoNome.setText(displayName);
                                    }
                                    
                                    // Atualizar foto se disponível no Firestore
                                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                        // Usar Glide para carregar a imagem de forma assíncrona
                                        Glide.with(AtividadePerfil.this)
                                                .load(profileImageUrl)
                                                .placeholder(R.drawable.ic_profile)
                                                .error(R.drawable.ic_profile)
                                                .into(imagemPerfil);
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

    /**
     * Trata a seleção de itens do menu da toolbar
     * 
     * Gerencia as ações disponíveis no menu da tela de perfil:
     * - Botão voltar: retorna à tela anterior
     * - Logout: faz logout do usuário e retorna à tela inicial
     * - Minhas reservas: navega para a tela de reservas do usuário
     * 
     * @param item Item do menu selecionado
     * @return true se o item foi tratado, false caso contrário
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            // Fazer logout e retornar à tela inicial
            GerenciadorFirebase.sair();
            startActivity(new Intent(this, AtividadePrincipal.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_my_reservations) {
            // Navegar para tela de reservas do usuário
            startActivity(new Intent(this, AtividadeMinhasReservas.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 