package com.example.instrumentaliza;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeDetalhesInstrumento - Tela de detalhes completos de um instrumento
 * 
 * Esta tela exibe todas as informações detalhadas de um instrumento específico,
 * incluindo dados do instrumento e informações do proprietário. Oferece diferentes
 * funcionalidades baseadas no tipo de usuário (proprietário ou locatário).
 * 
 * Funcionalidades para proprietários:
 * - Gerenciar disponibilidade do instrumento
 * - Visualizar todas as informações do instrumento
 * 
 * Funcionalidades para locatários:
 * - Visualizar disponibilidade do instrumento
 * - Iniciar conversa com o proprietário
 * - Ver informações de contato do proprietário
 * 
 * Características técnicas:
 * - Carregamento assíncrono de dados do Firebase
 * - Tratamento robusto de erros e estados de loading
 * - Interface adaptativa baseada no tipo de usuário
 * - Integração com sistema de chat
 * - Formatação adequada de preços e dados
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeDetalhesInstrumento extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "DetalhesInstrumento";
    
    // Componentes da interface - dados do instrumento
    private ImageView imagemInstrumento;
    private TextView textoNome, textoCategoria, textoPreco, textoDescricao;
    
    // Componentes da interface - dados do proprietário
    private TextView textoNomeProprietario, textoEmailProprietario, textoTelefoneProprietario;
    
    // Dados do instrumento
    private String idInstrumento;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de detalhes do instrumento, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização de todos os componentes da interface
     * - Validação do ID do instrumento recebido
     * - Verificação de integridade dos componentes
     * - Inicialização da autenticação Firebase
     * - Carregamento dos dados do instrumento
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_instrument_details);

            // Configurar Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getString(R.string.instrument_details_title));
            }

            // Inicializar componentes da interface - dados do instrumento
            imagemInstrumento = findViewById(R.id.instrumentImageView);
            textoNome = findViewById(R.id.nameTextView);
            textoCategoria = findViewById(R.id.categoryTextView);
            textoPreco = findViewById(R.id.priceTextView);
            textoDescricao = findViewById(R.id.descriptionTextView);
            
            // Inicializar componentes da interface - dados do proprietário
            textoNomeProprietario = findViewById(R.id.ownerNameTextView);
            textoEmailProprietario = findViewById(R.id.ownerEmailTextView);
            textoTelefoneProprietario = findViewById(R.id.ownerPhoneTextView);
            
            // Verificar se os componentes críticos foram encontrados
            if (textoPreco == null) {
                Log.e(TAG, "ERRO: textoPreco é null!");
            } else {
                Log.d(TAG, "textoPreco encontrado com sucesso");
            }

            // Inicializar autenticação Firebase
            autenticacao = FirebaseAuth.getInstance();

            // Obter e validar ID do instrumento
            idInstrumento = getIntent().getStringExtra("instrument_id");
            if (idInstrumento == null) {
                Toast.makeText(this, getString(R.string.error_id_not_provided), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Carregar dados do instrumento
            carregarDetalhesInstrumento();
        } catch (Exception e) {
            Log.e(TAG, "Erro no onCreate: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Carrega todos os detalhes do instrumento e dados do proprietário
     * 
     * Este método é responsável por:
     * 1. Buscar os dados do instrumento no Firebase Firestore
     * 2. Buscar os dados do proprietário do instrumento
     * 3. Atualizar a interface com todas as informações
     * 4. Configurar os botões de ação baseados no tipo de usuário
     * 5. Tratar erros e estados de carregamento
     * 
     * Fluxo de carregamento:
     * - Carrega documento do instrumento
     * - Extrai ID do proprietário
     * - Carrega dados do proprietário
     * - Atualiza interface na thread principal
     * - Configura botões de ação apropriados
     * 
     * Tratamento de erros:
     * - Instrumento não encontrado
     * - Falha ao carregar dados do proprietário
     * - Erros de conversão de dados
     * - Problemas de rede
     */
    private void carregarDetalhesInstrumento() {
        Log.d(TAG, "Carregando instrumento com ID: " + idInstrumento);
        
        // Carregar instrumento do Firebase Firestore
        GerenciadorFirebase.obterInstrumentoPorId(idInstrumento)
                .thenAccept(instrumentDoc -> {
                    if (instrumentDoc == null) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, getString(R.string.error_instrument_not_found), Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    Log.d(TAG, "Instrumento carregado: " + instrumentDoc.get("name"));
                    
                    // Log de debug para todos os campos
                    Log.d(TAG, "=== DEBUG: Campos do instrumento ===");
                    Log.d(TAG, "name: " + instrumentDoc.get("name"));
                    Log.d(TAG, "category: " + instrumentDoc.get("category"));
                    Log.d(TAG, "price: " + instrumentDoc.get("price") + " (tipo: " + (instrumentDoc.get("price") != null ? instrumentDoc.get("price").getClass().getSimpleName() : "null") + ")");
                    Log.d(TAG, "description: " + instrumentDoc.get("description"));
                    Log.d(TAG, "ownerId: " + instrumentDoc.get("ownerId"));
                    Log.d(TAG, "imageUri: " + instrumentDoc.get("imageUri"));
                    Log.d(TAG, "=====================================");
                    
                    // Carregar dados do proprietário
                    String ownerId = (String) instrumentDoc.get("ownerId");
                    GerenciadorFirebase.obterDadosUsuario(ownerId)
                            .thenAccept(ownerData -> {
                                runOnUiThread(() -> {
                                    try {
                                        // Atualizar imagem
                                        String imageUri = (String) instrumentDoc.get("imageUri");
                                        if (imageUri != null && !imageUri.isEmpty()) {
                                            Glide.with(this)
                                                    .load(imageUri)
                                                    .transition(DrawableTransitionOptions.withCrossFade())
                                                    .into(imagemInstrumento);
                                        } else {
                                            imagemInstrumento.setImageResource(R.drawable.ic_music_note);
                                        }

                                        // Atualizar dados do instrumento
                                        textoNome.setText((String) instrumentDoc.get("name"));
                                        textoCategoria.setText((String) instrumentDoc.get("category"));
                                        
                                        // Tratamento melhorado para o preço
                                        Object priceObj = instrumentDoc.get("price");
                                        if (priceObj != null) {
                                            try {
                                                double price = ((Number) priceObj).doubleValue();
                                                String priceText = String.format(Locale.getDefault(), "R$ %.2f/dia", price);
                                                textoPreco.setText(priceText);
                                                Log.d(TAG, "Preço carregado com sucesso: " + priceText);
                                                Log.d(TAG, "textoPreco.setText() executado com: " + priceText);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Erro ao converter preço: " + e.getMessage());
                                                textoPreco.setText(getString(R.string.info_no_data));
                                            }
                                        } else {
                                            Log.w(TAG, "Campo 'price' é nulo no documento");
                                            textoPreco.setText(getString(R.string.info_no_data));
                                        }
                                        
                                        textoDescricao.setText((String) instrumentDoc.get("description"));

                                        // Atualizar dados do proprietário
                                        if (ownerData != null) {
                                            String ownerName = (String) ownerData.get("name");
                                            String ownerEmail = (String) ownerData.get("email");
                                            String ownerPhone = (String) ownerData.get("phone");
                                            
                                            textoNomeProprietario.setText(ownerName != null ? ownerName : getString(R.string.info_no_data));
                                            textoEmailProprietario.setText(ownerEmail != null ? ownerEmail : getString(R.string.info_no_data));
                                            textoTelefoneProprietario.setText(ownerPhone != null ? ownerPhone : getString(R.string.info_no_data));
                                        } else {
                                            textoNomeProprietario.setText(getString(R.string.info_no_data));
                                            textoEmailProprietario.setText(getString(R.string.info_no_data));
                                            textoTelefoneProprietario.setText(getString(R.string.info_no_data));
                                        }

                                        // Configurar botões de ação
                                        Button reserveButton = findViewById(R.id.reserveButton);
                                        Button sendMessageButton = findViewById(R.id.sendMessageButton);
                                        Button manageAvailabilityButton = findViewById(R.id.manageAvailabilityButton);
                                        
                                        // Verificar se o usuário atual é o proprietário
                                        if (autenticacao.getCurrentUser() != null && autenticacao.getCurrentUser().getUid().equals(ownerId)) {
                                            // Usuário é o proprietário - mostrar botão de gerenciar disponibilidade
                                            reserveButton.setVisibility(View.GONE);
                                            sendMessageButton.setVisibility(View.GONE);
                                            manageAvailabilityButton.setVisibility(View.VISIBLE);
                                            
                                            manageAvailabilityButton.setOnClickListener(v -> {
                                                Intent intent = new Intent(AtividadeDetalhesInstrumento.this, AtividadeGerenciarDisponibilidade.class);
                                                intent.putExtra("instrument_id", idInstrumento);
                                                intent.putExtra("instrument_name", (String) instrumentDoc.get("name"));
                                                startActivity(intent);
                                            });
                                        } else {
                                            // Usuário não é o proprietário - mostrar botões de reserva e mensagem
                                            reserveButton.setVisibility(View.VISIBLE);
                                            sendMessageButton.setVisibility(View.VISIBLE);
                                            manageAvailabilityButton.setVisibility(View.GONE);
                                            
                                            // Alterar texto do botão de reserva para "VER DISPONIBILIDADE"
                                            reserveButton.setText(getString(R.string.view_availability));
                                            
                                            reserveButton.setOnClickListener(v -> {
                                                // Abrir tela de visualização de disponibilidade
                                                Intent intent = new Intent(AtividadeDetalhesInstrumento.this, AtividadeVisualizarDisponibilidadeInstrumento.class);
                                                intent.putExtra("instrument_id", idInstrumento);
                                                intent.putExtra("instrument_name", (String) instrumentDoc.get("name"));
                                                startActivity(intent);
                                            });
                                            
                                            sendMessageButton.setOnClickListener(v -> {
                                                openChatWithOwner(idInstrumento, ownerId);
                                            });
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Erro ao atualizar UI: " + e.getMessage(), e);
                                        Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .exceptionally(throwable -> {
                                Log.e(TAG, "Erro ao carregar dados do proprietário: " + throwable.getMessage(), throwable);
                                runOnUiThread(() -> {
                                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                                });
                                return null;
                            });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar instrumento: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return null;
                });
    }

    /**
     * Abre ou cria um chat com o proprietário do instrumento
     * 
     * Este método permite que um usuário interessado em alugar o instrumento
     * inicie uma conversa com o proprietário. O sistema verifica se já existe
     * um chat entre os usuários para o instrumento específico, ou cria um novo.
     * 
     * Fluxo de funcionamento:
     * 1. Verifica se o usuário está autenticado
     * 2. Obtém o ID do usuário atual (locatário)
     * 3. Chama o GerenciadorFirebase para criar/obter chat
     * 4. Navega para a tela de chat com os parâmetros necessários
     * 
     * @param idInstrumento ID do instrumento sobre o qual será a conversa
     * @param ownerId ID do proprietário do instrumento
     */
    private void openChatWithOwner(String idInstrumento, String ownerId) {
        FirebaseUser currentUser = autenticacao.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String locatorId = currentUser.getUid();
        
        // Mostrar feedback de carregamento
        Toast.makeText(this, getString(R.string.messages_title), Toast.LENGTH_SHORT).show();
        
        // Criar ou obter chat existente entre locatário e proprietário
        GerenciadorFirebase.criarOuObterChat(idInstrumento, locatorId, ownerId)
                .thenAccept(chatId -> {
                    runOnUiThread(() -> {
                        // Navegar para a tela de chat
                        Intent intent = new Intent(AtividadeDetalhesInstrumento.this, AtividadeChat.class);
                        intent.putExtra("chat_id", chatId);
                        intent.putExtra("instrument_id", idInstrumento);
                        startActivity(intent);
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao criar/obter chat: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    /**
     * Trata a seleção de itens do menu da toolbar
     * 
     * Gerencia principalmente o botão de voltar (home) da toolbar,
     * que fecha a atividade atual e retorna à tela anterior.
     * 
     * @param item Item do menu selecionado
     * @return true se o item foi tratado, false caso contrário
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Método chamado quando a atividade é destruída
     * 
     * Como esta atividade usa CompletableFuture para operações assíncronas
     * em vez de ExecutorService, não há recursos específicos para limpar.
     * O garbage collector cuidará da limpeza automática dos objetos.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Não há ExecutorService para limpar, pois usamos CompletableFuture
    }
} 