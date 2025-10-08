package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * FragmentChatTab - Fragment para exibir lista de chats por tipo
 * 
 * Este fragment exibe chats separados por tipo:
 * - Meus Anúncios: chats onde o usuário é proprietário do instrumento
 * - Meus Interesses: chats onde o usuário é interessado no instrumento
 */
public class FragmentChatTab extends Fragment implements AdaptadorListaChat.OnChatClickListener {
    
    private static final String TAG = "FragmentChatTab";
    private static final String ARG_TIPO_CHAT = "tipo_chat";
    
    public static final String TIPO_MEUS_ANUNCIOS = "meus_anuncios";
    public static final String TIPO_MEUS_INTERESSES = "meus_interesses";
    
    private RecyclerView listaChats;
    private View layoutEstadoVazio;
    private AdaptadorListaChat adaptadorListaChat;
    private String tipoChat;
    private String idUsuarioAtual;
    private FirebaseAuth autenticacao;
    
    /**
     * Cria nova instância do fragment
     */
    public static FragmentChatTab newInstance(String tipoChat) {
        FragmentChatTab fragment = new FragmentChatTab();
        Bundle args = new Bundle();
        args.putString(ARG_TIPO_CHAT, tipoChat);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tipoChat = getArguments().getString(ARG_TIPO_CHAT);
        }
        
        autenticacao = FirebaseAuth.getInstance();
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        if (usuarioAtual != null) {
            idUsuarioAtual = usuarioAtual.getUid();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_tab, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar componentes
        listaChats = view.findViewById(R.id.chatsRecyclerView);
        layoutEstadoVazio = view.findViewById(R.id.emptyStateCard);
        
        Log.d(TAG, "Elementos inicializados - listaChats: " + (listaChats != null) + ", layoutEstadoVazio: " + (layoutEstadoVazio != null));
        
        // Configurar RecyclerView
        listaChats.setLayoutManager(new LinearLayoutManager(getContext()));
        adaptadorListaChat = new AdaptadorListaChat(new ArrayList<>(), this, idUsuarioAtual);
        listaChats.setAdapter(adaptadorListaChat);
        
        // Configurar textos do estado vazio
        configurarEstadoVazio();
        
        // Carregar chats
        carregarChats();
    }
    
    /**
     * Configura os textos do estado vazio baseado no tipo de chat
     */
    private void configurarEstadoVazio() {
        // Os textos serão configurados via findViewById no layout
        // Por enquanto, usar textos padrão
    }
    
    /**
     * Carrega os chats baseado no tipo
     */
    public void carregarChats() {
        Log.d(TAG, "Carregando chats do tipo: " + tipoChat + " para usuário: " + idUsuarioAtual);
        
        GerenciadorFirebase.obterChatsUsuario(idUsuarioAtual)
                .thenAccept(todosChats -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Filtrar chats baseado no tipo
                            List<DocumentSnapshot> chatsFiltrados = filtrarChatsPorTipo(todosChats);
                            
                            Log.d(TAG, "Resultado da filtragem: " + chatsFiltrados.size() + " chats");
                            
                            if (chatsFiltrados.isEmpty()) {
                                Log.d(TAG, "Mostrando estado vazio");
                                mostrarEstadoVazio();
                            } else {
                                Log.d(TAG, "Mostrando lista com " + chatsFiltrados.size() + " chats");
                                esconderEstadoVazio();
                                adaptadorListaChat.atualizarChats(chatsFiltrados);
                            }
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar chats: " + throwable.getMessage(), throwable);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Erro ao carregar conversas", Toast.LENGTH_SHORT).show();
                            mostrarEstadoVazio();
                        });
                    }
                    return null;
                });
    }
    
    /**
     * Filtra os chats baseado no tipo (meus anúncios ou meus interesses)
     */
    private List<DocumentSnapshot> filtrarChatsPorTipo(List<DocumentSnapshot> todosChats) {
        List<DocumentSnapshot> chatsFiltrados = new ArrayList<>();
        
        Log.d(TAG, "Filtrando " + todosChats.size() + " chats para tipo: " + tipoChat + " (usuário: " + idUsuarioAtual + ")");
        
        for (DocumentSnapshot chat : todosChats) {
            String ownerId = chat.getString("ownerId");
            String locatorId = chat.getString("locatorId");
            
            Log.d(TAG, "Chat " + chat.getId() + " - ownerId: " + ownerId + ", locatorId: " + locatorId);
            
            boolean isMeuAnuncio = idUsuarioAtual.equals(ownerId);
            boolean isMeuInteresse = idUsuarioAtual.equals(locatorId);
            
            Log.d(TAG, "  - É meu anúncio: " + isMeuAnuncio + ", É meu interesse: " + isMeuInteresse);
            
            if (TIPO_MEUS_ANUNCIOS.equals(tipoChat) && isMeuAnuncio) {
                chatsFiltrados.add(chat);
                Log.d(TAG, "  -> ADICIONADO como meu anúncio");
            } else if (TIPO_MEUS_INTERESSES.equals(tipoChat) && isMeuInteresse) {
                chatsFiltrados.add(chat);
                Log.d(TAG, "  -> ADICIONADO como meu interesse");
            } else {
                Log.d(TAG, "  -> IGNORADO");
            }
        }
        
        Log.d(TAG, "Chats filtrados para " + tipoChat + ": " + chatsFiltrados.size() + " de " + todosChats.size());
        return chatsFiltrados;
    }
    
    private void mostrarEstadoVazio() {
        Log.d(TAG, "Mostrando estado vazio - layoutEstadoVazio: " + (layoutEstadoVazio != null) + ", listaChats: " + (listaChats != null));
        if (layoutEstadoVazio != null) layoutEstadoVazio.setVisibility(View.VISIBLE);
        if (listaChats != null) listaChats.setVisibility(View.GONE);
    }
    
    private void esconderEstadoVazio() {
        Log.d(TAG, "Escondendo estado vazio - layoutEstadoVazio: " + (layoutEstadoVazio != null) + ", listaChats: " + (listaChats != null));
        if (layoutEstadoVazio != null) layoutEstadoVazio.setVisibility(View.GONE);
        if (listaChats != null) listaChats.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onChatClick(DocumentSnapshot chat) {
        // Implementar navegação para chat
        if (getActivity() instanceof AtividadeListaChat) {
            ((AtividadeListaChat) getActivity()).navegarParaChat(chat);
        }
    }
    
    @Override
    public void onChatDelete(DocumentSnapshot chat) {
        // Implementar exclusão de chat
        if (getActivity() instanceof AtividadeListaChat) {
            ((AtividadeListaChat) getActivity()).excluirChat(chat);
        }
    }
}
