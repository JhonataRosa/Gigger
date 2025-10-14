package com.example.instrumentaliza;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * AdaptadorReservas - Adaptador para RecyclerView de reservas
 * 
 * Este adaptador gerencia a exibição de uma lista de reservas ativas
 * em um RecyclerView, fornecendo uma interface limpa e organizada para
 * mostrar as informações relevantes de cada reserva.
 * 
 * Funcionalidades principais:
 * - Exibição de informações da reserva
 * - Nome do instrumento e período
 * - Status e preço da reserva
 * - Data de criação da reserva
 * - Interface de clique para navegação
 * - Suporte a diferentes estados (CONFIRMED, PENDING, CANCELLED)
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
public class AdaptadorReservas extends RecyclerView.Adapter<AdaptadorReservas.ViewHolder> {
    
    private static final String TAG = "AdaptadorReservas";
    
    private List<DocumentSnapshot> reservas;
    private final OnReservaClickListener listener;
    private String tipoReserva; // "meus_instrumentos" ou "meus_interesses"

    /**
     * Interface para manipular cliques nas reservas
     */
    public interface OnReservaClickListener {
        void onReservaClick(DocumentSnapshot reserva);
        void onAvaliarReserva(DocumentSnapshot reserva);
    }

    /**
     * Construtor do adaptador
     * 
     * @param reservas Lista de documentos de reservas do Firestore
     * @param listener Listener para cliques nas reservas
     */
    public AdaptadorReservas(List<DocumentSnapshot> reservas, OnReservaClickListener listener) {
        this.reservas = reservas;
        this.listener = listener;
    }
    
