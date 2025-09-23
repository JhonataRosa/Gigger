package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

/**
 * AtividadeListaChat - Tela de lista de conversas
 * 
 * Esta tela exibe todas as conversas de chat do usuário logado, permitindo
 * navegação para conversas específicas. Mostra informações sobre cada
 * conversa incluindo o instrumento relacionado e timestamp da última mensagem.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de conversas ativas
 * - Navegação para conversas específicas
 * - Estado vazio quando não há conversas
 * - Atualização automática da lista
 * - Diferenciação entre anúncios próprios e interesses
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para conversas
 * - Carregamento assíncrono do Firebase
 * - Interface de callback para navegação
 * - Tratamento de estados vazios
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeListaChat extends AppCompatActivity implements AdaptadorListaChat.OnChatClickListener {
    
    // Constantes
    private static final String TAG = "ListaChat";

    // Componentes da interface
    private RecyclerView listaChats;
    private View layoutEstadoVazio;
    private AdaptadorListaChat adaptadorListaChat;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    private String idUsuarioAtual;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de lista de conversas, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração do RecyclerView e adaptador
     * - Carregamento das conversas do usuário
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.messages_title));
        }

        listaChats = findViewById(R.id.chatsRecyclerView);
        layoutEstadoVazio = findViewById(R.id.emptyStateLayout);

        listaChats.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        idUsuarioAtual = user.getUid();

        adaptadorListaChat = new AdaptadorListaChat(new ArrayList<>(), this, idUsuarioAtual);
        listaChats.setAdapter(adaptadorListaChat);

        carregarChatsUsuario();
    }

    private void carregarChatsUsuario() {
        Log.d(TAG, "Carregando chats do usuário: " + idUsuarioAtual);

        GerenciadorFirebase.obterChatsUsuario(idUsuarioAtual)
                .thenAccept(chats -> runOnUiThread(() -> {
                    if (chats.isEmpty()) {
                        mostrarEstadoVazio();
                    } else {
                        esconderEstadoVazio();
                        adaptadorListaChat.atualizarChats(chats);
                    }
                }))
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar chats: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_LONG).show();
                        mostrarEstadoVazio();
                    });
                    return null;
                });
    }

    private void mostrarEstadoVazio() {
        layoutEstadoVazio.setVisibility(View.VISIBLE);
    }

    private void esconderEstadoVazio() {
        layoutEstadoVazio.setVisibility(View.GONE);
    }

    @Override
    public void onChatClick(DocumentSnapshot chat) {
        String chatId = chat.getId();
        String instrumentId = chat.getString("instrumentId");
        Intent intent = new Intent(this, AtividadeChat.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("instrument_id", instrumentId);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
