package com.example.instrumentaliza;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AtividadeReserva extends AppCompatActivity {
    private static final String TAG = "AtividadeReserva";
    private static final String STATUS_PENDENTE = "PENDENTE";
    private static final String STATUS_CONFIRMADA = "CONFIRMADA";
    private static final String STATUS_CANCELADA = "CANCELADA";
    private static final String STATUS_CONCLUIDA = "CONCLUIDA";

    private long instrumentId;
    private long userId;
    private double instrumentPrice;
    private Date dataInicio;
    private Date dataFim;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private GerenciadorSessao gerenciadorSessao;

    private TextView textoNomeInstrumento;
    private TextView textoPrecoInstrumento;
    private TextInputEditText campoDataInicio;
    private TextInputEditText campoDataFim;
    private TextView textoPrecoTotal;
    private Button botaoConfirmarReserva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        gerenciadorSessao = new GerenciadorSessao(this);
        userId = gerenciadorSessao.getUserId();
        instrumentId = getIntent().getLongExtra("instrumentId", -1);

        if (instrumentId == -1) {
            Toast.makeText(this, getString(R.string.error_instrument_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarViews();
        carregarDetalhesInstrumento();
        configurarSeletoresData();
        configurarBotaoConfirmar();
    }

    private void inicializarViews() {
        textoNomeInstrumento = findViewById(R.id.instrumentNameTextView);
        textoPrecoInstrumento = findViewById(R.id.instrumentPriceTextView);
        campoDataInicio = findViewById(R.id.startDateEditText);
        campoDataFim = findViewById(R.id.endDateEditText);
        textoPrecoTotal = findViewById(R.id.totalPriceTextView);
        botaoConfirmarReserva = findViewById(R.id.confirmReservationButton);
    }

    private void carregarDetalhesInstrumento() {
        executorService.execute(() -> {
            try {
                Instrumento instrumento = AppDatabase.getInstance(this)
                        .instrumentoDao()
                        .obterPorId(instrumentId);

                if (instrumento == null) {
                    throw new IllegalStateException("Instrumento não encontrado");
                }

                instrumentPrice = instrumento.getPreco();

                runOnUiThread(() -> {
                    textoNomeInstrumento.setText(instrumento.getNome());
                    textoPrecoInstrumento.setText(String.format(Locale.getDefault(),
                            "R$ %.2f/dia", instrumento.getPreco()));
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar detalhes do instrumento: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void configurarSeletoresData() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        campoDataInicio.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        dataInicio = calendar.getTime();
                        campoDataInicio.setText(dateFormat.format(dataInicio));
                        calcularPrecoTotal();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        campoDataFim.setOnClickListener(v -> {
            if (dataInicio == null) {
                Toast.makeText(this, getString(R.string.select_start_date), 
                        Toast.LENGTH_SHORT).show();
                return;
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        dataFim = calendar.getTime();
                        
                        if (dataFim.before(dataInicio)) {
                            Toast.makeText(this, getString(R.string.error_generic), 
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        campoDataFim.setText(dateFormat.format(dataFim));
                        calcularPrecoTotal();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(dataInicio.getTime());
            datePickerDialog.show();
        });
    }

    private void calcularPrecoTotal() {
        if (dataInicio != null && dataFim != null) {
            long diffInMillis = dataFim.getTime() - dataInicio.getTime();
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
            double totalPrice = diffInDays * instrumentPrice;
            textoPrecoTotal.setText(String.format(Locale.getDefault(), 
                    "Total: R$ %.2f", totalPrice));
        }
    }

    private void configurarBotaoConfirmar() {
        botaoConfirmarReserva.setOnClickListener(v -> {
            if (dataInicio == null || dataFim == null) {
                Toast.makeText(this, getString(R.string.apply_filter), 
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar se há reservas sobrepostas
            executorService.execute(() -> {
                try {
                    List<Reserva> reservasSobrepostas = AppDatabase.getInstance(this)
                            .reservaDao()
                            .obterReservasSobrepostas(instrumentId, dataInicio, dataFim);

                    if (!reservasSobrepostas.isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, 
                                    getString(R.string.error_generic), 
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // Calcular preço total
                    long diffInMillis = dataFim.getTime() - dataInicio.getTime();
                    long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                    double totalPrice = diffInDays * instrumentPrice;

                    // Criar nova reserva
                    Reserva reserva = new Reserva(
                            userId,
                            instrumentId,
                            dataInicio,
                            dataFim,
                            totalPrice,
                            STATUS_PENDENTE
                    );

                    long reservationId = AppDatabase.getInstance(this)
                            .reservaDao()
                            .inserir(reserva);

                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.reservation_success), 
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao criar reserva: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.reservation_failed) + ": " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
} 