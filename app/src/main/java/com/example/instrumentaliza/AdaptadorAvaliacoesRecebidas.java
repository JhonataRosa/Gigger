package com.example.instrumentaliza;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para exibir avaliações recebidas pelo usuário
 */
public class AdaptadorAvaliacoesRecebidas extends RecyclerView.Adapter<AdaptadorAvaliacoesRecebidas.ViewHolder> {
    
    private static final String TAG = "AdaptadorAvaliacoesRecebidas";
    
    private List<DocumentSnapshot> avaliacoes;
    private SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    
    public AdaptadorAvaliacoesRecebidas(List<DocumentSnapshot> avaliacoes) {
        this.avaliacoes = avaliacoes;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avaliacao_recebida, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot avaliacao = avaliacoes.get(position);
        
        Log.d(TAG, "Exibindo avaliação: " + avaliacao.getId());
        
        // Nome do avaliador
        String avaliadorNome = avaliacao.getString("avaliadorNome");
        holder.textoAvaliadorNome.setText(avaliadorNome != null ? avaliadorNome : "Avaliador Anônimo");
        
        // Nome do instrumento
        String instrumentoNome = avaliacao.getString("instrumentoNome");
        if (instrumentoNome != null && !instrumentoNome.trim().isEmpty()) {
            holder.textoInstrumentoNome.setText("Instrumento: " + instrumentoNome);
            holder.textoInstrumentoNome.setVisibility(View.VISIBLE);
        } else {
            holder.textoInstrumentoNome.setVisibility(View.GONE);
        }
        
        // Nota
        Double notaDouble = avaliacao.getDouble("nota");
        if (notaDouble != null) {
            float nota = notaDouble.floatValue();
            holder.ratingBar.setRating(nota);
            holder.ratingBar.setVisibility(View.VISIBLE);
        } else {
            holder.ratingBar.setVisibility(View.GONE);
        }
        
        // Comentário
        String comentario = avaliacao.getString("comentario");
        if (comentario != null && !comentario.trim().isEmpty()) {
            holder.textoComentario.setText(comentario);
            holder.textoComentario.setVisibility(View.VISIBLE);
        } else {
            holder.textoComentario.setText("Sem comentário");
            holder.textoComentario.setVisibility(View.VISIBLE);
        }
        
        // Data da avaliação
        if (avaliacao.getTimestamp("dataAvaliacao") != null) {
            Date dataAvaliacao = avaliacao.getTimestamp("dataAvaliacao").toDate();
            String dataFormatada = formatarDataRelativa(dataAvaliacao);
            holder.textoDataAvaliacao.setText(dataFormatada);
            holder.textoDataAvaliacao.setVisibility(View.VISIBLE);
        } else {
            holder.textoDataAvaliacao.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return avaliacoes.size();
    }
    
    /**
     * Atualiza a lista de avaliações
     */
    public void atualizarAvaliacoes(List<DocumentSnapshot> novasAvaliacoes) {
        this.avaliacoes = novasAvaliacoes;
        notifyDataSetChanged();
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }
    
    /**
     * Formata a data de forma relativa (ex: "há 2 dias")
     */
    private String formatarDataRelativa(Date data) {
        try {
            long diffEmMs = System.currentTimeMillis() - data.getTime();
            long diffEmDias = diffEmMs / (1000 * 60 * 60 * 24);
            
            if (diffEmDias == 0) {
                return "hoje";
            } else if (diffEmDias == 1) {
                return "ontem";
            } else if (diffEmDias < 7) {
                return "há " + diffEmDias + " dias";
            } else {
                return formatoData.format(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao formatar data: " + e.getMessage(), e);
            return formatoData.format(data);
        }
    }
    
    /**
     * ViewHolder para os itens de avaliação
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textoAvaliadorNome;
        TextView textoInstrumentoNome;
        RatingBar ratingBar;
        TextView textoComentario;
        TextView textoDataAvaliacao;
        
        ViewHolder(View itemView) {
            super(itemView);
            textoAvaliadorNome = itemView.findViewById(R.id.textoAvaliadorNome);
            textoInstrumentoNome = itemView.findViewById(R.id.textoInstrumentoNome);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            textoComentario = itemView.findViewById(R.id.textoComentario);
            textoDataAvaliacao = itemView.findViewById(R.id.textoDataAvaliacao);
        }
    }
}
