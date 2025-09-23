package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AtividadeMinhasReservas - Tela de minhas reservas
 * 
 * Esta tela exibe todas as reservas feitas pelo usuário logado, permitindo
 * visualizar o histórico de aluguéis e gerenciar reservas ativas.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de reservas do usuário
 * - Diferenciação por status (Pendente, Confirmada, Cancelada, Concluída)
 * - Formatação de datas e preços
 * - Estado vazio quando não há reservas
 * - Navegação de volta para tela anterior
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - ExecutorService para operações assíncronas
 * - Adaptador customizado para reservas
 * - GerenciadorSessao para dados do usuário
 * - Tratamento de estados vazios
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeMinhasReservas extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "MinhasReservas";
    private static final String STATUS_PENDENTE = "PENDING";
    private static final String STATUS_CONFIRMADA = "CONFIRMED";
    private static final String STATUS_CANCELADA = "CANCELLED";
    private static final String STATUS_CONCLUIDA = "COMPLETED";

    // Componentes da interface
    private RecyclerView listaReservas;
    private TextView textoEstadoVazio;
    
    // Gerenciamento de dados e threads
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private GerenciadorSessao gerenciadorSessao;
    private AdaptadorReservas adaptadorReservas;
    private List<DetalhesReserva> reservas;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de minhas reservas, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do gerenciador de sessão
     * - Configuração do RecyclerView e adaptador
     * - Carregamento das reservas do usuário
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        gerenciadorSessao = new GerenciadorSessao(this);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.reservations_title));
        }

        // Inicializar views
        listaReservas = findViewById(R.id.reservationsRecyclerView);
        textoEstadoVazio = findViewById(R.id.emptyStateTextView);

        // Configurar RecyclerView
        reservas = new ArrayList<>();
        adaptadorReservas = new AdaptadorReservas(reservas);
        listaReservas.setLayoutManager(new LinearLayoutManager(this));
        listaReservas.setAdapter(adaptadorReservas);

        // Carregar reservas
        carregarReservas();
    }

    private void carregarReservas() {
        executorService.execute(() -> {
            try {
                List<Reserva> reservasUsuario = AppDatabase.getInstance(this)
                        .reservaDao()
                        .obterPorIdUsuario(gerenciadorSessao.getUserId());

                List<DetalhesReserva> reservasWithDetails = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                for (Reserva reserva : reservasUsuario) {
                    Instrumento instrumento = AppDatabase.getInstance(this)
                            .instrumentoDao()
                            .obterPorId(reserva.getIdInstrumento());

                    if (instrumento != null) {
                        DetalhesReserva details = new DetalhesReserva(
                                reserva,
                                instrumento.getNome(),
                                dateFormat.format(reserva.getDataInicio()),
                                dateFormat.format(reserva.getDataFim())
                        );
                        reservasWithDetails.add(details);
                    }
                }

                runOnUiThread(() -> {
                    reservas.clear();
                    reservas.addAll(reservasWithDetails);
                    if (reservas.isEmpty()) {
                        mostrarEstadoVazio();
                    } else {
                        esconderEstadoVazio();
                        adaptadorReservas.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar reservas: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mostrarEstadoVazio();
                });
            }
        });
    }

    private void mostrarEstadoVazio() {
        textoEstadoVazio.setVisibility(View.VISIBLE);
        listaReservas.setVisibility(View.GONE);
    }

    private void esconderEstadoVazio() {
        textoEstadoVazio.setVisibility(View.GONE);
        listaReservas.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private static class DetalhesReserva {
        private final Reserva reserva;
        private final String instrumentName;
        private final String startDate;
        private final String endDate;

        public DetalhesReserva(Reserva reserva, String instrumentName, 
                String startDate, String endDate) {
            this.reserva = reserva;
            this.instrumentName = instrumentName;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Reserva getReservation() {
            return reserva;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }
    }

    private class AdaptadorReservas extends RecyclerView.Adapter<AdaptadorReservas.ViewHolder> {
        private final List<DetalhesReserva> reservas;

        public AdaptadorReservas(List<DetalhesReserva> reservas) {
            this.reservas = reservas;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_reservation, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DetalhesReserva details = reservas.get(position);
            Reserva reserva = details.getReservation();

            holder.textoNomeInstrumento.setText(details.getInstrumentName());
            holder.textoPeriodo.setText(String.format("%s - %s", 
                    details.getStartDate(), details.getEndDate()));
            holder.textoPrecoTotal.setText(String.format(Locale.getDefault(), 
                    "Total: R$ %.2f", reserva.getPrecoTotal()));
            holder.textoStatus.setText(obterTextoStatus(reserva.getStatus()));
        }

        @Override
        public int getItemCount() {
            return reservas.size();
        }

        private String obterTextoStatus(String status) {
            switch (status) {
                case STATUS_PENDENTE:
                    return getString(R.string.status_pending);
                case STATUS_CONFIRMADA:
                    return getString(R.string.status_confirmed);
                case STATUS_CANCELADA:
                    return getString(R.string.status_cancelled);
                case STATUS_CONCLUIDA:
                    return getString(R.string.status_completed);
                default:
                    return status;
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textoNomeInstrumento;
            TextView textoPeriodo;
            TextView textoPrecoTotal;
            TextView textoStatus;

            ViewHolder(View itemView) {
                super(itemView);
                textoNomeInstrumento = itemView.findViewById(R.id.instrumentNameTextView);
                textoPeriodo = itemView.findViewById(R.id.periodTextView);
                textoPrecoTotal = itemView.findViewById(R.id.totalPriceTextView);
                textoStatus = itemView.findViewById(R.id.statusTextView);
            }
        }
    }
} 