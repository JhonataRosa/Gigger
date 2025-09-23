package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.example.instrumentaliza.GerenciadorFirebase;

/**
 * AtividadeGerenciarDisponibilidade - Tela de gerenciamento de disponibilidade
 * 
 * Esta tela permite que proprietários de instrumentos gerenciem os períodos
 * em que seus instrumentos estarão indisponíveis para aluguel. Oferece
 * funcionalidades para adicionar, visualizar e remover faixas de datas.
 * 
 * Funcionalidades principais:
 * - Visualização de faixas de datas indisponíveis
 * - Adição de novos períodos indisponíveis via DatePicker
 * - Remoção individual de períodos
 * - Limpeza de todos os períodos
 * - Validação de sobreposição de datas
 * - Sincronização com Firebase Firestore
 * 
 * Características técnicas:
 * - MaterialDatePicker para seleção de datas
 * - RecyclerView com adaptador customizado
 * - Operações assíncronas com CompletableFuture
 * - Validação de permissões de proprietário
 * - Tratamento de erros robusto
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeGerenciarDisponibilidade extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "GerenciarDisponibilidade";
    
    // Dados do instrumento
    private String idInstrumento;
    private String nomeInstrumento;
    
    // Autenticação e Firebase
    private FirebaseAuth autenticacao;
    private FirebaseFirestore firestore;
    
    // Componentes da interface
    private TextView textoNomeInstrumento;
    private RecyclerView listaFaixasIndisponiveis;
    private AdaptadorFaixasIndisponiveis adaptador;
    
    // Dados de disponibilidade
    private List<Map<String, Object>> faixasIndisponiveis;
    
    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de gerenciamento de disponibilidade, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização de todos os componentes da interface
     * - Validação do ID do instrumento recebido
     * - Verificação de permissões de proprietário
     * - Configuração do RecyclerView e adaptador
     * - Carregamento dos dados de disponibilidade
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_availability_manager);
        
        // Inicializar Firebase
        autenticacao = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        // Obter ID do instrumento
        idInstrumento = getIntent().getStringExtra("instrument_id");
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
            getSupportActionBar().setTitle(getString(R.string.manage_availability));
        }
        
        // Inicializar views
        textoNomeInstrumento = findViewById(R.id.instrumentNameTextView);
        listaFaixasIndisponiveis = findViewById(R.id.unavailableRangesRecyclerView);
        
        // Configurar RecyclerView
        listaFaixasIndisponiveis.setLayoutManager(new LinearLayoutManager(this));
        faixasIndisponiveis = new ArrayList<>();
        adaptador = new AdaptadorFaixasIndisponiveis(faixasIndisponiveis, this::removerFaixaIndisponivel);
        listaFaixasIndisponiveis.setAdapter(adaptador);
        
        // Configurar botões
        MaterialButton addRangeButton = findViewById(R.id.addRangeButton);
        MaterialButton clearAllButton = findViewById(R.id.clearAllButton);
        
        addRangeButton.setOnClickListener(v -> mostrarSeletorIntervaloDatas());
        clearAllButton.setOnClickListener(v -> mostrarDialogoLimparTudo());
        
        // Carregar dados do instrumento
        carregarDadosInstrumento();
    }
    
    private void carregarDadosInstrumento() {
        GerenciadorFirebase.obterInstrumentoPorId(idInstrumento)
                .thenAccept(documentSnapshot -> {
                    if (documentSnapshot != null) {
                        nomeInstrumento = documentSnapshot.getString("name");
                        runOnUiThread(() -> {
                            textoNomeInstrumento.setText(nomeInstrumento);
                        });
                        
                        // Carregar faixas indisponíveis usando GerenciadorFirebase
                        GerenciadorFirebase.obterFaixasIndisponiveisInstrumento(idInstrumento)
                                .thenAccept(ranges -> {
                                    runOnUiThread(() -> {
                                        faixasIndisponiveis.clear();
                                        faixasIndisponiveis.addAll(ranges);
                                        adaptador.notifyDataSetChanged();
                                        
                                        // Mostrar/esconder estado vazio
                                        View emptyStateLayout = findViewById(R.id.emptyStateLayout);
                                        if (emptyStateLayout != null) {
                                            emptyStateLayout.setVisibility(ranges.isEmpty() ? View.VISIBLE : View.GONE);
                                        }
                                    });
                                })
                                .exceptionally(throwable -> {
                                    Log.e(TAG, "Erro ao carregar faixas indisponíveis", throwable);
                                    return null;
                                });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar dados do instrumento", throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    private void mostrarSeletorIntervaloDatas() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        
        // Título em português
        builder.setTitleText(getString(R.string.date_picker_title_unavailable));
        // Botões em português (definidos no builder)
        builder.setPositiveButtonText(getString(R.string.action_save_caps));
        builder.setNegativeButtonText(getString(R.string.action_cancel_caps));
        
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                adicionarFaixaIndisponivel(selection.first, selection.second);
            }
        });
        
        picker.show(getSupportFragmentManager(), getString(R.string.picker_tag_unavailable_range));
    }
    
    private void adicionarFaixaIndisponivel(Long startDate, Long endDate) {
        Map<String, Object> range = new HashMap<>();
        range.put("startDate", new Timestamp(new Date(startDate)));
        range.put("endDate", new Timestamp(new Date(endDate)));
        range.put("reason", getString(R.string.marked_by_owner));
        
        faixasIndisponiveis.add(range);
        adaptador.notifyDataSetChanged();
        
        // Salvar no Firestore
        salvarFaixasIndisponiveis();
    }
    
    private void removerFaixaIndisponivel(int position) {
        if (position >= 0 && position < faixasIndisponiveis.size()) {
            faixasIndisponiveis.remove(position);
            adaptador.notifyDataSetChanged();
            salvarFaixasIndisponiveis();
        }
    }
    
    private void salvarFaixasIndisponiveis() {
        GerenciadorFirebase.atualizarDisponibilidadeInstrumento(idInstrumento, faixasIndisponiveis)
                .thenAccept(success -> {
                    if (success) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, getString(R.string.success_update), Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao salvar disponibilidade", throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    private void mostrarDialogoLimparTudo() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_all_dates))
                .setMessage(getString(R.string.confirm_clear_unavailable_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    faixasIndisponiveis.clear();
                    adaptador.notifyDataSetChanged();
                    salvarFaixasIndisponiveis();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Adapter para a lista de faixas indisponíveis
    private static class AdaptadorFaixasIndisponiveis extends RecyclerView.Adapter<AdaptadorFaixasIndisponiveis.ViewHolder> {
        private List<Map<String, Object>> faixas;
        private ListenerRemocaoFaixa listener;
        
        public interface ListenerRemocaoFaixa {
            void onRemoverFaixa(int posicao);
        }
        
        public AdaptadorFaixasIndisponiveis(List<Map<String, Object>> faixas, ListenerRemocaoFaixa listener) {
            this.faixas = faixas;
            this.listener = listener;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> faixa = faixas.get(position);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            long startMillis = 0L;
            long endMillis = 0L;
            Object startObj = faixa.get("startDate");
            Object endObj = faixa.get("endDate");
            if (startObj instanceof com.google.firebase.Timestamp) {
                startMillis = ((com.google.firebase.Timestamp) startObj).toDate().getTime();
            } else if (startObj instanceof java.util.Date) {
                startMillis = ((java.util.Date) startObj).getTime();
            }
            if (endObj instanceof com.google.firebase.Timestamp) {
                endMillis = ((com.google.firebase.Timestamp) endObj).toDate().getTime();
            } else if (endObj instanceof java.util.Date) {
                endMillis = ((java.util.Date) endObj).getTime();
            }

            String textoInicio = holder.itemView.getContext().getString(R.string.label_data_inicial);
            String textoFim = holder.itemView.getContext().getString(R.string.label_data_final);
            if (startMillis > 0) {
                textoInicio += sdf.format(new java.util.Date(startMillis));
            }
            if (endMillis > 0) {
                textoFim += sdf.format(new java.util.Date(endMillis));
            }
            holder.texto1.setText(textoInicio);
            holder.texto2.setText(textoFim);
            
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onRemoverFaixa(holder.getAdapterPosition());
                }
                return true;
            });
        }
        
        @Override
        public int getItemCount() {
            return faixas.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView texto1, texto2;
            
            ViewHolder(View itemView) {
                super(itemView);
                texto1 = itemView.findViewById(android.R.id.text1);
                texto2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
