package com.example.instrumentaliza;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.instrumentaliza.models.FirebaseSolicitacao;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeDetalhesSolicitacao - Tela de detalhes de uma solicitação de reserva
 * 
 * Esta tela exibe todos os detalhes de uma solicitação específica, permitindo
 * ao proprietário do instrumento visualizar as informações completas e decidir
 * se aceita ou recusa a solicitação de reserva.
 * 
 * Funcionalidades principais:
 * - Exibição completa dos dados da solicitação
 * - Informações detalhadas do solicitante
 * - Período, preço e observações
 * - Botões para aceitar ou recusar a solicitação
 * - Dialog para motivo da recusa
 * - Atualização do status no Firebase
 * - Conversão em reserva ativa (quando aceita)
 * - Navegação de volta após ação
 * 
 * Características técnicas:
 * - Carregamento assíncrono de dados do Firebase
 * - Interface adaptativa baseada no status
 * - Validação de permissões do usuário
 * - Tratamento de erros e estados de loading
 * - Dialog customizado para input de texto
 * - Integração com GerenciadorFirebase
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeDetalhesSolicitacao extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "DetalhesSolicitacao";
    
    // Variáveis da interface
    private TextView instrumentNameTextView;
    private TextView solicitanteNomeTextView;
    private TextView solicitanteEmailTextView;
    private TextView solicitanteTelefoneTextView;
    private TextView statusTextView;
    private TextView periodoTextView;
    private TextView precoTotalTextView;
    private TextView dataSolicitacaoTextView;
    private TextView observacoesTextView;
    private com.google.android.material.card.MaterialCardView observacoesCardView;
    private TextView motivoRecusaTextView;
    private com.google.android.material.card.MaterialCardView motivoRecusaCardView;
    private LinearLayout actionButtonsLayout;
    private MaterialButton acceptButton;
    private MaterialButton rejectButton;
    
    // Variáveis de dados
    private String idSolicitacao;
    private String nomeInstrumento;
    private FirebaseSolicitacao solicitacao;
    
    // Firebase
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Inicializa todos os componentes da interface, configura a toolbar,
     * obtém os dados do Intent e carrega os detalhes da solicitação.
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_solicitacao);
        
        Log.d(TAG, "=== ATIVIDADE DETALHES SOLICITAÇÃO INICIADA ===");
        Log.d(TAG, "Layout carregado: activity_detalhes_solicitacao");
        
        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detalhes da Solicitação");
        }
        
        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();
        usuarioAtual = autenticacao.getCurrentUser();
        
        // Verificar se o usuário está logado
        if (usuarioAtual == null) {
            Log.e(TAG, "Usuário não está logado");
            finish();
            return;
        }
        
        // Obter dados do Intent
        idSolicitacao = getIntent().getStringExtra("solicitacao_id");
        nomeInstrumento = getIntent().getStringExtra("instrument_name");
        
        if (idSolicitacao == null) {
            Log.e(TAG, "ID da solicitação não fornecido");
            finish();
            return;
        }
        
        Log.d(TAG, "Solicitação: " + idSolicitacao + " - Instrumento: " + nomeInstrumento);
        
        // Inicializar componentes da UI
        inicializarComponentes();
        
        // Carregar detalhes da solicitação
        carregarDetalhesSolicitacao();
        
        // Marcar solicitação como lida
        marcarSolicitacaoComoLida();
    }

    /**
     * Inicializa todos os componentes da interface de usuário
     * 
     * Configura as referências para todos os elementos visuais
     * e configura os listeners dos botões de ação.
     */
    private void inicializarComponentes() {
        Log.d(TAG, "Inicializando componentes da UI...");
        
        instrumentNameTextView = findViewById(R.id.instrumentNameTextView);
        solicitanteNomeTextView = findViewById(R.id.solicitanteNomeTextView);
        solicitanteEmailTextView = findViewById(R.id.solicitanteEmailTextView);
        solicitanteTelefoneTextView = findViewById(R.id.solicitanteTelefoneTextView);
        statusTextView = findViewById(R.id.statusTextView);
        periodoTextView = findViewById(R.id.periodoTextView);
        precoTotalTextView = findViewById(R.id.precoTotalTextView);
        dataSolicitacaoTextView = findViewById(R.id.dataSolicitacaoTextView);
        observacoesTextView = findViewById(R.id.observacoesTextView);
        observacoesCardView = findViewById(R.id.observacoesCardView);
        motivoRecusaTextView = findViewById(R.id.motivoRecusaTextView);
        motivoRecusaCardView = findViewById(R.id.motivoRecusaCardView);
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout);
        acceptButton = findViewById(R.id.acceptButton);
        rejectButton = findViewById(R.id.rejectButton);
        MaterialButton sendMessageButton = findViewById(R.id.sendMessageButton);
        
        Log.d(TAG, "Componentes inicializados com sucesso");
        
        // Configurar nome do instrumento
        if (nomeInstrumento != null) {
            instrumentNameTextView.setText(nomeInstrumento);
        }
        
        // Configurar listeners dos botões
        acceptButton.setOnClickListener(v -> aceitarSolicitacao());
        rejectButton.setOnClickListener(v -> mostrarDialogRecusa());
        sendMessageButton.setOnClickListener(v -> enviarMensagem());
    }

    /**
     * Carrega os detalhes da solicitação do Firebase
     * 
     * Busca a solicitação específica pelo ID e atualiza a interface
     * com todas as informações disponíveis.
     */
    private void carregarDetalhesSolicitacao() {
        Log.d(TAG, "Carregando detalhes da solicitação: " + idSolicitacao);
        
        GerenciadorFirebase.obterSolicitacaoPorId(idSolicitacao)
                .thenAccept(documento -> {
                    runOnUiThread(() -> {
                        if (documento.exists()) {
                            solicitacao = FirebaseSolicitacao.fromDocument(documento);
                            atualizarInterface();
                        } else {
                            Log.e(TAG, "Solicitação não encontrada");
                            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar solicitação: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao carregar detalhes da solicitação", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return null;
                });
    }

    /**
     * Atualiza a interface com os dados da solicitação
     * 
     * Preenche todos os campos visuais com as informações da solicitação
     * e ajusta a interface baseada no status atual.
     */
    private void atualizarInterface() {
        if (solicitacao == null) return;
        
        Log.d(TAG, "Atualizando interface para solicitação: " + solicitacao.getStatus());
        
        // Informações do solicitante
        solicitanteNomeTextView.setText(solicitacao.getSolicitanteNome());
        solicitanteEmailTextView.setText(solicitacao.getSolicitanteEmail());
        
        String telefone = solicitacao.getSolicitanteTelefone();
        if (telefone != null && !telefone.isEmpty()) {
            solicitanteTelefoneTextView.setText("Telefone: " + telefone);
        } else {
            solicitanteTelefoneTextView.setText("Telefone: Não informado");
        }
        
        // Detalhes da solicitação
        statusTextView.setText(solicitacao.getStatus());
        atualizarCorStatus();
        
        // Período
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String periodo = formatoData.format(solicitacao.getDataInicio()) + " a " + 
                        formatoData.format(solicitacao.getDataFim());
        periodoTextView.setText(periodo);
        
        // Preço total
        precoTotalTextView.setText(String.format(Locale.getDefault(), "R$ %.2f", solicitacao.getPrecoTotal()));
        
        // Data da solicitação
        if (solicitacao.getDataCriacao() != null) {
            SimpleDateFormat formatoDataHora = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault());
            String dataSolicitacao = "Solicitado em " + formatoDataHora.format(solicitacao.getDataCriacao());
            dataSolicitacaoTextView.setText(dataSolicitacao);
        }
        
        // Observações
        String observacoes = solicitacao.getObservacoes();
        if (observacoes != null && !observacoes.trim().isEmpty()) {
            observacoesTextView.setText(observacoes);
            observacoesCardView.setVisibility(View.VISIBLE);
        } else {
            observacoesCardView.setVisibility(View.GONE);
        }
        
        // Motivo da recusa (se aplicável)
        String motivoRecusa = solicitacao.getMotivoRecusa();
        if ("RECUSADA".equals(solicitacao.getStatus()) && motivoRecusa != null && !motivoRecusa.trim().isEmpty()) {
            motivoRecusaTextView.setText(motivoRecusa);
            motivoRecusaCardView.setVisibility(View.VISIBLE);
        } else {
            motivoRecusaCardView.setVisibility(View.GONE);
        }
        
        // Configurar botões de ação baseado no status
        configurarBotoesAcao();
    }

    /**
     * Atualiza a cor do status baseada no valor atual
     */
    private void atualizarCorStatus() {
        String status = solicitacao.getStatus();
        int corStatus;
        
        switch (status) {
            case "PENDENTE":
                corStatus = getResources().getColor(R.color.status_pending);
                break;
            case "ACEITA":
                corStatus = getResources().getColor(R.color.status_accepted);
                break;
            case "RECUSADA":
                corStatus = getResources().getColor(R.color.status_rejected);
                break;
            default:
                corStatus = getResources().getColor(R.color.text_gray_light);
                break;
        }
        
        statusTextView.setTextColor(corStatus);
    }

    /**
     * Configura a visibilidade e estado dos botões de ação
     * 
     * Mostra os botões apenas para solicitações pendentes e apenas
     * para o proprietário do instrumento.
     * Mostra o botão "Enviar Mensagem" para solicitações aceitas.
     */
    private void configurarBotoesAcao() {
        MaterialButton sendMessageButton = findViewById(R.id.sendMessageButton);
        
        if ("PENDENTE".equals(solicitacao.getStatus()) && 
            usuarioAtual.getUid().equals(solicitacao.getProprietarioId())) {
            actionButtonsLayout.setVisibility(View.VISIBLE);
            sendMessageButton.setVisibility(View.GONE);
        } else if ("ACEITA".equals(solicitacao.getStatus())) {
            actionButtonsLayout.setVisibility(View.GONE);
            sendMessageButton.setVisibility(View.VISIBLE);
        } else {
            actionButtonsLayout.setVisibility(View.GONE);
            sendMessageButton.setVisibility(View.GONE);
        }
    }

    /**
     * Aceita a solicitação de reserva
     * 
     * Atualiza o status da solicitação para "ACEITA" e converte
     * em uma reserva ativa no sistema.
     */
    private void aceitarSolicitacao() {
        Log.d(TAG, "Aceitando solicitação: " + idSolicitacao);
        
        // Desabilitar botões para evitar múltiplas ações
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        acceptButton.setText("Aceitando...");
        
        // Mostrar toast informativo
        Toast.makeText(this, "Aceitando solicitação e atualizando disponibilidade...", Toast.LENGTH_SHORT).show();
        
        GerenciadorFirebase.aceitarSolicitacao(idSolicitacao)
                .thenCompose(resultado -> {
                    if (resultado) {
                        // Converter em reserva ativa
                        return GerenciadorFirebase.converterSolicitacaoEmReserva(idSolicitacao);
                    } else {
                        throw new RuntimeException("Falha ao aceitar solicitação");
                    }
                })
                .thenAccept(resultado -> {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Solicitação aceita, convertida em reserva e disponibilidade atualizada");
                        Toast.makeText(this, "Solicitação aceita! Reserva criada e disponibilidade atualizada.", Toast.LENGTH_LONG).show();
                        
                        // Voltar para a lista de solicitações
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao aceitar solicitação: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao aceitar solicitação: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        
                        // Reabilitar botões
                        acceptButton.setEnabled(true);
                        rejectButton.setEnabled(true);
                        acceptButton.setText("Aceitar");
                    });
                    return null;
                });
    }

    /**
     * Mostra o dialog para inserir motivo da recusa
     * 
     * Exibe um dialog com campo de texto para o proprietário
     * inserir o motivo da recusa da solicitação.
     */
    private void mostrarDialogRecusa() {
        Log.d(TAG, "Mostrando dialog de recusa");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recusar Solicitação");
        builder.setMessage("Por favor, informe o motivo da recusa para que o solicitante possa entender:");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Ex: Instrumento já reservado para este período, instrumento em manutenção, etc.");
        input.setMinLines(3);
        input.setMaxLines(5);
        
        // Adicionar padding ao EditText
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);
        
        builder.setPositiveButton("Confirmar Recusa", (dialog, which) -> {
            String motivoRecusa = input.getText().toString().trim();
            if (motivoRecusa.isEmpty()) {
                Toast.makeText(this, "Por favor, informe o motivo da recusa", Toast.LENGTH_SHORT).show();
                return;
            }
            if (motivoRecusa.length() < 10) {
                Toast.makeText(this, "O motivo deve ter pelo menos 10 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            recusarSolicitacao(motivoRecusa);
        });
        
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Focar no campo de texto
        input.requestFocus();
    }

    /**
     * Recusa a solicitação de reserva
     * 
     * Atualiza o status da solicitação para "RECUSADA" com o motivo
     * fornecido pelo proprietário.
     * 
     * @param motivoRecusa Motivo da recusa fornecido pelo proprietário
     */
    private void recusarSolicitacao(String motivoRecusa) {
        Log.d(TAG, "Recusando solicitação: " + idSolicitacao + " - Motivo: " + motivoRecusa);
        
        // Desabilitar botões para evitar múltiplas ações
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        rejectButton.setText("Processando...");
        
        // Mostrar toast informativo
        Toast.makeText(this, "Recusando solicitação...", Toast.LENGTH_SHORT).show();
        
        GerenciadorFirebase.recusarSolicitacao(idSolicitacao, motivoRecusa)
                .thenAccept(resultado -> {
                    runOnUiThread(() -> {
                        if (resultado) {
                            Log.d(TAG, "Solicitação recusada com sucesso");
                            Toast.makeText(this, "Solicitação recusada com sucesso. O solicitante será notificado.", Toast.LENGTH_LONG).show();
                            
                            // Voltar para a lista de solicitações
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            throw new RuntimeException("Falha ao recusar solicitação");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao recusar solicitação: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao recusar solicitação: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        
                        // Reabilitar botões
                        acceptButton.setEnabled(true);
                        rejectButton.setEnabled(true);
                        rejectButton.setText("Recusar");
                    });
                    return null;
                });
    }

    /**
     * Manipula a seleção de itens do menu (navegação)
     * 
     * @param item Item do menu selecionado
     * @return true se o item foi manipulado, false caso contrário
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Marca a solicitação como lida no Firebase
     */
    private void marcarSolicitacaoComoLida() {
        if (idSolicitacao != null) {
            GerenciadorFirebase.marcarSolicitacaoComoLida(idSolicitacao)
                    .thenAccept(sucesso -> {
                        if (sucesso) {
                            Log.d(TAG, "Solicitação marcada como lida: " + idSolicitacao);
                        } else {
                            Log.w(TAG, "Falha ao marcar solicitação como lida");
                        }
                    })
                    .exceptionally(erro -> {
                        Log.e(TAG, "Erro ao marcar solicitação como lida: " + erro.getMessage(), erro);
                        return null;
                    });
        }
    }
    
    /**
     * Abre o chat para comunicação sobre a solicitação aceita
     */
    private void enviarMensagem() {
        Log.d(TAG, "Abrindo chat para solicitação: " + idSolicitacao);
        
        // Verificar se já existe um chat para este instrumento
        String instrumentoId = solicitacao.getInstrumentoId();
        String proprietarioId = solicitacao.getProprietarioId();
        String solicitanteId = solicitacao.getSolicitanteId();
        
        // Criar ou buscar chat existente
        GerenciadorFirebase.criarOuObterChat(instrumentoId, solicitanteId, proprietarioId)
                .thenAccept(chatId -> {
                    runOnUiThread(() -> {
                        if (chatId != null) {
                            // Navegar para o chat
                            Intent intent = new Intent(this, AtividadeChat.class);
                            intent.putExtra("chat_id", chatId);
                            intent.putExtra("instrument_id", instrumentoId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Erro ao criar chat", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao criar/buscar chat: " + erro.getMessage(), erro);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao abrir chat: " + erro.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }
}
