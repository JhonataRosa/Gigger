package com.example.instrumentaliza;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instrumentaliza.models.FirebaseMessage;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * AdaptadorMensagensChat - Adaptador para RecyclerView de mensagens do chat
 * 
 * Este adaptador exibe as mensagens de uma conversa de chat, diferenciando
 * visualmente entre mensagens enviadas pelo usuário atual e mensagens
 * recebidas de outros usuários.
 * 
 * Funcionalidades principais:
 * - Exibição de mensagens com layouts diferentes (própria/outra)
 * - Formatação de timestamp das mensagens
 * - Diferenciação visual entre remetente e destinatário
 * - Atualização dinâmica da lista de mensagens
 * 
 * Características técnicas:
 * - Suporte a múltiplos tipos de view (getItemViewType)
 * - Layouts diferentes para mensagens próprias e de outros
 * - Formatação de data/hora localizada
 * - Integração com Firebase Firestore
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorMensagensChat extends RecyclerView.Adapter<AdaptadorMensagensChat.MessageViewHolder> {
    private static final String TAG = "AdaptadorMensagensChat";
    
    // Dados do adaptador
    private List<DocumentSnapshot> mensagens;
    private final String idUsuarioAtual;
    
    // Constantes para tipos de view
    private static final int TIPO_VIEW_MINHA_MENSAGEM = 1;
    private static final int TIPO_VIEW_OUTRA_MENSAGEM = 2;

    /**
     * Construtor do adaptador
     * 
     * @param mensagens Lista de mensagens do Firestore
     * @param idUsuarioAtual ID do usuário logado para diferenciar mensagens
     */
    public AdaptadorMensagensChat(List<DocumentSnapshot> mensagens, String idUsuarioAtual) {
        this.mensagens = mensagens;
        this.idUsuarioAtual = idUsuarioAtual;
    }

    /**
     * Cria ViewHolder baseado no tipo de mensagem
     * 
     * @param parent ViewGroup pai (RecyclerView)
     * @param viewType Tipo da view (minha mensagem ou outra mensagem)
     * @return ViewHolder configurado
     */
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        if (viewType == TIPO_VIEW_MINHA_MENSAGEM) {
            layoutRes = R.layout.item_message_my;
        } else {
            layoutRes = R.layout.item_message_other;
        }
        
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    /**
     * Vincula dados da mensagem às views do ViewHolder
     * 
     * @param holder ViewHolder que contém as views
     * @param position Posição da mensagem na lista
     */
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DocumentSnapshot documentoMensagem = mensagens.get(position);
        FirebaseMessage mensagem = FirebaseMessage.fromDocument(documentoMensagem);
        
        Log.d(TAG, "Exibindo mensagem: " + mensagem.getContent() + " de: " + mensagem.getSenderId());
        
        // Exibir conteúdo da mensagem
        holder.textoMensagem.setText(mensagem.getContent());
        
        // Formatar timestamp para exibição (apenas hora)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String tempo = sdf.format(mensagem.getTimestamp());
        holder.textoTempo.setText(tempo);
    }

    /**
     * Retorna o número total de mensagens
     * 
     * @return Número de mensagens na lista
     */
    @Override
    public int getItemCount() {
        return mensagens.size();
    }

    /**
     * Determina o tipo de view baseado no remetente da mensagem
     * 
     * @param position Posição da mensagem na lista
     * @return Tipo de view (minha mensagem ou outra mensagem)
     */
    @Override
    public int getItemViewType(int position) {
        DocumentSnapshot documentoMensagem = mensagens.get(position);
        String idRemetente = documentoMensagem.getString("senderId");
        
        if (idUsuarioAtual.equals(idRemetente)) {
            return TIPO_VIEW_MINHA_MENSAGEM;
        } else {
            return TIPO_VIEW_OUTRA_MENSAGEM;
        }
    }

    /**
     * Atualiza a lista de mensagens e notifica o RecyclerView
     * 
     * @param novasMensagens Nova lista de mensagens do Firestore
     */
    public void atualizarMensagens(List<DocumentSnapshot> novasMensagens) {
        Log.d(TAG, "atualizarMensagens chamado com " + novasMensagens.size() + " mensagens");
        
        this.mensagens = novasMensagens;
        notifyDataSetChanged();
        
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }

    /**
     * ViewHolder para itens de mensagem do RecyclerView
     * 
     * Implementa o padrão ViewHolder para melhorar a performance do RecyclerView.
     * Mantém referências para as views de uma mensagem, evitando chamadas
     * repetidas de findViewById().
     */
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        
        // Componentes da interface da mensagem
        TextView textoMensagem;
        TextView textoTempo;

        /**
         * Construtor do ViewHolder
         * 
         * Inicializa as referências para as views do layout de mensagem
         * 
         * @param itemView View raiz do item do RecyclerView
         */
        MessageViewHolder(View itemView) {
            super(itemView);
            textoMensagem = itemView.findViewById(R.id.messageTextView);
            textoTempo = itemView.findViewById(R.id.timeTextView);
        }
    }
}
