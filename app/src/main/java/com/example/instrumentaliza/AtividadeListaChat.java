package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private AdaptadorChatTabs adaptadorTabs;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;
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
        usuarioAtual = autenticacao.getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.messages_title));
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        FirebaseUser user = autenticacao.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        idUsuarioAtual = user.getUid();

        // Configurar ViewPager2 e TabLayout
        adaptadorTabs = new AdaptadorChatTabs(this);
        viewPager.setAdapter(adaptadorTabs);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Meus Anúncios");
                    break;
                case 1:
                    tab.setText("Meus Interesses");
                    break;
            }
        }).attach();
    }

    /**
     * Navega para o chat selecionado
     */
    public void navegarParaChat(DocumentSnapshot chat) {
        String chatId = chat.getId();
        String instrumentId = chat.getString("instrumentId");
        Intent intent = new Intent(this, AtividadeChat.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("instrument_id", instrumentId);
        startActivity(intent);
    }
    
    /**
     * Exclui um chat
     */
    public void excluirChat(DocumentSnapshot chat) {
        Log.d(TAG, "Exclusão solicitada para chat: " + chat.getId());
        
        // Mostrar dialog de confirmação
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir Conversa")
                .setMessage("Tem certeza que deseja excluir esta conversa?\n\nAs mensagens só serão apagadas definitivamente se ambos os usuários excluírem a conversa.")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    excluirConversa(chat);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    /**
     * Exclui uma conversa do ponto de vista do usuário atual
     */
    private void excluirConversa(DocumentSnapshot chat) {
        String chatId = chat.getId();
        String userId = usuarioAtual.getUid();
        
        Log.d(TAG, "Excluindo conversa " + chatId + " para usuário " + userId);
        
        // Marcar como excluída para este usuário
        GerenciadorFirebase.marcarConversaComoExcluida(chatId, userId)
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Toast.makeText(this, "Conversa excluída com sucesso", Toast.LENGTH_SHORT).show();
                            // Recarregar as abas
                            recarregarAbas();
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
    
    /**
     * Recarrega as abas após exclusão
     */
    private void recarregarAbas() {
        // Notificar os fragments para recarregar
        if (adaptadorTabs != null) {
            for (int i = 0; i < adaptadorTabs.getItemCount(); i++) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + i);
                if (fragment instanceof FragmentChatTab) {
                    ((FragmentChatTab) fragment).carregarChats();
                }
            }
        }
    }

    @Override
    public void onChatClick(DocumentSnapshot chat) {
        navegarParaChat(chat);
    }
    
    @Override
    public void onChatDelete(DocumentSnapshot chat) {
        excluirChat(chat);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
