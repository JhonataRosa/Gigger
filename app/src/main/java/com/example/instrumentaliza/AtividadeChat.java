package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AtividadeChat - Tela de conversa individual
 * 
 * Esta tela permite que usuários conversem sobre um instrumento específico,
 * facilitando a comunicação entre locatários e proprietários durante
 * o processo de negociação de aluguel.
 * 
 * Funcionalidades principais:
 * - Exibição de mensagens da conversa
 * - Envio de novas mensagens
 * - Carregamento automático de mensagens
 * - Diferenciação entre mensagens próprias e do outro usuário
 * - Navegação de volta para lista de conversas
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para mensagens
 * - Carregamento assíncrono do Firebase
 * - Scroll automático para última mensagem
 * - Validação de campos de entrada
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeChat extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "AtividadeChat";

    // Dados da conversa
    private String idChat;
    private String idInstrumento;
    private String idProprietario;
    private String nomeInstrumento;
    
    // Componentes da interface
    private RecyclerView listaMensagens;
    private EditText campoMensagem;
    private ImageButton botaoEnviar;
    private TextView textoNomeInstrumento;
    
    // Gerenciamento de dados
    private AdaptadorMensagensChat adaptadorMensagens;
    private FirebaseAuth autenticacao;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de chat, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Obtenção de dados do Intent (chat_id, instrument_id, owner_id)
     * - Configuração do RecyclerView e adaptador
     * - Carregamento das mensagens da conversa
     * - Configuração do envio de mensagens
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();

        idChat = getIntent().getStringExtra("chat_id");
        idInstrumento = getIntent().getStringExtra("instrument_id");
        idProprietario = getIntent().getStringExtra("owner_id");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.chat_title));
        }

        listaMensagens = findViewById(R.id.messagesRecyclerView);
        campoMensagem = findViewById(R.id.messageEditText);
        botaoEnviar = findViewById(R.id.sendButton);
        textoNomeInstrumento = findViewById(R.id.instrumentNameTextView);
        ImageButton botaoExcluir = findViewById(R.id.deleteChatButton);

        listaMensagens.setLayoutManager(new LinearLayoutManager(this));
        FirebaseUser cu = autenticacao.getCurrentUser();
        adaptadorMensagens = new AdaptadorMensagensChat(new ArrayList<>(), cu != null ? cu.getUid() : "");
        listaMensagens.setAdapter(adaptadorMensagens);

        botaoEnviar.setOnClickListener(v -> sendMessage());
        
        // Configurar botão de exclusão
        botaoExcluir.setOnClickListener(v -> mostrarDialogExclusao());

        carregarCabecalhoEMensagens();
    }

    private void carregarCabecalhoEMensagens() {
        if (idInstrumento != null) {
            // Carregar nome do instrumento do intent
            GerenciadorFirebase.obterInstrumentoPorId(idInstrumento)
                    .thenAccept(doc -> {
                        if (doc != null) {
                            nomeInstrumento = doc.getString("name");
                            runOnUiThread(() -> {
                                textoNomeInstrumento.setText(nomeInstrumento != null ? "Chat sobre: " + nomeInstrumento : "Chat");
                            });
                        }
                    });
        } else if (idChat != null) {
            // Se não tiver idInstrumento mas tiver idChat, carregar do chat
            GerenciadorFirebase.obterChatPorId(idChat)
                    .thenAccept(chatDoc -> {
                        if (chatDoc != null) {
                            String chatInstrumentId = chatDoc.getString("idInstrumento");
                            if (chatInstrumentId != null) {
                                idInstrumento = chatInstrumentId;
                                // Agora carregar o nome do instrumento
                                GerenciadorFirebase.obterInstrumentoPorId(chatInstrumentId)
                                        .thenAccept(instrumentDoc -> {
                                            if (instrumentDoc != null) {
                                                nomeInstrumento = instrumentDoc.getString("name");
                                                runOnUiThread(() -> {
                                                    textoNomeInstrumento.setText(nomeInstrumento != null ? "Chat sobre: " + nomeInstrumento : "Chat");
                                                });
                                            }
                                        });
                            }
                        }
                    });
        }
        
        if (idChat != null) {
            carregarMensagens();
        }
    }

    private void carregarMensagens() {
        GerenciadorFirebase.obterMensagensChat(idChat)
                .thenAccept(messages -> runOnUiThread(() -> {
                    adaptadorMensagens.atualizarMensagens(messages);
                    if (messages.size() > 0) {
                        listaMensagens.scrollToPosition(messages.size() - 1);
                    }
                }))
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar mensagens: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show());
                    return null;
                });
    }

    private void sendMessage() {
        String content = campoMensagem.getText().toString().trim();
        if (content.isEmpty()) return;

        FirebaseUser currentUser = autenticacao.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
            return;
        }
        campoMensagem.setText("");

        if (idChat == null) {
            // Criar chat no primeiro envio (mas antes verificar se já existe)
            String locatorId = currentUser.getUid();
            String resolvedOwnerId = idProprietario != null ? idProprietario : "";
            String nameForChat = nomeInstrumento;

            GerenciadorFirebase.encontrarChatExistente(idInstrumento, locatorId, resolvedOwnerId)
                    .thenAccept(existingId -> {
                        if (existingId != null) {
                            idChat = existingId;
                            actuallySendMessage(currentUser.getUid(), content);
                        } else {
                            GerenciadorFirebase.criarChat(idInstrumento, locatorId, resolvedOwnerId, nameForChat)
                                    .thenAccept(newChatId -> {
                                        idChat = newChatId;
                                        actuallySendMessage(currentUser.getUid(), content);
                                    })
                                    .exceptionally(throwable -> {
                                        Log.e(TAG, "Erro ao criar chat: " + throwable.getMessage(), throwable);
                                        runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show());
                                        return null;
                                    });
                        }
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Erro ao verificar chat existente: " + throwable.getMessage(), throwable);
                        // fallback: tentar criar
                        GerenciadorFirebase.criarChat(idInstrumento, locatorId, resolvedOwnerId, nameForChat)
                                .thenAccept(newChatId -> {
                                    idChat = newChatId;
                                    actuallySendMessage(currentUser.getUid(), content);
                                })
                                .exceptionally(err -> {
                                    Log.e(TAG, "Erro ao criar chat: " + err.getMessage(), err);
                                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show());
                                    return null;
                                });
                        return null;
                    });
        } else {
            actuallySendMessage(currentUser.getUid(), content);
        }
    }

    private void actuallySendMessage(String senderId, String content) {
        GerenciadorFirebase.enviarMensagem(idChat, senderId, content)
                .thenAccept(success -> {
                    if (success) {
                        carregarMensagens();
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show());
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao enviar mensagem: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show());
                    return null;
                });
    }
    
    /**
     * Mostra dialog de confirmação para exclusão da conversa
     */
    private void mostrarDialogExclusao() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir Conversa")
                .setMessage("Tem certeza que deseja excluir esta conversa?\n\nAs mensagens só serão apagadas definitivamente se ambos os usuários excluírem a conversa.")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    excluirConversa();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    /**
     * Exclui a conversa atual
     */
    private void excluirConversa() {
        String chatId = getIntent().getStringExtra("chat_id");
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        
        if (chatId == null || usuarioAtual == null) {
            Toast.makeText(this, "Erro: dados da conversa não encontrados", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Excluindo conversa " + chatId + " para usuário " + usuarioAtual.getUid());
        
        // Marcar como excluída para este usuário
        GerenciadorFirebase.marcarConversaComoExcluida(chatId, usuarioAtual.getUid())
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Toast.makeText(this, "Conversa excluída com sucesso", Toast.LENGTH_SHORT).show();
                            // Voltar para a lista de conversas
                            finish();
                        } else {
                            Toast.makeText(this, "Erro ao excluir conversa", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao excluir conversa: " + erro.getMessage(), erro);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao excluir conversa: " + erro.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
