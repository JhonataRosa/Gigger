package com.example.instrumentaliza;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.instrumentaliza.models.FirebaseInstrument;

import java.util.List;
import java.util.Locale;

public class AdaptadorMeusInstrumentos extends RecyclerView.Adapter<AdaptadorMeusInstrumentos.ViewHolder> {
    private static final String TAG = "AdaptadorMeusInstrumentos";
    
    private List<DocumentSnapshot> instrumentos;
    private final OnMyInstrumentClickListener listener;

    public interface OnMyInstrumentClickListener {
        void onInstrumentClick(DocumentSnapshot instrumento);
        void onEditClick(DocumentSnapshot instrumento);
        void onDeleteClick(DocumentSnapshot instrumento);
    }

    public AdaptadorMeusInstrumentos(List<DocumentSnapshot> instrumentos, OnMyInstrumentClickListener listener) {
        this.instrumentos = instrumentos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_instrument, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot documentoInstrumento = instrumentos.get(position);
        FirebaseInstrument instrumento = FirebaseInstrument.fromDocument(documentoInstrumento);
        
        Log.d(TAG, "Exibindo meu instrumento: '" + instrumento.getName() + "', Categoria: '" + instrumento.getCategory() + "'");
        
        // Carregar imagem com Glide
        if (instrumento.getImageUri() != null && !instrumento.getImageUri().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(instrumento.getImageUri()))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imagemInstrumento);
        } else {
            holder.imagemInstrumento.setImageResource(R.drawable.ic_music_note);
        }

        holder.textoNome.setText(instrumento.getName());
        holder.textoCategoria.setText(instrumento.getCategory());
        holder.textoDescricao.setText(instrumento.getDescription());
        holder.textoPreco.setText(String.format(Locale.getDefault(), "R$ %.2f/dia", instrumento.getPrice()));

        // Configurar listener para clicar no item
        holder.itemView.setOnClickListener(v -> listener.onInstrumentClick(documentoInstrumento));
        
        // Configurar listener para o botão de editar
        holder.botaoEditar.setOnClickListener(v -> listener.onEditClick(documentoInstrumento));
        
        // Configurar listener para o botão de deletar
        holder.botaoDeletar.setOnClickListener(v -> listener.onDeleteClick(documentoInstrumento));
    }

    @Override
    public int getItemCount() {
        return instrumentos.size();
    }

    public void atualizarInstrumentos(List<DocumentSnapshot> novosInstrumentos) {
        Log.d(TAG, "atualizarInstrumentos chamado com " + novosInstrumentos.size() + " instrumentos");
        
        this.instrumentos = novosInstrumentos;
        notifyDataSetChanged();
        
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagemInstrumento;
        TextView textoNome;
        TextView textoCategoria;
        TextView textoDescricao;
        TextView textoPreco;
        ImageButton botaoEditar;
        ImageButton botaoDeletar;

        ViewHolder(View itemView) {
            super(itemView);
            imagemInstrumento = itemView.findViewById(R.id.instrumentImageView);
            textoNome = itemView.findViewById(R.id.nameTextView);
            textoCategoria = itemView.findViewById(R.id.categoryTextView);
            textoDescricao = itemView.findViewById(R.id.descriptionTextView);
            textoPreco = itemView.findViewById(R.id.priceTextView);
            botaoEditar = itemView.findViewById(R.id.editButton);
            botaoDeletar = itemView.findViewById(R.id.deleteButton);
        }
    }
}
