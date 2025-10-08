package com.example.instrumentaliza;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instrumentaliza.models.FirebaseSolicitacao;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * AdaptadorSolicitacoes - Adaptador para RecyclerView de solicitações
 * 
 * Este adaptador gerencia a exibição de uma lista de solicitações de reserva
 * em um RecyclerView, fornecendo uma interface limpa e organizada para
 * mostrar as informações relevantes de cada solicitação.
 * 
 * Funcionalidades principais:
 * - Exibição de informações do solicitante
 * - Período e preço da solicitação
 * - Status visual da solicitação
 * - Data de criação da solicitação
 * - Interface de clique para navegação
 * - Suporte a diferentes estados (PENDENTE, ACEITA, RECUSADA)
 * 
 * Características técnicas:
 * - ViewHolder pattern para performance
 * - Interface de callback para interações
 * - Formatação automática de datas e preços
 * - Cores dinâmicas baseadas no status
 * - Layout responsivo com ConstraintLayout
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorSolicitacoes extends RecyclerView.Adapter<AdaptadorSolicitacoes.ViewHolder> {
    
    private static final String TAG = "AdaptadorSolicitacoes";
    
    private List<DocumentSnapshot> solicitacoes;
    private final OnSolicitacaoClickListener listener;

    /**
     * Interface para manipular cliques nas solicitações
     */
    public interface OnSolicitacaoClickListener {
        void onSolicitacaoClick(DocumentSnapshot solicitacao);
    }

    /**
     * Construtor do adaptador
     * 
     * @param solicitacoes Lista de documentos de solicitações do Firestore
     * @param listener Listener para cliques nas solicitações
     */
    public AdaptadorSolicitacoes(List<DocumentSnapshot> solicitacoes, OnSolicitacaoClickListener listener) {
        this.solicitacoes = solicitacoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitacao, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot documentoSolicitacao = solicitacoes.get(position);
        FirebaseSolicitacao solicitacao = FirebaseSolicitacao.fromDocument(documentoSolicitacao);
        
        Log.d(TAG, "Exibindo solicitação: " + solicitacao.getId() + " - Status: " + solicitacao.getStatus());
        Log.d(TAG, "Status original: '" + solicitacao.getStatus() + "'");
        
        // Configurar informações do solicitante
        holder.textoSolicitanteNome.setText(solicitacao.getInstrumentoNome());
        holder.textoSolicitanteEmail.setText(solicitacao.getSolicitanteEmail());
        
        // Configurar período
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String periodo = formatoData.format(solicitacao.getDataInicio()) + " a " + 
                        formatoData.format(solicitacao.getDataFim());
        holder.textoPeriodo.setText(periodo);
        
        // Configurar preço total
        holder.textoPrecoTotal.setText(String.format(Locale.getDefault(), "R$ %.2f", solicitacao.getPrecoTotal()));
        
        // Configurar data da solicitação
        if (solicitacao.getDataCriacao() != null) {
            String dataSolicitacao = "Solicitado em " + formatoData.format(solicitacao.getDataCriacao());
            holder.textoDataSolicitacao.setText(dataSolicitacao);
        }
        
        // Configurar status - garantir que sempre tenha um valor
        String status = solicitacao.getStatus();
        Log.d(TAG, "Status obtido: '" + status + "'");
        
        // Se status for null, vazio ou não for um dos valores esperados, usar PENDENTE
        if (status == null || status.isEmpty() || 
            (!status.equals("PENDENTE") && !status.equals("ACEITA") && !status.equals("RECUSADA"))) {
            status = "PENDENTE";
            Log.d(TAG, "Status inválido/nulo, definindo como PENDENTE");
        }
        
        Log.d(TAG, "Status final: '" + status + "'");
        holder.textoStatus.setText(status);
        holder.textoStatus.setVisibility(View.VISIBLE);
        
        // Configurar cor do status
        int corStatus;
        switch (status) {
            case "PENDENTE":
                corStatus = android.graphics.Color.WHITE; // Branco para contraste com fundo laranja
                break;
            case "ACEITA":
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.status_accepted);
                break;
            case "RECUSADA":
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.status_rejected);
                break;
            default:
                corStatus = android.graphics.Color.WHITE; // Branco por padrão
                break;
        }
        holder.textoStatus.setTextColor(corStatus);
        
        // Carregar imagem do instrumento
        carregarImagemInstrumento(solicitacao.getInstrumentoId(), holder);
        
        // Configurar listener para clicar no item
        holder.itemView.setOnClickListener(v -> listener.onSolicitacaoClick(documentoSolicitacao));
    }

    @Override
    public int getItemCount() {
        return solicitacoes.size();
    }

    /**
     * Atualiza a lista de solicitações
     * 
     * @param novasSolicitacoes Nova lista de solicitações
     */
    public void atualizarSolicitacoes(List<DocumentSnapshot> novasSolicitacoes) {
        this.solicitacoes = novasSolicitacoes;
        notifyDataSetChanged();
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }
    
    /**
     * Carrega a imagem do instrumento
     * 
     * @param instrumentoId ID do instrumento
     * @param holder ViewHolder para atualizar a imagem
     */
    private void carregarImagemInstrumento(String instrumentoId, ViewHolder holder) {
        if (holder.imagemAvatar == null) {
            Log.w(TAG, "ImageView não encontrada no layout");
            return;
        }
        
        if (instrumentoId == null || instrumentoId.trim().isEmpty()) {
            Log.w(TAG, "ID do instrumento não fornecido");
            holder.imagemAvatar.setImageResource(R.drawable.ic_instrument_placeholder);
            return;
        }
        
        // Buscar instrumento no Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("instruments")
                .document(instrumentoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("imageUri");
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            // Carregar imagem do Firebase Storage ou URL
                            Log.d(TAG, "Carregando imagem do instrumento: " + imageUrl);
                            carregarImagemDeUrl(imageUrl, holder.imagemAvatar);
                        } else {
                            // Usar imagem padrão se não houver URL
                            Log.d(TAG, "Usando imagem padrão para instrumento sem URL");
                            holder.imagemAvatar.setImageResource(R.drawable.ic_instrument_placeholder);
                        }
                    } else {
                        Log.w(TAG, "Instrumento não encontrado: " + instrumentoId);
                        holder.imagemAvatar.setImageResource(R.drawable.ic_instrument_placeholder);
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar instrumento: " + erro.getMessage(), erro);
                    holder.imagemAvatar.setImageResource(R.drawable.ic_instrument_placeholder);
                });
    }
    
    /**
     * Carrega uma imagem de uma URL usando Glide
     * 
     * @param imageUrl URL da imagem
     * @param imageView ImageView onde exibir a imagem
     */
    private void carregarImagemDeUrl(String imageUrl, ImageView imageView) {
        try {
            Log.d(TAG, "Carregando imagem com Glide: " + imageUrl);
            
            com.bumptech.glide.Glide.with(imageView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_instrument_placeholder)
                    .error(R.drawable.ic_instrument_placeholder)
                    .centerCrop()
                    .into(imageView);
                    
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar imagem com Glide: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.ic_instrument_placeholder);
        }
    }

    /**
     * ViewHolder para os itens da lista de solicitações
     * 
     * Mantém referências para todos os componentes da interface
     * de um item de solicitação, otimizando a performance do RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagemAvatar;
        TextView textoSolicitanteNome;
        TextView textoSolicitanteEmail;
        TextView textoStatus;
        TextView textoPeriodo;
        TextView textoPrecoTotal;
        TextView textoDataSolicitacao;

        ViewHolder(View itemView) {
            super(itemView);
            imagemAvatar = itemView.findViewById(R.id.solicitanteAvatarImageView);
            textoSolicitanteNome = itemView.findViewById(R.id.solicitanteNomeTextView);
            textoSolicitanteEmail = itemView.findViewById(R.id.solicitanteEmailTextView);
            textoStatus = itemView.findViewById(R.id.statusBadge);
            textoPeriodo = itemView.findViewById(R.id.periodoTextView);
            textoPrecoTotal = itemView.findViewById(R.id.precoTotalTextView);
            textoDataSolicitacao = itemView.findViewById(R.id.dataSolicitacaoTextView);
            
        }
    }
}
