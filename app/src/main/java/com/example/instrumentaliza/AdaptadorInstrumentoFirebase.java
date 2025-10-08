package com.example.instrumentaliza;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.instrumentaliza.models.FirebaseInstrument;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * AdaptadorInstrumentoFirebase - Adaptador para RecyclerView de instrumentos
 * 
 * Este adaptador é responsável por exibir a lista de instrumentos musicais
 * em um RecyclerView, convertendo DocumentSnapshot do Firestore em views
 * interativas para o usuário.
 * 
 * Funcionalidades principais:
 * - Exibição de dados do instrumento (nome, categoria, descrição, preço)
 * - Carregamento assíncrono de imagens com Glide
 * - Gerenciamento de favoritos com verificação em tempo real
 * - Callbacks para interações do usuário (clique, favoritar)
 * - Atualização dinâmica da lista de instrumentos
 * 
 * Características técnicas:
 * - Usa ViewHolder pattern para performance
 * - Integração com Firebase Firestore
 * - Carregamento de imagens otimizado
 * - Interface de callback para comunicação com Activity
 * - Tratamento de estados de favoritos
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorInstrumentoFirebase extends RecyclerView.Adapter<AdaptadorInstrumentoFirebase.ViewHolder> {
    
    // Constantes
    private static final String TAG = "AdaptadorInstrumentoFirebase";
    
    // Dados do adaptador
    private List<DocumentSnapshot> instrumentos;
    private final String idUsuarioAtual;
    private final OnInstrumentClickListener listener;

    /**
     * Interface para comunicação com a Activity
     * 
     * Define os callbacks que a Activity deve implementar para responder
     * às interações do usuário com os itens do RecyclerView.
     */
    public interface OnInstrumentClickListener {
        /**
         * Callback chamado quando o usuário clica no item do instrumento
         * @param instrumento DocumentSnapshot do instrumento clicado
         */
        void aoClicarInstrumento(DocumentSnapshot instrumento);
        
        /**
         * Callback chamado quando o usuário clica no botão de editar
         * @param instrumento DocumentSnapshot do instrumento a ser editado
         */
        void aoClicarEditar(DocumentSnapshot instrumento);
        
        /**
         * Callback chamado quando o usuário clica no botão de deletar
         * @param instrumento DocumentSnapshot do instrumento a ser deletado
         */
        void aoClicarDeletar(DocumentSnapshot instrumento);
        
        /**
         * Callback chamado quando o usuário clica no botão de favorito
         * @param instrumento DocumentSnapshot do instrumento
         * @param ehFavorito true se está nos favoritos (será removido), false caso contrário
         */
        void aoClicarFavorito(DocumentSnapshot instrumento, boolean ehFavorito);
    }

    /**
     * Construtor do adaptador
     * 
     * @param instrumentos Lista de instrumentos do Firestore
     * @param idUsuarioAtual ID do usuário logado para verificar favoritos
     * @param listener Interface de callback para interações
     */
    public AdaptadorInstrumentoFirebase(List<DocumentSnapshot> instrumentos, String idUsuarioAtual, OnInstrumentClickListener listener) {
        this.instrumentos = instrumentos;
        this.idUsuarioAtual = idUsuarioAtual;
        this.listener = listener;
    }

    /**
     * Cria uma nova instância do ViewHolder
     * 
     * Chamado pelo RecyclerView quando precisa de uma nova view para exibir um item.
     * Infla o layout item_instrument.xml e retorna um ViewHolder configurado.
     * 
     * @param parent ViewGroup pai (RecyclerView)
     * @param viewType Tipo da view (não usado neste adaptador)
     * @return ViewHolder configurado
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instrument, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Vincula os dados do instrumento às views do ViewHolder
     * 
     * Este é o método principal do adaptador, responsável por:
     * 1. Converter DocumentSnapshot em objeto FirebaseInstrument
     * 2. Carregar e exibir a imagem do instrumento
     * 3. Preencher todos os campos de texto
     * 4. Verificar e atualizar estado do favorito
     * 5. Configurar listeners para interações do usuário
     * 
     * @param holder ViewHolder que contém as views a serem preenchidas
     * @param position Posição do item na lista
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Obter dados do instrumento
        DocumentSnapshot documentoInstrumento = instrumentos.get(position);
        FirebaseInstrument instrumento = FirebaseInstrument.fromDocument(documentoInstrumento);
        
        Log.d("AdaptadorInstrumentoFirebase", "Exibindo: '" + instrumento.getName() + "', Categoria: '" + instrumento.getCategory() + "'");
        
        // Carregar imagem do instrumento com Glide
        if (instrumento.getImageUri() != null && !instrumento.getImageUri().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(instrumento.getImageUri()))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imagemInstrumento);
        } else {
            // Usar imagem padrão se não houver URI
            holder.imagemInstrumento.setImageResource(R.drawable.ic_music_note);
        }

        // Preencher campos de texto
        holder.textoNome.setText(instrumento.getName());
        holder.textoCategoria.setText(instrumento.getCategory());
        holder.textoDescricao.setText(instrumento.getDescription());
        
        // Configurar preço com nota
        Log.d(TAG, "Instrumento: " + instrumento.getName() + 
                   " - Nota média: " + instrumento.getNotaMedia() + 
                   " - Total avaliações: " + instrumento.getTotalAvaliacoes() +
                   " - Possui avaliações: " + instrumento.possuiAvaliacoes());
        
        String textoPreco;
        if (instrumento.possuiAvaliacoes()) {
            textoPreco = String.format(Locale.getDefault(), "R$ %.2f/dia\n%.1f (%d avaliações)", 
                instrumento.getPrice(), 
                instrumento.getNotaMedia(),
                instrumento.getTotalAvaliacoes());
        } else {
            textoPreco = String.format(Locale.getDefault(), "R$ %.2f/dia\nSem avaliações", instrumento.getPrice());
        }
        holder.textoPreco.setText(textoPreco);

        // Verificar se o instrumento está nos favoritos do usuário
        GerenciadorFirebase.ehFavorito(idUsuarioAtual, documentoInstrumento.getId())
                .thenAccept(ehFavorito -> {
                    // Atualizar UI na thread principal
                    holder.itemView.post(() -> {
                        atualizarBotaoFavorito(holder.botaoFavorito, ehFavorito);
                    });
                })
                .exceptionally(throwable -> {
                    Log.e("AdaptadorInstrumentoFirebase", "Erro ao verificar favorito: " + throwable.getMessage(), throwable);
                    return null;
                });

        // Configurar listener para clique no item (navegar para detalhes)
        holder.itemView.setOnClickListener(v -> listener.aoClicarInstrumento(documentoInstrumento));
        
        // Configurar listener para o botão de favoritar
        holder.botaoFavorito.setOnClickListener(v -> {
            // Verificar estado atual do favorito antes de alternar
            GerenciadorFirebase.ehFavorito(idUsuarioAtual, documentoInstrumento.getId())
                    .thenAccept(ehFavorito -> {
                        listener.aoClicarFavorito(documentoInstrumento, ehFavorito);
                    })
                    .exceptionally(throwable -> {
                        Log.e("AdaptadorInstrumentoFirebase", "Erro ao verificar favorito: " + throwable.getMessage(), throwable);
                        return null;
                    });
        });
    }
    
    /**
     * Atualiza a aparência do botão de favorito baseado no estado
     * 
     * @param botaoFavorito Botão a ser atualizado
     * @param ehFavorito true se está nos favoritos, false caso contrário
     */
    private void atualizarBotaoFavorito(ImageButton botaoFavorito, boolean ehFavorito) {
        if (ehFavorito) {
            // Instrumento está nos favoritos - mostrar ícone preenchido
            botaoFavorito.setImageResource(R.drawable.ic_favorite);
            botaoFavorito.setColorFilter(botaoFavorito.getContext().getResources().getColor(R.color.orange_primary));
        } else {
            // Instrumento não está nos favoritos - mostrar ícone vazio
            botaoFavorito.setImageResource(R.drawable.ic_favorite_border);
            botaoFavorito.setColorFilter(botaoFavorito.getContext().getResources().getColor(R.color.text_gray_light));
        }
    }

    /**
     * Retorna o número total de itens na lista
     * 
     * @return Número de instrumentos na lista
     */
    @Override
    public int getItemCount() {
        return instrumentos.size();
    }

    /**
     * Atualiza a lista de instrumentos e notifica o RecyclerView
     * 
     * Este método é chamado quando novos dados chegam do Firebase,
     * substituindo a lista atual e forçando uma atualização completa
     * de todos os itens visíveis.
     * 
     * @param novosInstrumentos Nova lista de instrumentos do Firestore
     */
    public void atualizarInstrumentos(List<DocumentSnapshot> novosInstrumentos) {
        Log.d("AdaptadorInstrumentoFirebase", "atualizarInstrumentos chamado com " + novosInstrumentos.size() + " instrumentos");
        
        this.instrumentos = novosInstrumentos;
        notifyDataSetChanged();
        
        Log.d("AdaptadorInstrumentoFirebase", "Adapter atualizado, getItemCount: " + getItemCount());
    }

    /**
     * ViewHolder para itens do RecyclerView de instrumentos
     * 
     * Implementa o padrão ViewHolder para melhorar a performance do RecyclerView.
     * Mantém referências para todas as views de um item, evitando chamadas
     * repetidas de findViewById().
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        
        // Componentes da interface do item
        ImageView imagemInstrumento;
        TextView textoNome;
        TextView textoCategoria;
        TextView textoDescricao;
        TextView textoPreco;
        ImageButton botaoFavorito;

        /**
         * Construtor do ViewHolder
         * 
         * Inicializa todas as referências para as views do layout item_instrument.xml
         * 
         * @param itemView View raiz do item do RecyclerView
         */
        ViewHolder(View itemView) {
            super(itemView);
            
            // Inicializar referências para as views
            imagemInstrumento = itemView.findViewById(R.id.instrumentImageView);
            textoNome = itemView.findViewById(R.id.nameTextView);
            textoCategoria = itemView.findViewById(R.id.categoryTextView);
            textoDescricao = itemView.findViewById(R.id.descriptionTextView);
            textoPreco = itemView.findViewById(R.id.priceTextView);
            botaoFavorito = itemView.findViewById(R.id.favoriteButton);
        }
    }
} 