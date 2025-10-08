package com.example.instrumentaliza;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instrumentaliza.models.FirebaseChat;
import com.example.instrumentaliza.models.FirebaseInstrument;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * AdaptadorListaChat - Adaptador para RecyclerView da lista de conversas
 * 
 * Este adaptador exibe a lista de conversas de chat do usuário, mostrando
 * informações sobre cada conversa incluindo o instrumento relacionado,
 * timestamp da última mensagem e papel do usuário na conversa.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de conversas ativas
 * - Carregamento dinâmico de nomes de instrumentos
 * - Diferenciação entre anúncios próprios e interesses
 * - Formatação de timestamps de última mensagem
 * - Callback para navegação para conversa específica
 * 
 * Características técnicas:
 * - Carregamento assíncrono de dados do Firestore
 * - Cache de nomes de instrumentos no documento do chat
 * - Fallback para IDs quando nome não está disponível
 * - Interface de callback para comunicação com Activity
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorListaChat extends RecyclerView.Adapter<AdaptadorListaChat.ChatViewHolder> {
    private static final String TAG = "AdaptadorListaChat";
    
    // Dados do adaptador
    private List<DocumentSnapshot> chats;
    private final OnChatClickListener listener;
    private final String idUsuarioAtual;
    private final FirebaseFirestore firestore;

    /**
     * Interface para comunicação com a Activity
     * 
     * Define o callback que a Activity deve implementar para responder
     * ao clique em uma conversa da lista.
     */
    public interface OnChatClickListener {
        /**
         * Callback chamado quando o usuário clica em uma conversa
         * @param chat DocumentSnapshot da conversa clicada
         */
        void onChatClick(DocumentSnapshot chat);
        
        /**
         * Callback chamado quando o usuário quer excluir uma conversa
         * @param chat DocumentSnapshot da conversa a ser excluída
         */
        void onChatDelete(DocumentSnapshot chat);
    }

    /**
     * Construtor do adaptador
     * 
     * @param chats Lista de conversas do Firestore
     * @param listener Interface de callback para cliques
     * @param idUsuarioAtual ID do usuário logado para determinar papel
     */
    public AdaptadorListaChat(List<DocumentSnapshot> chats, OnChatClickListener listener, String idUsuarioAtual) {
        this.chats = chats;
        this.listener = listener;
        this.idUsuarioAtual = idUsuarioAtual;
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Cria uma nova instância do ViewHolder
     * 
     * @param parent ViewGroup pai (RecyclerView)
     * @param viewType Tipo da view (não usado neste adaptador)
     * @return ViewHolder configurado
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * Vincula dados da conversa às views do ViewHolder
     * 
     * @param holder ViewHolder que contém as views
     * @param position Posição da conversa na lista
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        DocumentSnapshot chatDoc = chats.get(position);
        FirebaseChat chat = FirebaseChat.fromDocument(chatDoc);

        Log.d(TAG, "Vinculando chat " + position + " - instrumentId: " + chat.getInstrumentId());

        // Primeiro, tentar usar o nome salvo no chat (cache)
        String nomeExibicao = null;
        if (chatDoc.contains("instrumentName")) {
            nomeExibicao = chatDoc.getString("instrumentName");
            Log.d(TAG, "Nome do instrumento encontrado no chat: " + nomeExibicao);
        }

        // Se não tiver nome em cache, buscar do instrumento
        if (nomeExibicao == null || nomeExibicao.isEmpty()) {
            Log.d(TAG, "Nome não encontrado no chat, buscando do instrumento...");
            carregarNomeInstrumento(chat.getInstrumentId(), holder.textoNomeInstrumento);
        } else {
            Log.d(TAG, "Usando nome salvo no chat: " + nomeExibicao);
            holder.textoNomeInstrumento.setText(nomeExibicao);
        }

        // Formatar timestamp da última mensagem
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        if (chat.getLastMessageAt() != null) {
            holder.textoUltimaMensagem.setText(sdf.format(chat.getLastMessageAt()));
        } else {
            holder.textoUltimaMensagem.setText("-");
        }

        // Determinar papel do usuário na conversa
        String textoPapel = idUsuarioAtual != null && idUsuarioAtual.equals(chat.getOwnerId())
                ? "Seu anúncio"
                : "Você - interessado";
        holder.textoPapelBadge.setText(textoPapel);

        // Configurar listeners
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chatDoc));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onChatDelete(chatDoc);
            return true; // Consumir o evento
        });
    }

    /**
     * Carrega o nome do instrumento do Firestore
     * 
     * Busca o nome do instrumento no Firestore e atualiza o TextView.
     * Em caso de erro ou instrumento não encontrado, exibe um fallback
     * com parte do ID do instrumento.
     * 
     * @param instrumentId ID do instrumento a ser buscado
     * @param textView TextView que será atualizado com o nome
     */
    private void carregarNomeInstrumento(String instrumentId, TextView textView) {
        if (instrumentId == null || instrumentId.isEmpty()) {
            textView.setText("Instrumento");
            return;
        }

        Log.d(TAG, "Carregando nome do instrumento para ID: " + instrumentId);

        firestore.collection("instruments").document(instrumentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        // Tentar diferentes campos de nome
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("nome");
                        }
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("instrumentName");
                        }
                        
                        Log.d(TAG, "Nome do instrumento encontrado: " + name);
                        if (name != null && !name.isEmpty()) {
                            textView.setText(name);
                        } else {
                            textView.setText("Instrumento #" + instrumentId.substring(0, Math.min(8, instrumentId.length())));
                        }
                    } else {
                        Log.w(TAG, "Documento do instrumento não encontrado: " + instrumentId);
                        textView.setText("Instrumento #" + instrumentId.substring(0, Math.min(8, instrumentId.length())));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar nome do instrumento " + instrumentId + ": " + e.getMessage());
                    textView.setText("Instrumento #" + instrumentId.substring(0, Math.min(8, instrumentId.length())));
                });
    }

    /**
     * Retorna o número total de conversas
     * 
     * @return Número de conversas na lista
     */
    @Override
    public int getItemCount() {
        return chats.size();
    }

    /**
     * Atualiza a lista de conversas e notifica o RecyclerView
     * 
     * @param novosChats Nova lista de conversas do Firestore
     */
    public void atualizarChats(List<DocumentSnapshot> novosChats) {
        this.chats = novosChats;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para itens de conversa do RecyclerView
     * 
     * Implementa o padrão ViewHolder para melhorar a performance do RecyclerView.
     * Mantém referências para as views de uma conversa, evitando chamadas
     * repetidas de findViewById().
     */
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        
        // Componentes da interface da conversa
        TextView textoNomeInstrumento;
        TextView textoUltimaMensagem;
        TextView textoPapelBadge;

        /**
         * Construtor do ViewHolder
         * 
         * Inicializa as referências para as views do layout item_chat.xml
         * 
         * @param itemView View raiz do item do RecyclerView
         */
        ChatViewHolder(View itemView) {
            super(itemView);
            textoNomeInstrumento = itemView.findViewById(R.id.instrumentNameTextView);
            textoUltimaMensagem = itemView.findViewById(R.id.lastMessageTimeTextView);
            textoPapelBadge = itemView.findViewById(R.id.roleBadgeTextView);
        }
    }
}
