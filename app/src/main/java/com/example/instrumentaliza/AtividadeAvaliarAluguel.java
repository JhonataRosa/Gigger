package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.instrumentaliza.models.FirebaseAvaliacao;
import com.example.instrumentaliza.models.FirebaseReservation;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.CompletableFuture;
import java.util.Date;
import java.util.Locale;

/**
 * AtividadeAvaliarAluguel - Tela para avaliar um aluguel
 * 
 * Esta tela permite ao locatário avaliar um instrumento alugado,
 * fornecendo uma nota de 1 a 5 estrelas e um comentário opcional.
 * 
 * Funcionalidades:
 * - Seleção de nota com RatingBar (1-5 estrelas)
 * - Campo de comentário opcional
 * - Validação dos dados inseridos
 * - Envio da avaliação para o Firebase
 * - Feedback visual durante o processo
 * - Navegação de volta após avaliação
 * 
 * Características técnicas:
 * - Validação de dados obrigatórios
 * - Tratamento de erros de rede
 * - Interface responsiva
 * - Feedback visual com toasts
 * - Logs detalhados para debug
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeAvaliarAluguel extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "AvaliarAluguel";
    
    // Componentes da interface
    private TextView textoNomeInstrumento;
    private TextView textoProprietario;
    private TextView textoPeriodoAluguel;
    private RatingBar ratingBar;
    private TextView textoNotaSelecionada;
    private EditText campoComentario;
    private MaterialButton botaoAvaliar;
    private TextView textoContadorCaracteres;
    
    // Dados da reserva
    private String reservaId;
    private FirebaseReservation reserva;
    private DocumentSnapshot reservaDocumento;
    private String instrumentoNome;
    private String proprietarioNome;
    private String proprietarioId;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;
    
    // Controle de estado
    private boolean avaliando = false;
    
    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de avaliação, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização dos componentes da UI
     * - Carregamento dos dados da reserva
     * - Configuração dos listeners
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avaliar_aluguel);
        
        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();
        usuarioAtual = autenticacao.getCurrentUser();
        
        // Verificar se usuário está logado
        if (usuarioAtual == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Obter dados da reserva
        reservaId = getIntent().getStringExtra("reserva_id");
        if (reservaId == null || reservaId.trim().isEmpty()) {
            Toast.makeText(this, "ID da reserva não fornecido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Obter dados adicionais se fornecidos
        String instrumentoIdExtra = getIntent().getStringExtra("instrumento_id");
        String instrumentoNomeExtra = getIntent().getStringExtra("instrumento_nome");
        String proprietarioIdExtra = getIntent().getStringExtra("proprietario_id");
        String proprietarioNomeExtra = getIntent().getStringExtra("proprietario_nome");
        
        Log.d(TAG, "Dados recebidos - Reserva: " + reservaId + 
                   ", Instrumento: " + instrumentoIdExtra + 
                   ", Proprietário: " + proprietarioIdExtra);
        
        // Configurar Toolbar
        configurarToolbar();
        
        // Inicializar componentes
        inicializarComponentes();
        
        // Carregar dados da reserva
        carregarDadosReserva();
        
        // Configurar listeners
        configurarListeners();
        
        Log.d(TAG, "Atividade criada para reserva: " + reservaId);
    }
    
    /**
     * Configura a toolbar com título e botão de voltar
     */
    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Avaliar Aluguel");
        }
    }
    
    /**
     * Inicializa todos os componentes da interface
     */
    private void inicializarComponentes() {
        textoNomeInstrumento = findViewById(R.id.textoNomeInstrumento);
        textoProprietario = findViewById(R.id.textoProprietario);
        textoPeriodoAluguel = findViewById(R.id.textoPeriodoAluguel);
        ratingBar = findViewById(R.id.ratingBar);
        textoNotaSelecionada = findViewById(R.id.textoNotaSelecionada);
        campoComentario = findViewById(R.id.campoComentario);
        botaoAvaliar = findViewById(R.id.botaoAvaliar);
        textoContadorCaracteres = findViewById(R.id.textoContadorCaracteres);
        
        // Configurar RatingBar
        ratingBar.setRating(0f); // Valor padrão: 0 estrelas
        ratingBar.setStepSize(0.5f); // Incrementos de 0.5 estrela (meias estrelas)
        ratingBar.setIsIndicator(false); // Permitir interação do usuário
        
        // Configurar texto inicial da nota
        textoNotaSelecionada.setText("0.0");
        
        // Configurar botão
        botaoAvaliar.setText("Enviar Avaliação");
    }
    
    /**
     * Carrega os dados da reserva do Firebase
     */
    private void carregarDadosReserva() {
        Log.d(TAG, "Carregando dados da reserva: " + reservaId);
        
        GerenciadorFirebase.obterReservaPorId(reservaId)
                .thenAccept(documento -> {
                    if (documento != null && documento.exists()) {
                        runOnUiThread(() -> {
                            reservaDocumento = documento;
                            reserva = FirebaseReservation.fromDocument(documento);
                            exibirDadosReserva();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Reserva não encontrada", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
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
     * Exibe os dados da reserva na interface
     */
    private void exibirDadosReserva() {
        if (reserva == null) return;
        
        // Usar dados já carregados ou buscar do documento
        if (instrumentoNome != null) {
            textoNomeInstrumento.setText(instrumentoNome);
        } else {
            textoNomeInstrumento.setText("Carregando...");
        }
        
        if (proprietarioNome != null) {
            textoProprietario.setText("Proprietário: " + proprietarioNome);
        } else {
            textoProprietario.setText("Carregando...");
        }
        
        // Formatar período do aluguel
        String periodo = formatarPeriodo(reserva.getStartDate(), reserva.getEndDate());
        textoPeriodoAluguel.setText("Período: " + periodo);
        
        // Buscar informações adicionais do instrumento
        if (reserva.getInstrumentId() != null) {
            buscarInformacoesInstrumento(reserva.getInstrumentId());
        }
        
        Log.d(TAG, "Dados da reserva exibidos");
    }
    
    /**
     * Busca informações adicionais do instrumento e proprietário
     * 
     * @param instrumentoId ID do instrumento
     */
    private void buscarInformacoesInstrumento(String instrumentoId) {
        Log.d(TAG, "Buscando informações do instrumento: " + instrumentoId);
        
        GerenciadorFirebase.obterInstrumentoPorId(instrumentoId)
                .thenAccept(instrumentoDoc -> {
                    if (instrumentoDoc != null && instrumentoDoc.exists()) {
                        runOnUiThread(() -> {
                            // Obter nome do instrumento
                            instrumentoNome = instrumentoDoc.getString("name");
                            if (instrumentoNome != null) {
                                textoNomeInstrumento.setText(instrumentoNome);
                            }
                            
                            // Obter ID do proprietário
                            proprietarioId = instrumentoDoc.getString("ownerId");
                            if (proprietarioId != null) {
                                buscarInformacoesProprietario(proprietarioId);
                            }
                            
                            Log.d(TAG, "Informações do instrumento carregadas: " + instrumentoNome);
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao buscar instrumento: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        textoNomeInstrumento.setText("Erro ao carregar nome do instrumento");
                    });
                    return null;
                });
    }
    
    /**
     * Busca informações do proprietário
     * 
     * @param proprietarioId ID do proprietário
     */
    private void buscarInformacoesProprietario(String proprietarioId) {
        Log.d(TAG, "Buscando informações do proprietário: " + proprietarioId);
        
        // Tentar buscar do documento da reserva primeiro
        if (reservaDocumento != null) {
            String nomeReserva = reservaDocumento.getString("ownerName");
            String emailReserva = reservaDocumento.getString("ownerEmail");
            
            if (nomeReserva != null && !nomeReserva.trim().isEmpty()) {
                runOnUiThread(() -> {
                    proprietarioNome = nomeReserva;
                    textoProprietario.setText("Proprietário: " + proprietarioNome);
                    Log.d(TAG, "Nome do proprietário obtido da reserva: " + proprietarioNome);
                });
                return; // Se encontrou na reserva, não precisa buscar no Firestore
            }
        }
        
        // Buscar no Firestore se não encontrou na reserva
        GerenciadorFirebase.obterUsuarioPorId(proprietarioId)
                .thenAccept(usuarioDoc -> {
                    if (usuarioDoc != null && usuarioDoc.exists()) {
                        runOnUiThread(() -> {
                            proprietarioNome = usuarioDoc.getString("name");
                            if (proprietarioNome != null && !proprietarioNome.trim().isEmpty()) {
                                textoProprietario.setText("Proprietário: " + proprietarioNome);
                            } else {
                                proprietarioNome = "Proprietário";
                                textoProprietario.setText("Proprietário: " + proprietarioNome);
                            }
                            
                            Log.d(TAG, "Informações do proprietário carregadas: " + proprietarioNome);
                        });
                    } else {
                        runOnUiThread(() -> {
                            proprietarioNome = "Proprietário";
                            textoProprietario.setText("Proprietário: " + proprietarioNome);
                            Log.d(TAG, "Usando nome padrão para o proprietário");
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao buscar proprietário: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        proprietarioNome = "Proprietário";
                        textoProprietario.setText("Proprietário: " + proprietarioNome);
                        Log.d(TAG, "Erro ao buscar, usando nome padrão");
                    });
                    return null;
                });
    }
    
    /**
     * Formata o período de aluguel para exibição
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @return String formatada com o período
     */
    private String formatarPeriodo(Date dataInicio, Date dataFim) {
        if (dataInicio == null || dataFim == null) {
            return "Período não informado";
        }
        
        java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return formato.format(dataInicio) + " a " + formato.format(dataFim);
    }
    
    /**
     * Configura os listeners dos componentes
     */
    private void configurarListeners() {
        // Listener do RatingBar
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            Log.d(TAG, "Nota selecionada: " + rating);
            textoNotaSelecionada.setText(String.format(Locale.getDefault(), "%.1f", rating));
        });
        
        // Listener do campo de comentário
        campoComentario.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                int caracteres = s.length();
                textoContadorCaracteres.setText(caracteres + "/500");
                
                // Limitar a 500 caracteres
                if (caracteres > 500) {
                    s.delete(500, s.length());
                    Toast.makeText(AtividadeAvaliarAluguel.this, "Máximo de 500 caracteres", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Listener do botão avaliar
        botaoAvaliar.setOnClickListener(v -> avaliarAluguel());
    }
    
    /**
     * Processa a avaliação do aluguel
     */
    private void avaliarAluguel() {
        if (avaliando) return;
        
        // Validar dados
        if (!validarDados()) return;
        
        avaliando = true;
        botaoAvaliar.setText("Enviando...");
        botaoAvaliar.setEnabled(false);
        
        // Obter dados da avaliação
        float notaFloat = ratingBar.getRating();
        // Manter a nota como float para preservar meias estrelas (3.5, 4.0, 4.5, etc.)
        String comentario = campoComentario.getText().toString().trim();
        
        // Validar nota
        if (notaFloat < 1.0f || notaFloat > 5.0f) {
            Toast.makeText(this, "Por favor, selecione uma nota entre 1 e 5 estrelas", Toast.LENGTH_SHORT).show();
            avaliando = false;
            botaoAvaliar.setText("Enviar Avaliação");
            botaoAvaliar.setEnabled(true);
            return;
        }
        
        Log.d(TAG, "Enviando avaliação - Nota: " + notaFloat + ", Comentário: " + comentario);
        
        // Validar dados obrigatórios
        if (reserva == null || reserva.getInstrumentId() == null) {
            Log.e(TAG, "Erro: Dados da reserva inválidos");
            Toast.makeText(this, "Erro: Dados da reserva não encontrados", Toast.LENGTH_SHORT).show();
            avaliando = false;
            botaoAvaliar.setText("Enviar Avaliação");
            botaoAvaliar.setEnabled(true);
            return;
        }
        
        // Criar avaliação
        FirebaseAvaliacao avaliacao = new FirebaseAvaliacao(
                reserva.getInstrumentId(),
                instrumentoNome != null ? instrumentoNome : "Instrumento",
                proprietarioId != null ? proprietarioId : "",
                proprietarioNome != null ? proprietarioNome : "Proprietário",
                usuarioAtual.getUid(),
                usuarioAtual.getDisplayName() != null ? usuarioAtual.getDisplayName() : "Usuário",
                reservaId,
                notaFloat,
                comentario
        );
        
        Log.d(TAG, "Avaliação criada: " + avaliacao.toString());
        
        // Enviar para o Firebase
        GerenciadorFirebase.enviarAvaliacao(avaliacao)
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Avaliação enviada com sucesso");
                            
                            // Marcar reserva como avaliada
                            marcarReservaComoAvaliada();
                        } else {
                            Toast.makeText(this, "Erro ao enviar avaliação", Toast.LENGTH_SHORT).show();
                            reativarBotao();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao enviar avaliação: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao enviar avaliação: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        reativarBotao();
                    });
                    return null;
                });
    }
    
    /**
     * Valida os dados inseridos pelo usuário
     * 
     * @return true se válidos, false caso contrário
     */
    private boolean validarDados() {
        // Validação básica - a validação detalhada é feita no método avaliarAluguel
        return true;
    }
    
    /**
     * Marca a reserva como avaliada no Firebase
     */
    private void marcarReservaComoAvaliada() {
        GerenciadorFirebase.marcarReservaComoAvaliada(reservaId)
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Log.d(TAG, "Reserva marcada como avaliada");
                        }
                        
                        // Fechar atividade após um delay
                        new android.os.Handler().postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 1500);
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao marcar reserva como avaliada: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        // Fechar atividade mesmo com erro
                        new android.os.Handler().postDelayed(() -> {
                            setResult(RESULT_OK);
                            finish();
                        }, 1500);
                    });
                    return null;
                });
    }
    
    /**
     * Reativa o botão após erro
     */
    private void reativarBotao() {
        avaliando = false;
        botaoAvaliar.setText("Enviar Avaliação");
        botaoAvaliar.setEnabled(true);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
