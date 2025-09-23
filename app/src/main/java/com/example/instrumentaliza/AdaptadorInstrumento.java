package com.example.instrumentaliza;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;
import java.util.Locale;

public class AdaptadorInstrumento extends RecyclerView.Adapter<AdaptadorInstrumento.ViewHolder> {
    private List<Instrumento> instrumentos;
    private final long idUsuarioAtual;
    private final OnInstrumentClickListener listener;

    public interface OnInstrumentClickListener {
        void onInstrumentClick(Instrumento instrumento);
        void onEditClick(Instrumento instrumento);
        void onDeleteClick(Instrumento instrumento);
    }

    public AdaptadorInstrumento(List<Instrumento> instrumentos, long idUsuarioAtual, OnInstrumentClickListener listener) {
        this.instrumentos = instrumentos;
        this.idUsuarioAtual = idUsuarioAtual;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instrument, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Instrumento instrumento = instrumentos.get(position);
        Log.d("AdaptadorInstrumento", "Exibindo: '" + instrumento.getNome() + "', Categoria: '" + instrumento.getCategoria() + "'");
        
        // Carregar imagem com Glide
        if (instrumento.getUriImagem() != null && !instrumento.getUriImagem().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(instrumento.getUriImagem()))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imagemInstrumento);
        } else {
            holder.imagemInstrumento.setImageResource(R.drawable.ic_music_note);
        }

        holder.textoNome.setText(instrumento.getNome());
        holder.textoCategoria.setText(instrumento.getCategoria());
        holder.textoDescricao.setText(instrumento.getDescricao());
        holder.textoPreco.setText(String.format(Locale.getDefault(), "R$ %.2f/dia", instrumento.getPreco()));

        // Configurar listener para clicar no item
        holder.itemView.setOnClickListener(v -> listener.onInstrumentClick(instrumento));
    }

    @Override
    public int getItemCount() {
        return instrumentos.size();
    }

    public void atualizarInstrumentos(List<Instrumento> novosInstrumentos) {
        this.instrumentos = novosInstrumentos;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagemInstrumento;
        TextView textoNome;
        TextView textoCategoria;
        TextView textoDescricao;
        TextView textoPreco;

        ViewHolder(View itemView) {
            super(itemView);
            imagemInstrumento = itemView.findViewById(R.id.instrumentImageView);
            textoNome = itemView.findViewById(R.id.nameTextView);
            textoCategoria = itemView.findViewById(R.id.categoryTextView);
            textoDescricao = itemView.findViewById(R.id.descriptionTextView);
            textoPreco = itemView.findViewById(R.id.priceTextView);
        }
    }
} 