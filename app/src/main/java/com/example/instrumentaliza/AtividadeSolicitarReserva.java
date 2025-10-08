package com.example.instrumentaliza;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.firebase.Timestamp;

/**
 * AtividadeSolicitarReserva - Tela para solicitação de reserva de instrumento
 * 
 * Esta tela permite que usuários solicitem a reserva de um instrumento específico,
 * selecionando um período de aluguel e adicionando observações opcionais. Integra
 * com o sistema de solicitações do Firebase para processar as requisições.
 * 
 * Funcionalidades principais:
 * - Seleção de período de aluguel com calendário
 * - Cálculo automático do preço total
 * - Validação de datas e períodos
 * - Verificação de disponibilidade
 * - Criação de solicitação no Firebase
 * - Interface responsiva com feedback visual
 * 
 * Fluxo de funcionamento:
 * 1. Usuário seleciona data de início
 * 2. Usuário seleciona data de fim
 * 3. Sistema calcula preço total automaticamente
 * 4. Usuário pode adicionar observações opcionais
 * 5. Sistema valida e cria solicitação no Firebase
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeSolicitarReserva extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "SolicitarReserva";
    
    // Componentes da interface
    private TextView instrumentNameTextView;
    private TextView availabilityInfoTextView;
    private TextView instrumentCategoryTextView;
    private TextView instrumentPriceTextView;
    private TextView startDateTextView;
    private TextView endDateTextView;
    private TextView periodSummaryTextView;
    private TextView totalPriceTextView;
    private EditText observationsEditText;
    private Button solicitarButton;
    private LinearLayout periodSummaryLayout;
    private LinearLayout totalLayout;
    
    // Dados da solicitação
    private String idInstrumento;
    private String nomeInstrumento;
    private String categoriaInstrumento;
    private double precoPorDia;
    private String idProprietario;
    private Date dataInicio;
    private Date dataFim;
    private double precoTotal;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;
    
    // Datas indisponíveis
    private List<Map<String, Object>> faixasIndisponiveis;
    private ValidadorDatasIndisponiveis validadorDatas;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de solicitação de reserva, incluindo:
     * - Inicialização dos componentes da interface
     * - Carregamento dos dados do instrumento
     * - Configuração dos listeners de seleção de data
     * - Validação de permissões do usuário
     * - Configuração da toolbar e navegação
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitar_reserva);
        
        Log.d(TAG, "=== ATIVIDADE SOLICITAR RESERVA INICIADA ===");
        
        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();
        usuarioAtual = autenticacao.getCurrentUser();
        
        // Verificar se o usuário está logado
        if (usuarioAtual == null) {
            Log.d(TAG, "Usuário não está logado, redirecionando para login");
            Toast.makeText(this, "É necessário estar logado para solicitar reservas", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AtividadeLogin.class));
            finish();
            return;
        }
        
        // Inicializar componentes da interface
        inicializarComponentes();
        
        // Carregar dados do instrumento
        carregarDadosInstrumento();
        
        // Carregar datas indisponíveis
        carregarDatasIndisponiveis();
        
        // Configurar listeners
        configurarListeners();
    }

    /**
     * Carrega as datas indisponíveis do instrumento
     * 
     * Busca no Firebase as faixas de datas que não estão disponíveis
     * para reserva (períodos manualmente marcados ou já reservados).
     */
    private void carregarDatasIndisponiveis() {
        Log.d(TAG, "Carregando datas indisponíveis para o instrumento: " + idInstrumento);
        
        if (idInstrumento == null || idInstrumento.isEmpty()) {
            Log.e(TAG, "ID do instrumento não disponível para carregar datas indisponíveis");
            faixasIndisponiveis = new ArrayList<>();
            return;
        }
        
        GerenciadorFirebase.obterFaixasIndisponiveisInstrumento(idInstrumento)
                .thenAccept(faixas -> {
                    runOnUiThread(() -> {
                        faixasIndisponiveis = faixas != null ? faixas : new ArrayList<>();
                        Log.d(TAG, "Datas indisponíveis carregadas: " + faixasIndisponiveis.size() + " períodos");
                        
                        // Criar validador com as datas indisponíveis
                        criarValidadorDatas();
                        
                        // Atualizar informação visual de disponibilidade
                        atualizarInformacaoDisponibilidade();
                        
                        // Log das faixas carregadas para debug
                        for (int i = 0; i < faixasIndisponiveis.size(); i++) {
                            Map<String, Object> faixa = faixasIndisponiveis.get(i);
                            Object inicio = faixa.get("startDate");
                            Object fim = faixa.get("endDate");
                            Object tipo = faixa.get("type");
                            
                            Log.d(TAG, "Faixa " + (i+1) + ": " + inicio + " a " + fim + " (tipo: " + tipo + ")");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar datas indisponíveis: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        faixasIndisponiveis = new ArrayList<>();
                        Toast.makeText(this, "Erro ao carregar disponibilidade do instrumento", Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    /**
     * Cria o validador de datas indisponíveis baseado nas faixas carregadas
     */
    private void criarValidadorDatas() {
        List<Long> datasIndisponiveis = new ArrayList<>();
        
        if (faixasIndisponiveis != null && !faixasIndisponiveis.isEmpty()) {
            for (Map<String, Object> faixa : faixasIndisponiveis) {
                Object startObj = faixa.get("startDate");
                Object endObj = faixa.get("endDate");
                
                if (startObj instanceof Timestamp && endObj instanceof Timestamp) {
                    Timestamp inicio = (Timestamp) startObj;
                    Timestamp fim = (Timestamp) endObj;
                    
                    // Adicionar todas as datas do período
                    Date dataAtual = inicio.toDate();
                    Date dataFim = fim.toDate();
                    
                    while (!dataAtual.after(dataFim)) {
                        // Converter para início do dia
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(dataAtual);
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                        cal.set(java.util.Calendar.MINUTE, 0);
                        cal.set(java.util.Calendar.SECOND, 0);
                        cal.set(java.util.Calendar.MILLISECOND, 0);
                        
                        long timestamp = cal.getTimeInMillis();
                        if (!datasIndisponiveis.contains(timestamp)) {
                            datasIndisponiveis.add(timestamp);
                        }
                        
                        // Próximo dia
                        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                        dataAtual = cal.getTime();
                    }
                }
            }
        }
        
        validadorDatas = new ValidadorDatasIndisponiveis(datasIndisponiveis);
        Log.d(TAG, "Validador criado com " + datasIndisponiveis.size() + " datas indisponíveis");
    }
    
    /**
     * Atualiza a informação visual sobre disponibilidade do instrumento
     */
    private void atualizarInformacaoDisponibilidade() {
        if (availabilityInfoTextView == null) return;
        
        if (faixasIndisponiveis == null || faixasIndisponiveis.isEmpty()) {
            // Instrumento totalmente disponível
            availabilityInfoTextView.setText("✅ Instrumento totalmente disponível");
            availabilityInfoTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            availabilityInfoTextView.setVisibility(android.view.View.VISIBLE);
        } else {
            // Há datas indisponíveis
            int totalPeriodos = faixasIndisponiveis.size();
            
            // Contar quantos são reservas vs períodos manualmente marcados
            int reservas = 0;
            int periodosManuais = 0;
            
            for (Map<String, Object> faixa : faixasIndisponiveis) {
                Object tipo = faixa.get("type");
                if ("reservation".equals(tipo)) {
                    reservas++;
                } else {
                    periodosManuais++;
                }
            }
            
            String mensagem = "⚠️ " + totalPeriodos + " período(s) indisponível(is)";
            if (reservas > 0 && periodosManuais > 0) {
                mensagem += " (" + reservas + " reserva(s), " + periodosManuais + " período(s) manual(is))";
            } else if (reservas > 0) {
                mensagem += " (todas reservadas)";
            } else if (periodosManuais > 0) {
                mensagem += " (períodos manualmente marcados)";
            }
            mensagem += " - datas riscadas no calendário";
            
            availabilityInfoTextView.setText(mensagem);
            availabilityInfoTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            availabilityInfoTextView.setVisibility(android.view.View.VISIBLE);
        }
    }
    
    /**
     * Verifica se uma data específica está indisponível
     * 
     * @param data Data a ser verificada
     * @return true se a data estiver indisponível, false caso contrário
     */
    private boolean isDataIndisponivel(Date data) {
        if (faixasIndisponiveis == null || faixasIndisponiveis.isEmpty()) {
            return false;
        }
        
        Calendar dataVerificacao = Calendar.getInstance();
        dataVerificacao.setTime(data);
        dataVerificacao.set(Calendar.HOUR_OF_DAY, 0);
        dataVerificacao.set(Calendar.MINUTE, 0);
        dataVerificacao.set(Calendar.SECOND, 0);
        dataVerificacao.set(Calendar.MILLISECOND, 0);
        
        for (Map<String, Object> faixa : faixasIndisponiveis) {
            Object startObj = faixa.get("startDate");
            Object endObj = faixa.get("endDate");
            
            if (startObj instanceof Timestamp && endObj instanceof Timestamp) {
                Timestamp inicio = (Timestamp) startObj;
                Timestamp fim = (Timestamp) endObj;
                
                Calendar inicioFaixa = Calendar.getInstance();
                inicioFaixa.setTime(inicio.toDate());
                inicioFaixa.set(Calendar.HOUR_OF_DAY, 0);
                inicioFaixa.set(Calendar.MINUTE, 0);
                inicioFaixa.set(Calendar.SECOND, 0);
                inicioFaixa.set(Calendar.MILLISECOND, 0);
                
                Calendar fimFaixa = Calendar.getInstance();
                fimFaixa.setTime(fim.toDate());
                fimFaixa.set(Calendar.HOUR_OF_DAY, 23);
                fimFaixa.set(Calendar.MINUTE, 59);
                fimFaixa.set(Calendar.SECOND, 59);
                fimFaixa.set(Calendar.MILLISECOND, 999);
                
                // Verificar se a data está dentro da faixa indisponível
                if ((dataVerificacao.after(inicioFaixa) || dataVerificacao.equals(inicioFaixa)) &&
                    (dataVerificacao.before(fimFaixa) || dataVerificacao.equals(fimFaixa))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Verifica se um período de datas conflita com datas indisponíveis
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return true se houver conflito, false caso contrário
     */
    private boolean verificarConflitoPeriodo(Date dataInicio, Date dataFim) {
        if (faixasIndisponiveis == null || faixasIndisponiveis.isEmpty()) {
            return false;
        }
        
        Calendar inicioPeriodo = Calendar.getInstance();
        inicioPeriodo.setTime(dataInicio);
        inicioPeriodo.set(Calendar.HOUR_OF_DAY, 0);
        inicioPeriodo.set(Calendar.MINUTE, 0);
        inicioPeriodo.set(Calendar.SECOND, 0);
        inicioPeriodo.set(Calendar.MILLISECOND, 0);
        
        Calendar fimPeriodo = Calendar.getInstance();
        fimPeriodo.setTime(dataFim);
        fimPeriodo.set(Calendar.HOUR_OF_DAY, 23);
        fimPeriodo.set(Calendar.MINUTE, 59);
        fimPeriodo.set(Calendar.SECOND, 59);
        fimPeriodo.set(Calendar.MILLISECOND, 999);
        
        for (Map<String, Object> faixa : faixasIndisponiveis) {
            Object startObj = faixa.get("startDate");
            Object endObj = faixa.get("endDate");
            
            if (startObj instanceof Timestamp && endObj instanceof Timestamp) {
                Timestamp inicioFaixa = (Timestamp) startObj;
                Timestamp fimFaixa = (Timestamp) endObj;
                
                Calendar inicioIndisponivel = Calendar.getInstance();
                inicioIndisponivel.setTime(inicioFaixa.toDate());
                inicioIndisponivel.set(Calendar.HOUR_OF_DAY, 0);
                inicioIndisponivel.set(Calendar.MINUTE, 0);
                inicioIndisponivel.set(Calendar.SECOND, 0);
                inicioIndisponivel.set(Calendar.MILLISECOND, 0);
                
                Calendar fimIndisponivel = Calendar.getInstance();
                fimIndisponivel.setTime(fimFaixa.toDate());
                fimIndisponivel.set(Calendar.HOUR_OF_DAY, 23);
                fimIndisponivel.set(Calendar.MINUTE, 59);
                fimIndisponivel.set(Calendar.SECOND, 59);
                fimIndisponivel.set(Calendar.MILLISECOND, 999);
                
                // Verificar sobreposição de períodos
                boolean sobrepoe = (inicioPeriodo.before(fimIndisponivel) || inicioPeriodo.equals(fimIndisponivel)) &&
                                 (fimPeriodo.after(inicioIndisponivel) || fimPeriodo.equals(inicioIndisponivel));
                
                if (sobrepoe) {
                    Log.d(TAG, "Conflito detectado: período " + dataInicio + " a " + dataFim + 
                          " conflita com faixa indisponível " + inicioFaixa.toDate() + " a " + fimFaixa.toDate());
                    return true;
                }
            }
        }
        
        return false;
    }
    

    /**
     * Inicializa todos os componentes da interface
     * 
     * Conecta as views do layout com as variáveis da classe e
     * configura o estado inicial dos componentes.
     */
    private void inicializarComponentes() {
        Log.d(TAG, "Inicializando componentes da interface");
        
        // Componentes de informação do instrumento
        instrumentNameTextView = findViewById(R.id.instrumentNameTextView);
        availabilityInfoTextView = findViewById(R.id.availabilityInfoTextView);
        instrumentCategoryTextView = findViewById(R.id.instrumentCategoryTextView);
        instrumentPriceTextView = findViewById(R.id.instrumentPriceTextView);
        
        // Componentes de seleção de data
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        periodSummaryTextView = findViewById(R.id.periodSummaryTextView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        
        // Componentes de layout
        periodSummaryLayout = findViewById(R.id.periodSummaryLayout);
        totalLayout = findViewById(R.id.totalLayout);
        
        // Componentes de entrada
        observationsEditText = findViewById(R.id.observationsEditText);
        solicitarButton = findViewById(R.id.solicitarButton);
        
        // Estado inicial
        solicitarButton.setEnabled(false);
        periodSummaryLayout.setVisibility(android.view.View.GONE);
        totalLayout.setVisibility(android.view.View.GONE);
        
        Log.d(TAG, "Componentes inicializados com sucesso");
    }

    /**
     * Carrega os dados do instrumento a partir dos extras da Intent
     * 
     * Extrai as informações do instrumento passadas pela tela anterior
     * e atualiza a interface com os dados carregados.
     */
    private void carregarDadosInstrumento() {
        Log.d(TAG, "Carregando dados do instrumento");
        
        Intent intent = getIntent();
        if (intent != null) {
            idInstrumento = intent.getStringExtra("instrument_id");
            nomeInstrumento = intent.getStringExtra("instrument_name");
            precoPorDia = intent.getDoubleExtra("instrument_price", 0.0);
            idProprietario = intent.getStringExtra("owner_id");
            
            Log.d(TAG, "Dados carregados - ID: " + idInstrumento + ", Nome: " + nomeInstrumento + 
                      ", Preço: " + precoPorDia + ", Proprietário: " + idProprietario);
            
            // Verificar se o usuário não está tentando solicitar seu próprio instrumento
            if (usuarioAtual.getUid().equals(idProprietario)) {
                Log.d(TAG, "Usuário tentando solicitar seu próprio instrumento");
                Toast.makeText(this, "Você não pode solicitar reserva do seu próprio instrumento", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Atualizar interface
            instrumentNameTextView.setText(nomeInstrumento);
            instrumentPriceTextView.setText(String.format("R$ %.2f/dia", precoPorDia));
            
            // TODO: Carregar categoria do instrumento do Firebase se necessário
            
        } else {
            Log.e(TAG, "Intent é null");
            Toast.makeText(this, "Erro ao carregar dados do instrumento", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Configura os listeners dos componentes da interface
     * 
     * Define as ações para seleção de datas, validação de períodos
     * e submissão da solicitação.
     */
    private void configurarListeners() {
        Log.d(TAG, "Configurando listeners");
        
        // Listener para seleção de data de início
        startDateTextView.setOnClickListener(v -> selecionarDataInicio());
        
        // Listener para seleção de data de fim
        endDateTextView.setOnClickListener(v -> selecionarDataFim());
        
        // Listener para botão de solicitar
        solicitarButton.setOnClickListener(v -> solicitarReserva());
    }

    /**
     * Abre o seletor de data para a data de início
     * 
     * Configura e exibe um DatePickerDialog para seleção da data de início,
     * validando que a data seja futura e anterior à data de fim (se já selecionada).
     */
    private void selecionarDataInicio() {
        Log.d(TAG, "Abrindo seletor de data de início");
        
        Calendar calendario = Calendar.getInstance();
        calendario.add(Calendar.DAY_OF_MONTH, 1); // Mínimo: amanhã
        
        Calendar calendarioMaximo = null;
        if (dataFim != null) {
            calendarioMaximo = Calendar.getInstance();
            calendarioMaximo.setTime(dataFim);
            calendarioMaximo.add(Calendar.DAY_OF_MONTH, -1); // Máximo: um dia antes da data de fim
        }
        
        // Determinar título baseado na disponibilidade
        String titulo = "Selecionar Data de Início";
        if (faixasIndisponiveis != null && !faixasIndisponiveis.isEmpty()) {
            titulo = "Selecionar Data de Início\n⚠️ Datas indisponíveis serão bloqueadas";
        }
        
        // Criar validador que combina data futura + datas indisponíveis
        CalendarConstraints.DateValidator validadorCombinado;
        if (validadorDatas != null) {
            // Combinar validação de data futura com datas indisponíveis
            DateValidatorPointForward validadorFuturo = DateValidatorPointForward.now();
            validadorCombinado = new CalendarConstraints.DateValidator() {
                @Override
                public boolean isValid(long date) {
                    return validadorFuturo.isValid(date) && validadorDatas.isValid(date);
                }
                
                @Override
                public int describeContents() {
                    return 0;
                }
                
                @Override
                public void writeToParcel(android.os.Parcel dest, int flags) {
                    // Implementação vazia para Parcelable
                }
            };
        } else {
            // Fallback para apenas validação de data futura
            validadorCombinado = DateValidatorPointForward.now();
        }
        
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(titulo)
                .setSelection(calendario.getTimeInMillis())
                .setCalendarConstraints(
                        new CalendarConstraints.Builder()
                                .setStart(calendario.getTimeInMillis())
                                .setEnd(calendarioMaximo != null ? calendarioMaximo.getTimeInMillis() : 
                                        calendario.getTimeInMillis() + (365L * 24 * 60 * 60 * 1000)) // 1 ano
                                .setValidator(validadorCombinado)
                                .build()
                )
                .build();
        
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Date dataSelecionada = new Date(selection);
            
            // Verificar se a data selecionada está indisponível
            if (isDataIndisponivel(dataSelecionada)) {
                Log.w(TAG, "Tentativa de selecionar data indisponível: " + dataSelecionada);
                
                // Mostrar mensagem mais específica
                SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dataFormatada = formato.format(dataSelecionada);
                
                Toast.makeText(this, 
                    "❌ Data " + dataFormatada + " não está disponível.\n" +
                    "Esta data já foi reservada ou está bloqueada.\n" +
                    "Selecione uma data disponível.", 
                    Toast.LENGTH_LONG).show();
                return;
            }
            
            dataInicio = dataSelecionada;
            Log.d(TAG, "Data de início selecionada: " + dataInicio);
            atualizarInterfaceDataInicio();
            validarPeriodoCompleto();
        });
        
        datePicker.show(getSupportFragmentManager(), "DATA_INICIO");
    }

    /**
     * Abre o seletor de data para a data de fim
     * 
     * Configura e exibe um DatePickerDialog para seleção da data de fim,
     * validando que a data seja posterior à data de início (se já selecionada).
     */
    private void selecionarDataFim() {
        Log.d(TAG, "Abrindo seletor de data de fim");
        
        Calendar calendario = Calendar.getInstance();
        if (dataInicio != null) {
            calendario.setTime(dataInicio);
            calendario.add(Calendar.DAY_OF_MONTH, 1); // Mínimo: um dia após a data de início
        } else {
            calendario.add(Calendar.DAY_OF_MONTH, 2); // Mínimo: depois de amanhã
        }
        
        // Determinar título baseado na disponibilidade
        String titulo = "Selecionar Data de Fim";
        if (faixasIndisponiveis != null && !faixasIndisponiveis.isEmpty()) {
            titulo = "Selecionar Data de Fim\n⚠️ Datas indisponíveis serão bloqueadas";
        }
        
        // Criar validador que combina data futura + datas indisponíveis
        CalendarConstraints.DateValidator validadorCombinado;
        if (validadorDatas != null) {
            // Combinar validação de data futura com datas indisponíveis
            DateValidatorPointForward validadorFuturo = DateValidatorPointForward.now();
            validadorCombinado = new CalendarConstraints.DateValidator() {
                @Override
                public boolean isValid(long date) {
                    return validadorFuturo.isValid(date) && validadorDatas.isValid(date);
                }
                
                @Override
                public int describeContents() {
                    return 0;
                }
                
                @Override
                public void writeToParcel(android.os.Parcel dest, int flags) {
                    // Implementação vazia para Parcelable
                }
            };
        } else {
            // Fallback para apenas validação de data futura
            validadorCombinado = DateValidatorPointForward.now();
        }
        
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(titulo)
                .setSelection(calendario.getTimeInMillis())
                .setCalendarConstraints(
                        new CalendarConstraints.Builder()
                                .setStart(calendario.getTimeInMillis())
                                .setEnd(calendario.getTimeInMillis() + (365L * 24 * 60 * 60 * 1000)) // 1 ano
                                .setValidator(validadorCombinado)
                                .build()
                )
                .build();
        
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Date dataSelecionada = new Date(selection);
            
            // Verificar se a data selecionada está indisponível
            if (isDataIndisponivel(dataSelecionada)) {
                Log.w(TAG, "Tentativa de selecionar data indisponível: " + dataSelecionada);
                
                // Mostrar mensagem mais específica
                SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dataFormatada = formato.format(dataSelecionada);
                
                Toast.makeText(this, 
                    "❌ Data " + dataFormatada + " não está disponível.\n" +
                    "Esta data já foi reservada ou está bloqueada.\n" +
                    "Selecione uma data disponível.", 
                    Toast.LENGTH_LONG).show();
                return;
            }
            
            dataFim = dataSelecionada;
            Log.d(TAG, "Data de fim selecionada: " + dataFim);
            atualizarInterfaceDataFim();
            validarPeriodoCompleto();
        });
        
        datePicker.show(getSupportFragmentManager(), "DATA_FIM");
    }

    /**
     * Atualiza a interface com a data de início selecionada
     */
    private void atualizarInterfaceDataInicio() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        startDateTextView.setText(formato.format(dataInicio));
        startDateTextView.setBackgroundResource(R.drawable.button_selected_background);
        startDateTextView.setTextColor(getResources().getColor(android.R.color.white));
        
        Log.d(TAG, "Data de início selecionada: " + formato.format(dataInicio));
    }

    /**
     * Atualiza a interface com a data de fim selecionada
     */
    private void atualizarInterfaceDataFim() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        endDateTextView.setText(formato.format(dataFim));
        endDateTextView.setBackgroundResource(R.drawable.button_selected_background);
        endDateTextView.setTextColor(getResources().getColor(android.R.color.white));
        
        Log.d(TAG, "Data de fim selecionada: " + formato.format(dataFim));
    }

    /**
     * Valida se o período está completo e calcula o preço total
     * 
     * Verifica se ambas as datas foram selecionadas e se o período é válido,
     * calculando automaticamente o preço total e atualizando a interface.
     */
    private void validarPeriodoCompleto() {
        if (dataInicio != null && dataFim != null) {
            Log.d(TAG, "Validando período completo");
            Log.d(TAG, "Data início: " + dataInicio + ", Data fim: " + dataFim);
            Log.d(TAG, "Preço por dia: " + precoPorDia);
            
            // Calcular número de dias
            long diferencaMillis = dataFim.getTime() - dataInicio.getTime();
            long numeroDias = diferencaMillis / (24 * 60 * 60 * 1000);
            Log.d(TAG, "Número de dias: " + numeroDias);
            
            if (numeroDias > 0) {
                // Verificar se o período não conflita com datas indisponíveis
                if (verificarConflitoPeriodo(dataInicio, dataFim)) {
                    Log.w(TAG, "Período selecionado conflita com datas indisponíveis");
                    Toast.makeText(this, "Este período contém datas indisponíveis. Selecione outro período.", Toast.LENGTH_LONG).show();
                    periodSummaryLayout.setVisibility(android.view.View.GONE);
                    totalLayout.setVisibility(android.view.View.GONE);
                    solicitarButton.setEnabled(false);
                    return;
                }
                
                // Calcular preço total
                precoTotal = numeroDias * precoPorDia;
                Log.d(TAG, "Preço total calculado: " + precoTotal);
                
                // Atualizar interface
                SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                periodSummaryTextView.setText(String.format("Período: %s a %s (%d dias)", 
                    formato.format(dataInicio), formato.format(dataFim), numeroDias));
                totalPriceTextView.setText(String.format("R$ %.2f", precoTotal));
                
                periodSummaryLayout.setVisibility(android.view.View.VISIBLE);
                totalLayout.setVisibility(android.view.View.VISIBLE);
                solicitarButton.setEnabled(true);
                
                Log.d(TAG, "Período válido - " + numeroDias + " dias, Total: R$ " + precoTotal);
            } else {
                Log.w(TAG, "Período inválido - data de fim deve ser posterior à data de início");
                Toast.makeText(this, "A data de fim deve ser posterior à data de início", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Processa a solicitação de reserva
     * 
     * Valida os dados da solicitação, verifica se não há conflitos
     * e cria a solicitação no Firebase.
     */
    private void solicitarReserva() {
        Log.d(TAG, "=== PROCESSANDO SOLICITAÇÃO DE RESERVA ===");
        Log.d(TAG, "Usuário atual: " + (usuarioAtual != null ? usuarioAtual.getUid() : "null"));
        Log.d(TAG, "ID do proprietário: " + idProprietario);
        Log.d(TAG, "ID do instrumento: " + idInstrumento);
        
        // Validar se o período está completo
        if (dataInicio == null || dataFim == null) {
            Log.e(TAG, "Período não está completo - dataInicio: " + dataInicio + ", dataFim: " + dataFim);
            Toast.makeText(this, "Selecione o período completo", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Período selecionado: " + dataInicio + " a " + dataFim);
        
        // Verificar se o usuário não está tentando solicitar seu próprio instrumento
        if (usuarioAtual.getUid().equals(idProprietario)) {
            Toast.makeText(this, "Você não pode solicitar reserva do seu próprio instrumento", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Desabilitar botão para evitar múltiplas solicitações
        solicitarButton.setEnabled(false);
        solicitarButton.setText("Processando...");
        
        // Criar objeto de solicitação
        com.example.instrumentaliza.models.FirebaseSolicitacao solicitacao = new com.example.instrumentaliza.models.FirebaseSolicitacao(
            usuarioAtual.getUid(),
            idProprietario,
            idInstrumento,
            nomeInstrumento,
            usuarioAtual.getDisplayName() != null ? usuarioAtual.getDisplayName() : "Usuário",
            usuarioAtual.getEmail(),
            "", // Telefone - pode ser obtido do perfil se necessário
            dataInicio,
            dataFim,
            precoTotal,
            observationsEditText.getText().toString().trim()
        );
        
        // Log para debug
        Log.d(TAG, "Iniciando criação de solicitação...");
        Log.d(TAG, "Dados da solicitação: " + solicitacao.toString());
        
        // Criar solicitação no Firebase
        GerenciadorFirebase.criarSolicitacaoReserva(solicitacao)
                .thenAccept(idSolicitacao -> {
                    // Executar na thread principal
                    runOnUiThread(() -> {
                        Log.d(TAG, "Solicitação criada com sucesso: " + idSolicitacao);
                        Toast.makeText(this, "Solicitação enviada com sucesso!", Toast.LENGTH_SHORT).show();
                        
                        // Retornar para a tela anterior
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .exceptionally(erro -> {
                    // Executar na thread principal
                    runOnUiThread(() -> {
                        Log.e(TAG, "Erro ao criar solicitação: " + erro.getMessage(), erro);
                        Log.e(TAG, "Stack trace completo: ", erro);
                        
                        String mensagemErro = "Erro ao enviar solicitação";
                        if (erro.getCause() != null) {
                            mensagemErro += ": " + erro.getCause().getMessage();
                        } else if (erro.getMessage() != null) {
                            mensagemErro += ": " + erro.getMessage();
                        }
                        
                        Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show();
                        
                        // Reabilitar botão
                        solicitarButton.setEnabled(true);
                        solicitarButton.setText("Solicitar Reserva");
                    });
                    return null;
                });
    }

    /**
     * Manipula a seleção de itens do menu (navegação)
     * 
     * @param item Item do menu selecionado
     * @return true se o item foi tratado, false caso contrário
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "Botão voltar pressionado");
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Manipula o botão voltar
     * 
     * Retorna para a tela anterior com resultado cancelado.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed chamado");
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
