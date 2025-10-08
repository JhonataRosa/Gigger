package com.example.instrumentaliza;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * AdaptadorAvaliacoes - Adaptador para lista de avaliações
 * 
 * Este adaptador gerencia a exibição de uma lista de avaliações em um RecyclerView,
 * mostrando informações como nota, comentário, instrumento avaliado e dados do locatário.
 * 
 * Características:
 * - Exibição de nota com RatingBar
 * - Comentário do avaliador
 * - Nome do instrumento
 * - Nome do locatário
 * - Data da avaliação
 * - Layout responsivo com MaterialCardView
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorAvaliacoes extends RecyclerView.Adapter<AdaptadorAvaliacoes.ViewHolder> {
    
    // Constantes
    private static final String TAG = "AdaptadorAvaliacoes";
    
    // Dados
    private List<DocumentSnapshot> avaliacoes;
    
    /**
     * Construtor principal
     * 
     * @param avaliacoes Lista de documentos de avaliações
     */
    public AdaptadorAvaliacoes(List<DocumentSnapshot> avaliacoes) {
        this.avaliacoes = avaliacoes;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avaliacao, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot documentoAvaliacao = avaliacoes.get(position);
        
        // Extrair dados da avaliação
        String instrumentoNome = documentoAvaliacao.getString("instrumentoNome");
        String locatarioNome = documentoAvaliacao.getString("locatarioNome");
        Double notaDouble = documentoAvaliacao.getDouble("nota");
        String comentario = documentoAvaliacao.getString("comentario");
        com.google.firebase.Timestamp dataAvaliacao = documentoAvaliacao.getTimestamp("dataAvaliacao");
        
        float nota = notaDouble != null ? notaDouble.floatValue() : 0.0f;
        
        Log.d(TAG, "Exibindo avaliação: " + instrumentoNome + " - Nota: " + nota);
        
        // Configurar dados na interface
        holder.textoNomeInstrumento.setText(instrumentoNome != null ? instrumentoNome : "Instrumento não informado");
        holder.textoLocatario.setText("Avaliado por: " + (locatarioNome != null ? locatarioNome : "Usuário"));
        holder.ratingBar.setRating(nota);
        holder.textoNota.setText(String.format(Locale.getDefault(), "%.1f", (float) nota));
        
        // Configurar comentário
        if (comentario != null && !comentario.trim().isEmpty()) {
            holder.textoComentario.setText(comentario.trim());
            holder.textoComentario.setVisibility(View.VISIBLE);
        } else {
            holder.textoComentario.setVisibility(View.GONE);
        }
        
        // Configurar data
        if (dataAvaliacao != null) {
            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dataFormatada = formato.format(dataAvaliacao.toDate());
            holder.textoData.setText("Avaliado em " + dataFormatada);
        } else {
            holder.textoData.setText("Data não informada");
        }
        
        // Configurar cor da nota baseada no valor
        configurarCorNota(holder, nota);
    }
    
    /**
     * Configura a cor da nota baseada no valor
     * 
     * @param holder ViewHolder da avaliação
     * @param nota Nota de 1 a 5 (suporta meias estrelas)
     */
    private void configurarCorNota(ViewHolder holder, float nota) {
        int corNota;
        
        if (nota >= 4.0f) {
            corNota = holder.itemView.getContext().getResources().getColor(R.color.status_accepted, null);
        } else if (nota >= 3.0f) {
            corNota = holder.itemView.getContext().getResources().getColor(R.color.orange_primary, null);
        } else {
            corNota = holder.itemView.getContext().getResources().getColor(R.color.status_pending, null);
        }
        
        holder.textoNota.setTextColor(corNota);
        holder.ratingBar.setProgressTintList(android.content.res.ColorStateList.valueOf(corNota));
    }
    
    @Override
    public int getItemCount() {
        return avaliacoes.size();
    }
    
    /**
     * Atualiza a lista de avaliações
     * 
     * @param novasAvaliacoes Nova lista de avaliações
     */
    public void atualizarAvaliacoes(List<DocumentSnapshot> novasAvaliacoes) {
        Log.d(TAG, "atualizarAvaliacoes chamado com " + novasAvaliacoes.size() + " avaliações");
        
        this.avaliacoes = novasAvaliacoes;
        notifyDataSetChanged();
        
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }
    
    /**
     * ViewHolder para os itens de avaliação
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textoNomeInstrumento;
        TextView textoLocatario;
        RatingBar ratingBar;
        TextView textoNota;
        TextView textoComentario;
        TextView textoData;
        
        ViewHolder(View itemView) {
            super(itemView);
            textoNomeInstrumento = itemView.findViewById(R.id.textoNomeInstrumento);
            textoLocatario = itemView.findViewById(R.id.textoLocatario);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            textoNota = itemView.findViewById(R.id.textoNota);
            textoComentario = itemView.findViewById(R.id.textoComentario);
            textoData = itemView.findViewById(R.id.textoData);
        }
    }
}