    /**
     * Construtor do adaptador com tipo de reserva
     * 
     * @param reservas Lista de documentos de reservas do Firestore
     * @param listener Listener para cliques nas reservas
     * @param tipoReserva Tipo da reserva ("meus_instrumentos" ou "meus_interesses")
     *                   - "meus_instrumentos": mostra "AVALIAR LOCATÁRIO" (avalia quem alugou)
     *                   - "meus_interesses": mostra "AVALIAR" (avalia o instrumento)
     */
    public AdaptadorReservas(List<DocumentSnapshot> reservas, OnReservaClickListener listener, String tipoReserva) {
        this.reservas = reservas;
        this.listener = listener;
        this.tipoReserva = tipoReserva;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot documentReserva = reservas.get(position);
        
        Log.d(TAG, "Exibindo reserva: " + documentReserva.getId() + " - Status: " + documentReserva.getString("status"));
        
        // Configurar informações da reserva
        String instrumentoId = documentReserva.getString("instrumentId");
        String status = documentReserva.getString("status");
        Double precoTotal = documentReserva.getDouble("totalPrice");
        
        // Configurar período
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String periodo = "";
        if (documentReserva.get("startDate") != null && documentReserva.get("endDate") != null) {
            periodo = formatoData.format(documentReserva.getTimestamp("startDate").toDate()) + " a " + 
                     formatoData.format(documentReserva.getTimestamp("endDate").toDate());
        }
        holder.textoPeriodo.setText(periodo);
        
        // Configurar preço total
        if (precoTotal != null) {
            holder.textoPrecoTotal.setText(String.format(Locale.getDefault(), "R$ %.2f", precoTotal));
        } else {
            holder.textoPrecoTotal.setText("R$ 0,00");
        }
        
        // Configurar status em português
        String statusTraduzido = traduzirStatus(status);
        holder.textoStatus.setText(statusTraduzido);
        
        // Configurar cor do status
        int corStatus;
        switch (status != null ? status : "PENDENTE") {
            case "CONFIRMADA":
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.status_accepted);
                break;
            case "PENDENTE":
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.status_pending);
                break;
            case "CANCELADA":
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.status_rejected);
                break;
            default:
                corStatus = holder.itemView.getContext().getResources().getColor(R.color.text_gray_light);
                break;
        }
        holder.textoStatus.setTextColor(corStatus);
        
        // Configurar data da reserva
        if (documentReserva.get("createdAt") != null) {
            String dataReserva = "Reservado em " + formatoData.format(documentReserva.getTimestamp("createdAt").toDate());
            holder.textoDataReserva.setText(dataReserva);
        }
        
        // Configurar botão de avaliação
        if ("CONFIRMED".equals(status)) {
            verificarEAvaliarBotaoAvaliacao(documentReserva, holder);
        } else {
            holder.botaoAvaliar.setVisibility(View.GONE);
        }
        
        // Buscar nome do instrumento por ID
        if (instrumentoId != null) {
            buscarNomeInstrumento(instrumentoId, holder);
        } else {
            holder.textoNomeInstrumento.setText("Instrumento: N/A");
        }
        
        // Configurar listener para clicar no item
        holder.itemView.setOnClickListener(v -> listener.onReservaClick(documentReserva));
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }
    
    /**
     * Traduz o status do inglês para português
     * 
     * @param status Status em inglês
     * @return Status traduzido em português
     */
    private String traduzirStatus(String status) {
        if (status == null) return "PENDENTE";
        
        switch (status.toUpperCase()) {
            case "CONFIRMED":
                return "CONFIRMADO";
            case "PENDING":
                return "PENDENTE";
            case "CANCELLED":
                return "CANCELADO";
            case "COMPLETED":
                return "CONCLUÍDO";
            default:
                return status;
        }
    }
    
    /**
     * Busca o nome e imagem do instrumento pelo ID
     * 
     * @param instrumentoId ID do instrumento
     * @param holder ViewHolder para atualizar o nome e imagem
     */
    private void buscarNomeInstrumento(String instrumentoId, ViewHolder holder) {
        // Primeiro mostrar loading
        holder.textoNomeInstrumento.setText("Carregando...");
        
        // Buscar instrumento no Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("instruments")
                .document(instrumentoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nomeInstrumento = documentSnapshot.getString("name");
                        if (nomeInstrumento != null && !nomeInstrumento.isEmpty()) {
                            holder.textoNomeInstrumento.setText(nomeInstrumento);
                        } else {
                            holder.textoNomeInstrumento.setText("Instrumento sem nome");
                        }
                        
                        // Carregar imagem do instrumento
                        carregarImagemInstrumento(documentSnapshot, holder);
                        
                    } else {
                        holder.textoNomeInstrumento.setText("Instrumento não encontrado");
                        // Manter imagem padrão se instrumento não encontrado
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar nome do instrumento: " + erro.getMessage(), erro);
                    holder.textoNomeInstrumento.setText("Erro ao carregar nome");
                    // Manter imagem padrão em caso de erro
                });
    }
    
    /**
     * Carrega a imagem do instrumento
     * 
     * @param instrumentoDoc Documento do instrumento do Firestore
     * @param holder ViewHolder para atualizar a imagem
     */
    private void carregarImagemInstrumento(com.google.firebase.firestore.DocumentSnapshot instrumentoDoc, ViewHolder holder) {
        if (holder.imagemInstrumento == null) {
            Log.w(TAG, "ImageView não encontrada no layout");
            return;
        }
        
        String imageUrl = instrumentoDoc.getString("imageUri");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            // Carregar imagem do Firebase Storage ou URL
            Log.d(TAG, "Carregando imagem do instrumento: " + imageUrl);
            carregarImagemDeUrl(imageUrl, holder.imagemInstrumento);
        } else {
            // Usar imagem padrão se não houver URL
            Log.d(TAG, "Usando imagem padrão para instrumento sem URL");
            holder.imagemInstrumento.setImageResource(R.drawable.ic_instrument_placeholder);
        }
    }
    
    /**
     * Carrega uma imagem de uma URL usando Glide
     * 
     * @param imageUrl URL da imagem
     * @param imageView ImageView onde exibir a imagem
     */
    private void carregarImagemDeUrl(String imageUrl, android.widget.ImageView imageView) {
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
     * Verifica se a reserva já foi avaliada e configura o botão adequadamente
     * 
     * @param documentReserva Documento da reserva
     * @param holder ViewHolder da reserva
     */
    private void verificarEAvaliarBotaoAvaliacao(DocumentSnapshot documentReserva, ViewHolder holder) {
        String reservaId = documentReserva.getId();
        String tipoAvaliacao = "meus_instrumentos".equals(tipoReserva) ? "usuario" : "instrumento";
        
        Log.d(TAG, "Verificando se reserva " + reservaId + " já foi avaliada (" + tipoAvaliacao + ")");
        
        GerenciadorFirebase.verificarSeReservaFoiAvaliada(reservaId, tipoAvaliacao)
                .thenAccept(jaAvaliada -> {
                    if (holder.itemView.getContext() instanceof android.app.Activity) {
                        ((android.app.Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                            if (jaAvaliada) {
                                Log.d(TAG, "Reserva já avaliada, ocultando botão");
                                holder.botaoAvaliar.setVisibility(View.GONE);
                            } else {
                                Log.d(TAG, "Reserva não avaliada, mostrando botão");
                                holder.botaoAvaliar.setVisibility(View.VISIBLE);
                                
                                // Definir texto do botão baseado no tipo de reserva
                                if ("meus_instrumentos".equals(tipoReserva)) {
                                    holder.botaoAvaliar.setText("AVALIAR LOCATÁRIO");
                                } else {
                                    holder.botaoAvaliar.setText("AVALIAR");
                                }
                                
                                holder.botaoAvaliar.setOnClickListener(v -> {
                                    if (listener != null) {
                                        listener.onAvaliarReserva(documentReserva);
                                    }
                                });
                            }
                        });
                    }
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao verificar avaliação: " + erro.getMessage(), erro);
                    // Em caso de erro, mostrar botão por segurança
                    if (holder.itemView.getContext() instanceof android.app.Activity) {
                        ((android.app.Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                            holder.botaoAvaliar.setVisibility(View.VISIBLE);
                            if ("meus_instrumentos".equals(tipoReserva)) {
                                holder.botaoAvaliar.setText("AVALIAR LOCATÁRIO");
                            } else {
                                holder.botaoAvaliar.setText("AVALIAR");
                            }
                            holder.botaoAvaliar.setOnClickListener(v -> {
                                if (listener != null) {
                                    listener.onAvaliarReserva(documentReserva);
                                }
                            });
                        });
                    }
                    return null;
                });
    }

    /**
     * Atualiza a lista de reservas
     * 
     * @param novasReservas Nova lista de reservas
     */
    public void atualizarReservas(List<DocumentSnapshot> novasReservas) {
        this.reservas = novasReservas;
        notifyDataSetChanged();
        Log.d(TAG, "Adapter atualizado, getItemCount: " + getItemCount());
    }

    /**
     * ViewHolder para os itens da lista de reservas
     * 
     * Mantém referências para todos os componentes da interface
     * de um item de reserva, otimizando a performance do RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textoNomeInstrumento;
        TextView textoPeriodo;
        TextView textoStatus;
        TextView textoPrecoTotal;
        TextView textoDataReserva;
        MaterialButton botaoAvaliar;
        android.widget.ImageView imagemInstrumento;

        ViewHolder(View itemView) {
            super(itemView);
            textoNomeInstrumento = itemView.findViewById(R.id.instrumentNameTextView);
            textoPeriodo = itemView.findViewById(R.id.periodTextView);
            textoStatus = itemView.findViewById(R.id.statusTextView);
            textoPrecoTotal = itemView.findViewById(R.id.totalPriceTextView);
            textoDataReserva = itemView.findViewById(R.id.reservationDateTextView);
            botaoAvaliar = itemView.findViewById(R.id.avaliarButton);
            imagemInstrumento = itemView.findViewById(R.id.instrumentIconImageView);
        }
    }
}
