package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.instrumentaliza.models.FirebaseAvaliacaoUsuario;
import com.example.instrumentaliza.models.FirebaseReservation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeAvaliarUsuario - Tela para avaliar um usuário (locatário)
 * 
 * Esta tela permite que um proprietário avalie um locatário após uma reserva,
 * incluindo nota de 1-5 estrelas (com meias estrelas) e comentário opcional.
 * 
 * Funcionalidades:
 * - RatingBar com suporte a meias estrelas (0.5, 1.0, 1.5, etc.)
 * - Campo de comentário opcional
 * - Validação de dados
 * - Salvamento no Firebase
 * - Navegação de volta após sucesso
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeAvaliarUsuario extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "AvaliarUsuario";
    
    // Componentes da interface
    private Toolbar toolbar;
    private TextView textoNomeInstrumento;
    private TextView textoNomeLocatario;
    private TextView textoPeriodo;
    private RatingBar ratingBar;
    private TextView textoNotaSelecionada;
    private TextInputEditText campoComentario;
    private MaterialButton botaoAvaliar;
    
    // Dados da avaliação
    private String reservaId;
    private String instrumentoId;
    private String instrumentoNome;
    private String locatarioId;
    private String locatarioNome;
    private FirebaseReservation reserva;
    private FirebaseUser usuarioAtual;
    private boolean avaliando = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avaliar_usuario);
        
        Log.d(TAG, "=== ATIVIDADE AVALIAR USUÁRIO INICIADA ===");
        
        // Inicializar componentes
        inicializarComponentes();
        
        // Configurar toolbar
        configurarToolbar();
        
        // Carregar dados da reserva
        carregarDadosReserva();
    }
    
    /**
     * Inicializa todos os componentes da interface
     */
    private void inicializarComponentes() {
        Log.d(TAG, "Inicializando componentes");
        
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        
        // TextViews
        textoNomeInstrumento = findViewById(R.id.textoNomeInstrumento);
        textoNomeLocatario = findViewById(R.id.textoNomeLocatario);
        textoPeriodo = findViewById(R.id.textoPeriodo);
        textoNotaSelecionada = findViewById(R.id.textoNotaSelecionada);
        
        // RatingBar
        ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setStepSize(0.5f); // Permitir meias estrelas
        ratingBar.setRating(0f); // Iniciar com 0 estrelas
        ratingBar.setIsIndicator(false); // Permitir interação do usuário
        
        // Campo de comentário
        campoComentario = findViewById(R.id.campoComentario);
        
        // Botão
        botaoAvaliar = findViewById(R.id.botaoAvaliar);
        
        // Usuário atual
        usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        
        if (usuarioAtual == null) {
            Log.e(TAG, "Usuário não autenticado");
            Toast.makeText(this, "Erro: Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Usuário atual: " + usuarioAtual.getUid());
        
        // Configurar listeners
        configurarListeners();
    }
    
    /**
     * Configura a toolbar
     */
    private void configurarToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Avaliar Locatário");
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    /**
     * Configura os listeners dos componentes
     */
    private void configurarListeners() {
        // Listener do RatingBar
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                textoNotaSelecionada.setText(String.format(Locale.getDefault(), "%.1f", rating));
            }
        });
        
        // Listener do botão avaliar
        botaoAvaliar.setOnClickListener(v -> enviarAvaliacao());
    }
    
    /**
     * Carrega os dados da reserva a partir dos extras da Intent
     */
    private void carregarDadosReserva() {
        Log.d(TAG, "Carregando dados da reserva");
        
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent é null");
            Toast.makeText(this, "Erro: Dados não fornecidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Obter dados básicos
        reservaId = intent.getStringExtra("reserva_id");
        instrumentoId = intent.getStringExtra("instrumento_id");
        locatarioId = intent.getStringExtra("locatario_id");
        
        Log.d(TAG, "Dados recebidos - Reserva: " + reservaId + 
              ", Instrumento: " + instrumentoId + ", Locatário: " + locatarioId);
        
        if (reservaId == null || instrumentoId == null || locatarioId == null) {
            Log.e(TAG, "Dados obrigatórios não fornecidos");
            Toast.makeText(this, "Erro: Dados obrigatórios não fornecidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Carregar dados da reserva do Firebase
        GerenciadorFirebase.obterReservaPorId(reservaId)
                .thenAccept(reservaDoc -> {
                    runOnUiThread(() -> {
                        if (!reservaDoc.exists()) {
                            Log.e(TAG, "Reserva não encontrada");
                            Toast.makeText(this, "Erro: Reserva não encontrada", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        
                        try {
                            reserva = FirebaseReservation.fromDocument(reservaDoc);
                            if (reserva == null) {
                                Log.e(TAG, "Erro ao converter reserva");
                                Toast.makeText(this, "Erro: Dados da reserva inválidos", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            
                            // Carregar dados do instrumento e locatário
                            carregarDadosInstrumentoELocatario();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao carregar reserva: " + e.getMessage(), e);
                            Toast.makeText(this, "Erro ao carregar dados da reserva", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar reserva: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao carregar dados da reserva", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return null;
                });
    }
    
    /**
     * Carrega os dados do instrumento e do locatário
     */
    private void carregarDadosInstrumentoELocatario() {
        Log.d(TAG, "Carregando dados do instrumento e locatário");
        
        // Carregar dados do instrumento
        GerenciadorFirebase.obterInstrumentoPorId(instrumentoId)
                .thenCompose(instrumentoDoc -> {
                    if (instrumentoDoc.exists()) {
                        instrumentoNome = instrumentoDoc.getString("name");
                        if (instrumentoNome == null || instrumentoNome.trim().isEmpty()) {
                            instrumentoNome = instrumentoDoc.getString("nome");
                        }
                        if (instrumentoNome == null || instrumentoNome.trim().isEmpty()) {
                            instrumentoNome = instrumentoDoc.getString("instrumentName");
                        }
                        if (instrumentoNome == null || instrumentoNome.trim().isEmpty()) {
                            instrumentoNome = "Instrumento";
                        }
                    } else {
                        instrumentoNome = "Instrumento";
                    }
                    
                    // Carregar dados do locatário
                    return GerenciadorFirebase.obterUsuarioPorId(locatarioId);
                })
                .thenAccept(locatarioDoc -> {
                    runOnUiThread(() -> {
                        if (locatarioDoc != null && locatarioDoc.exists()) {
                            locatarioNome = locatarioDoc.getString("name");
                            if (locatarioNome == null || locatarioNome.trim().isEmpty()) {
                                locatarioNome = locatarioDoc.getString("nome");
                            }
                            if (locatarioNome == null || locatarioNome.trim().isEmpty()) {
                                locatarioNome = "Usuário";
                            }
                        } else {
                            locatarioNome = "Usuário";
                        }
                        
                        // Atualizar interface
                        atualizarInterface();
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar dados: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        // Usar valores padrão
                        if (instrumentoNome == null) instrumentoNome = "Instrumento";
                        if (locatarioNome == null) locatarioNome = "Usuário";
                        atualizarInterface();
                    });
                    return null;
                });
    }
    
    /**
     * Atualiza a interface com os dados carregados
     */
    private void atualizarInterface() {
        Log.d(TAG, "Atualizando interface");
        
        // Atualizar textos
        textoNomeInstrumento.setText(instrumentoNome);
        textoNomeLocatario.setText(locatarioNome);
        
        // Formatar período
        if (reserva != null) {
            String periodo = formatarPeriodo(reserva.getStartDate(), reserva.getEndDate());
            textoPeriodo.setText(periodo);
        }
        
        // Definir nota inicial
        textoNotaSelecionada.setText("5.0");
        ratingBar.setRating(5.0f);
        
        Log.d(TAG, "Interface atualizada");
    }
    
    /**
     * Formata o período da reserva
     */
    private String formatarPeriodo(java.util.Date dataInicio, java.util.Date dataFim) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return formato.format(dataInicio) + " a " + formato.format(dataFim);
    }
    
    /**
     * Envia a avaliação para o Firebase
     */
    private void enviarAvaliacao() {
        if (avaliando) {
            Log.d(TAG, "Já está enviando avaliação");
            return;
        }
        
        avaliando = true;
        botaoAvaliar.setText("Enviando...");
        botaoAvaliar.setEnabled(false);
        
        // Obter dados da avaliação
        float notaFloat = ratingBar.getRating();
        String comentario = campoComentario.getText().toString().trim();
        
        // Validar nota
        if (notaFloat < 1.0f || notaFloat > 5.0f) {
            Toast.makeText(this, "Por favor, selecione uma nota entre 1 e 5 estrelas", Toast.LENGTH_SHORT).show();
            avaliando = false;
            botaoAvaliar.setText("Avaliar Locatário");
            botaoAvaliar.setEnabled(true);
            return;
        }
        
        Log.d(TAG, "Enviando avaliação - Nota: " + notaFloat + ", Comentário: " + comentario);
        
        // Validar dados obrigatórios
        if (reserva == null || reserva.getInstrumentId() == null) {
            Log.e(TAG, "Erro: Dados da reserva inválidos");
            Toast.makeText(this, "Erro: Dados da reserva não encontrados", Toast.LENGTH_SHORT).show();
            avaliando = false;
            botaoAvaliar.setText("Avaliar Locatário");
            botaoAvaliar.setEnabled(true);
            return;
        }
        
        // Criar avaliação de usuário
        FirebaseAvaliacaoUsuario avaliacao = new FirebaseAvaliacaoUsuario(
                reserva.getInstrumentId(),
                instrumentoNome != null ? instrumentoNome : "Instrumento",
                usuarioAtual.getUid(),
                usuarioAtual.getDisplayName() != null ? usuarioAtual.getDisplayName() : "Proprietário",
                locatarioId,
                locatarioNome != null ? locatarioNome : "Locatário",
                reservaId,
                notaFloat,
                comentario
        );
        
        Log.d(TAG, "Avaliação criada: " + avaliacao.toString());
        
        // Enviar para o Firebase
        GerenciadorFirebase.enviarAvaliacaoUsuario(avaliacao)
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Log.d(TAG, "Avaliação enviada com sucesso");
                            Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_LONG).show();
                            
                            // Voltar para a tela anterior com indicação de atualização
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.e(TAG, "Falha ao enviar avaliação");
                            Toast.makeText(this, "Erro ao enviar avaliação. Tente novamente.", Toast.LENGTH_LONG).show();
                            avaliando = false;
                            botaoAvaliar.setText("Avaliar Locatário");
                            botaoAvaliar.setEnabled(true);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao enviar avaliação: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao enviar avaliação: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        avaliando = false;
                        botaoAvaliar.setText("Avaliar Locatário");
                        botaoAvaliar.setEnabled(true);
                    });
                    return null;
                });
    }
}
