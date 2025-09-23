package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instrumentaliza.GerenciadorFirebase;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeVisualizarDisponibilidadeInstrumento - Tela de visualização de disponibilidade
 * 
 * Esta tela permite que usuários interessados em alugar um instrumento visualizem
 * os períodos em que o instrumento está indisponível, através de um calendário
 * interativo e uma lista de faixas de datas.
 * 
 * Funcionalidades principais:
 * - Exibição do calendário mensal com dias indisponíveis marcados
 * - Lista detalhada de períodos indisponíveis
 * - Navegação entre meses (anterior/próximo)
 * - Visualização do nome do instrumento
 * - Estado vazio quando não há períodos indisponíveis
 * 
 * Características técnicas:
 * - GridLayoutManager para calendário
 * - LinearLayoutManager para lista de períodos
 * - Carregamento assíncrono de dados do Firebase
 * - Tratamento de timestamps Firebase
 * - Formatação de datas localizadas
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeVisualizarDisponibilidadeInstrumento extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "VisualizarDisponibilidade";
    
    // Dados do instrumento
    private String idInstrumento;
    private String nomeInstrumento;
    
    // Componentes da interface
    private RecyclerView listaFaixasIndisponiveis;
    private RecyclerView listaCalendario;
    private TextView textoNomeInstrumento;
    private TextView textoMesAtual;
    private TextView textoEstadoVazio;
    private View layoutEstadoVazio;
    
    // Dados de disponibilidade
    private List<Map<String, Object>> faixasIndisponiveis;
    private Calendar mesAtual;
    
    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de visualização de disponibilidade, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização de todos os componentes da interface
     * - Validação do ID do instrumento recebido
     * - Configuração dos RecyclerViews para calendário e lista
     * - Carregamento dos dados de disponibilidade
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_instrument_availability);
        
        // Obter dados do Intent
        idInstrumento = getIntent().getStringExtra("instrument_id");
        nomeInstrumento = getIntent().getStringExtra("instrument_name");
        
        if (idInstrumento == null) {
            Toast.makeText(this, getString(R.string.error_id_not_provided), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.availability_title));
        }
        
        // Inicializar views
        textoNomeInstrumento = findViewById(R.id.instrumentNameTextView);
        listaCalendario = findViewById(R.id.calendarRecyclerView);
        textoMesAtual = findViewById(R.id.currentMonthText);
        listaFaixasIndisponiveis = findViewById(R.id.unavailableRangesRecyclerView);
        layoutEstadoVazio = findViewById(R.id.emptyStateLayout);
        textoEstadoVazio = findViewById(R.id.emptyStateText);
        
        // Configurar nome do instrumento
        if (nomeInstrumento != null) {
            textoNomeInstrumento.setText(nomeInstrumento);
        }
        
        // Configurar RecyclerViews
        listaFaixasIndisponiveis.setLayoutManager(new LinearLayoutManager(this));
        
        // Configurar calendário
        configurarCalendario();
        
        // Configurar navegação do calendário
        configurarNavegacaoCalendario();
        
        // Carregar dados de disponibilidade
        carregarDadosDisponibilidade();
    }
    
    private void configurarCalendario() {
        mesAtual = Calendar.getInstance();
        
        // Configurar GridLayoutManager para 7 colunas (dias da semana)
        GridLayoutManager layoutManager = new GridLayoutManager(this, 7);
        listaCalendario.setLayoutManager(layoutManager);
        
        // Criar lista de datas para o mês atual
        atualizarDatasCalendario();
    }
    
    private void configurarNavegacaoCalendario() {
        // Botão mês anterior
        findViewById(R.id.previousMonthButton).setOnClickListener(v -> {
            mesAtual.add(Calendar.MONTH, -1);
            atualizarDatasCalendario();
            atualizarTextoMesAtual();
        });
        
        // Botão próximo mês
        findViewById(R.id.nextMonthButton).setOnClickListener(v -> {
            mesAtual.add(Calendar.MONTH, 1);
            atualizarDatasCalendario();
            atualizarTextoMesAtual();
        });
        
        // Atualizar texto do mês atual
        atualizarTextoMesAtual();
    }
    
    private void atualizarTextoMesAtual() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("pt", "BR"));
        String monthText = monthFormat.format(mesAtual.getTime());
        // Capitalizar primeira letra
        monthText = monthText.substring(0, 1).toUpperCase() + monthText.substring(1);
        textoMesAtual.setText(monthText);
    }
    
    private void atualizarDatasCalendario() {
        List<ItemDataCalendario> calendarDates = new ArrayList<>();
        
        // Adicionar dias da semana como cabeçalhos
        String[] weekDays = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        for (String day : weekDays) {
            calendarDates.add(new ItemDataCalendario(day, true, false)); // isHeader = true
        }
        
        // Obter primeiro dia do mês
        Calendar firstDay = (Calendar) mesAtual.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        
        // Obter último dia do mês
        Calendar lastDay = (Calendar) mesAtual.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, lastDay.getActualMaximum(Calendar.DAY_OF_MONTH));
        
        // Adicionar espaços vazios para alinhar com o primeiro dia da semana
        int firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Domingo
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarDates.add(new ItemDataCalendario("", false, false));
        }
        
        // Adicionar todos os dias do mês
        Calendar current = (Calendar) firstDay.clone();
        while (!current.after(lastDay)) {
            boolean isUnavailable = isDateUnavailable(current.getTime());
            calendarDates.add(new ItemDataCalendario(
                String.valueOf(current.get(Calendar.DAY_OF_MONTH)),
                false,
                isUnavailable
            ));
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Configurar adapter do calendário
        AdaptadorCalendario calendarAdapter = new AdaptadorCalendario(calendarDates);
        listaCalendario.setAdapter(calendarAdapter);
    }
    
    private boolean isDateUnavailable(Date date) {
        if (faixasIndisponiveis == null || faixasIndisponiveis.isEmpty()) {
            return false;
        }
        
        Calendar checkDate = Calendar.getInstance();
        checkDate.setTime(date);
        checkDate.set(Calendar.HOUR_OF_DAY, 0);
        checkDate.set(Calendar.MINUTE, 0);
        checkDate.set(Calendar.SECOND, 0);
        checkDate.set(Calendar.MILLISECOND, 0);
        
        for (Map<String, Object> range : faixasIndisponiveis) {
            Object startObj = range.get("startDate");
            Object endObj = range.get("endDate");
            
            if (startObj instanceof Timestamp && endObj instanceof Timestamp) {
                Calendar startDate = Calendar.getInstance();
                startDate.setTime(((Timestamp) startObj).toDate());
                startDate.set(Calendar.HOUR_OF_DAY, 0);
                startDate.set(Calendar.MINUTE, 0);
                startDate.set(Calendar.SECOND, 0);
                startDate.set(Calendar.MILLISECOND, 0);
                
                Calendar endDate = Calendar.getInstance();
                endDate.setTime(((Timestamp) endObj).toDate());
                endDate.set(Calendar.HOUR_OF_DAY, 23);
                endDate.set(Calendar.MINUTE, 59);
                endDate.set(Calendar.SECOND, 59);
                endDate.set(Calendar.MILLISECOND, 999);
                
                if (checkDate.after(startDate) && checkDate.before(endDate) || 
                    checkDate.equals(startDate) || checkDate.equals(endDate)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void carregarDadosDisponibilidade() {
        // Carregar faixas indisponíveis usando GerenciadorFirebase
        GerenciadorFirebase.obterFaixasIndisponiveisInstrumento(idInstrumento)
                .thenAccept(faixas -> {
                    runOnUiThread(() -> {
                        faixasIndisponiveis = faixas;
                        
                        if (faixas != null && !faixas.isEmpty()) {
                            // Há datas indisponíveis
                            layoutEstadoVazio.setVisibility(View.GONE);
                            listaFaixasIndisponiveis.setVisibility(View.VISIBLE);
                            
                            // Configurar adapter
                            AdaptadorFaixasIndisponiveisView adapter = new AdaptadorFaixasIndisponiveisView(faixas);
                            listaFaixasIndisponiveis.setAdapter(adapter);
                            
                            // Atualizar calendário para mostrar datas indisponíveis
                            atualizarDatasCalendario();
                        } else {
                            // Não há datas indisponíveis
                            layoutEstadoVazio.setVisibility(View.VISIBLE);
                            listaFaixasIndisponiveis.setVisibility(View.GONE);
                            textoEstadoVazio.setText("Este instrumento está disponível para todas as datas!");
                            
                            // Atualizar calendário
                            atualizarDatasCalendario();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar disponibilidade: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return null;
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Classe para representar um item do calendário
    private static class ItemDataCalendario {
        String text;
        boolean isHeader;
        boolean isUnavailable;
        
        ItemDataCalendario(String text, boolean isHeader, boolean isUnavailable) {
            this.text = text;
            this.isHeader = isHeader;
            this.isUnavailable = isUnavailable;
        }
    }
    
    // Adapter para o calendário
    private static class AdaptadorCalendario extends RecyclerView.Adapter<AdaptadorCalendario.ViewHolderCalendario> {
        private List<ItemDataCalendario> datas;
        
        public AdaptadorCalendario(List<ItemDataCalendario> datas) {
            this.datas = datas;
        }
        
        @Override
        public ViewHolderCalendario onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_date, parent, false);
            return new ViewHolderCalendario(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolderCalendario holder, int position) {
            ItemDataCalendario item = datas.get(position);
            
            holder.textoData.setText(item.text);
            
            if (item.isHeader) {
                // Cabeçalho dos dias da semana
                holder.textoData.setBackgroundResource(R.drawable.calendar_date_background);
                holder.textoData.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_category_brown));
                holder.textoData.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (item.text.isEmpty()) {
                // Espaço vazio
                holder.textoData.setBackgroundResource(android.R.color.transparent);
                holder.textoData.setText("");
            } else if (item.isUnavailable) {
                // Data indisponível
                holder.textoData.setBackgroundResource(R.drawable.calendar_date_unavailable_background);
                holder.textoData.setTextColor(android.graphics.Color.WHITE);
                holder.textoData.setText(item.text + " ✗"); // Adicionar X para indicar indisponível
            } else {
                // Data disponível
                holder.textoData.setBackgroundResource(R.drawable.calendar_date_background);
                holder.textoData.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_category_brown));
            }
        }
        
        @Override
        public int getItemCount() {
            return datas.size();
        }
        
        static class ViewHolderCalendario extends RecyclerView.ViewHolder {
            TextView textoData;
            
            ViewHolderCalendario(View itemView) {
                super(itemView);
                textoData = itemView.findViewById(R.id.dateText);
            }
        }
    }
    
    // Adapter para visualização das faixas indisponíveis (somente leitura)
    private static class AdaptadorFaixasIndisponiveisView extends RecyclerView.Adapter<AdaptadorFaixasIndisponiveisView.ViewHolder> {
        private List<Map<String, Object>> faixas;
        
        public AdaptadorFaixasIndisponiveisView(List<Map<String, Object>> faixas) {
            this.faixas = faixas;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_unavailable_date_range, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> faixa = faixas.get(position);
            
            Object startObj = faixa.get("startDate");
            Object endObj = faixa.get("endDate");
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            
            String startStr = holder.itemView.getContext().getString(R.string.label_data_inicial);
            String endStr = holder.itemView.getContext().getString(R.string.label_data_final);
            
            if (startObj instanceof Timestamp) {
                startStr += sdf.format(((Timestamp) startObj).toDate());
            }
            if (endObj instanceof Timestamp) {
                endStr += sdf.format(((Timestamp) endObj).toDate());
            }
            
            holder.startDateText.setText(startStr);
            holder.endDateText.setText(endStr);
        }
        
        @Override
        public int getItemCount() {
            return faixas.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView startDateText;
            TextView endDateText;
            
            ViewHolder(View itemView) {
                super(itemView);
                startDateText = itemView.findViewById(R.id.startDateText);
                endDateText = itemView.findViewById(R.id.endDateText);
            }
        }
    }
}
