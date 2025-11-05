package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageButton;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.text.Normalizer;
import android.view.View;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseUser;
import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import java.util.Date;
import com.google.firebase.Timestamp;
import java.util.Locale;

/**
 * AtividadeInstrumentos - Tela principal de listagem de instrumentos
 * 
 * Esta é a tela principal do aplicativo após o login, onde os usuários podem:
 * - Visualizar todos os instrumentos disponíveis
 * - Buscar instrumentos por nome/descrição
 * - Filtrar por categoria (Cordas, Teclas, Percussão, Sopro, Acessórios)
 * - Filtrar por disponibilidade em períodos específicos
 * - Ordenar por preço (crescente/decrescente)
 * - Adicionar/remover instrumentos dos favoritos
 * - Navegar para detalhes, edição ou exclusão de instrumentos
 * - Acessar menu lateral com opções do usuário
 * 
 * Funcionalidades principais:
 * - Sistema de busca em tempo real
 * - Filtros múltiplos (categoria + data + busca)
 * - Ordenação dinâmica por preço
 * - Gerenciamento de favoritos
 * - Navegação lateral com perfil do usuário
 * - Integração completa com Firebase Firestore
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeInstrumentos extends AppCompatActivity implements AdaptadorInstrumentoFirebase.OnInstrumentClickListener {
    
    // Componentes da interface
    private RecyclerView listaInstrumentos;
    private AdaptadorInstrumentoFirebase adaptadorInstrumento;
    private TextInputEditText campoBusca;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    
    // Filtros e busca
    private String consultaAtual = "";
    private String categoriaAtual = "";
    private boolean ordenarPorPrecoCrescente = false;
    private Long dataInicioFiltroUtc = null;
    private Long dataFimFiltroUtc = null;
    
    // Constantes
    private static final String TAG = "AtividadeInstrumentos";
    
    // Sistema de notificações
    private MenuItem menuItemRequests;
    private boolean hasUnreadRequests = false;

    /**
     * Normaliza uma string de categoria removendo acentos e convertendo para primeira letra maiúscula
     * 
     * Utilizado para comparação de categorias independente de acentuação.
     * Remove todos os caracteres diacríticos (acentos) e converte para primeira letra maiúscula.
     * Esta função deve ser idêntica à usada em AdicionarInstrumentoActivity para manter consistência.
     * 
     * @param input String de entrada a ser normalizada
     * @return String normalizada sem acentos e com primeira letra maiúscula
     */
    private static String normalizarCategoria(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        if (!normalized.isEmpty()) {
            normalized = normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase();
        }
        Log.d("FiltroCategoria", "normalizarCategoria: '" + input + "' -> '" + normalized + "'");
        return normalized;
    }

    /**
     * Converte diferentes tipos de objetos de data para milissegundos
     * 
     * Suporta conversão de Long, Integer, Date, Timestamp e Firebase Timestamp
     * para um valor long em milissegundos. Utilizado para comparação de datas
     * nos filtros de disponibilidade.
     * 
     * @param value Objeto de data a ser convertido
     * @return Valor em milissegundos ou Long.MIN_VALUE se não for possível converter
     */
    private static long toMillis(Object value) {
        if (value == null) return Long.MIN_VALUE;
        if (value instanceof Long) return (Long) value;
        if (value instanceof java.lang.Integer) return ((Integer) value).longValue();
        if (value instanceof Date) return ((Date) value).getTime();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof com.google.firebase.Timestamp)
            return ((com.google.firebase.Timestamp) value).toDate().getTime();
        return Long.MIN_VALUE;
    }

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura toda a interface da tela principal de instrumentos, incluindo:
     * - Verificação de autenticação do usuário
     * - Configuração do menu lateral (NavigationView)
     * - Configuração da toolbar e navegação
     * - Configuração dos filtros (busca, categoria, data)
     * - Configuração da ordenação por preço
     * - Configuração do RecyclerView e adaptador
     * - Carregamento inicial dos instrumentos
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_instruments);

            // Inicializar Firebase ANTES de qualquer uso
            GerenciadorFirebase.inicializar(this);

            // Verificar se o usuário está logado usando GerenciadorFirebase
            if (!GerenciadorFirebase.usuarioEstaLogado()) {
                Log.d("InstrumentsActivity", "Usuário não logado, voltando para MainActivity");
                startActivity(new Intent(this, AtividadePrincipal.class));
                finish();
                return;
            }

            // Obter usuário atual
            autenticacao = FirebaseAuth.getInstance();
            Log.d("InstrumentsActivity", "Usuário logado: " + autenticacao.getCurrentUser().getEmail());

            // DrawerLayout e NavigationView
            DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
            NavigationView navigationView = findViewById(R.id.navigationView);

            // Configurar header do NavigationView
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                CircleImageView navProfileImageView = headerView.findViewById(R.id.navProfileImageView);
                TextView navUserName = headerView.findViewById(R.id.navUserName);
                TextView navUserEmail = headerView.findViewById(R.id.navUserEmail);
                MaterialButton navViewProfileButton = headerView.findViewById(R.id.navViewProfileButton);

                // Configurar dados do usuário no header
                FirebaseUser currentUser = autenticacao.getCurrentUser();
                if (currentUser != null) {
                    navUserEmail.setText(currentUser.getEmail());
                    
                    // Tentar carregar nome e foto do Firestore
                    GerenciadorFirebase.obterDadosUsuario(currentUser.getUid())
                            .thenAccept(userData -> {
                                Log.d(TAG, "Dados do usuário carregados: " + (userData != null ? userData.size() : "null"));
                                if (userData != null && !userData.isEmpty()) {
                                    String displayName = (String) userData.get("name");
                                    String profileImageUrl = (String) userData.get("profileImageUrl");
                                    
                                    Log.d(TAG, "Nome do usuário: " + displayName);
                                    Log.d(TAG, "URL da foto: " + profileImageUrl);
                                    
                                    runOnUiThread(() -> {
                                        if (displayName != null && !displayName.isEmpty()) {
                                            navUserName.setText(displayName);
                                        } else {
                                            navUserName.setText(getString(R.string.user_default_name));
                                        }
                                        
                                        // Carregar foto de perfil se existir
                                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                            Log.d(TAG, "Tentando carregar foto: " + profileImageUrl);
                                            try {
                                                // Usar Glide para carregar a imagem de forma assíncrona
                                                Glide.with(AtividadeInstrumentos.this)
                                                        .load(profileImageUrl)
                                                        .placeholder(R.drawable.ic_profile)
                                                        .error(R.drawable.ic_profile)
                                                        .listener(new RequestListener<Drawable>() {
                                                            @Override
                                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                                Log.e(TAG, "Erro ao carregar foto com Glide: " + e.getMessage());
                                                                navProfileImageView.setImageResource(R.drawable.ic_profile);
                                                                return false;
                                                            }

                                                            @Override
                                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                                Log.d(TAG, "Foto carregada com sucesso com Glide");
                                                                return false;
                                                            }
                                                        })
                                                        .into(navProfileImageView);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Erro ao carregar foto de perfil: " + e.getMessage());
                                                navProfileImageView.setImageResource(R.drawable.ic_profile);
                                            }
                                        } else {
                                            Log.d(TAG, "URL da foto está vazia, usando imagem padrão");
                                            navProfileImageView.setImageResource(R.drawable.ic_profile);
                                        }
                                    });
                                } else {
                                    Log.d(TAG, "Dados do usuário estão vazios");
                                    runOnUiThread(() -> {
                                        navUserName.setText(getString(R.string.user_default_name));
                                        navProfileImageView.setImageResource(R.drawable.ic_profile);
                                    });
                                }
                            })
                            .exceptionally(throwable -> {
                                Log.e(TAG, "Erro ao carregar dados do usuário: " + throwable.getMessage(), throwable);
                                runOnUiThread(() -> {
                                    navUserName.setText(getString(R.string.user_default_name));
                                    navProfileImageView.setImageResource(R.drawable.ic_profile);
                                });
                                return null;
                            });

                    // Configurar botão "Ver perfil"
                    navViewProfileButton.setOnClickListener(v -> {
                        drawerLayout.closeDrawers();
                        startActivity(new Intent(this, AtividadePerfil.class));
                    });
                }
            }

            // Configurar Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
                getSupportActionBar().setTitle(getString(R.string.instruments_title));
            }

            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    drawerLayout.closeDrawers();
                    // Voltar para MainActivity
                    startActivity(new Intent(this, AtividadePrincipal.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_instruments) {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(this, AtividadeMeusInstrumentos.class));
                    return true;
                } else if (id == R.id.nav_my_rentals) {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(this, AtividadeMinhasReservas.class));
                    return true;
                } else if (id == R.id.nav_requests) {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(this, AtividadeListaSolicitacoes.class));
                    return true;
                } else if (id == R.id.nav_favorites) {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(this, AtividadeFavoritos.class));
                    return true;
                } else if (id == R.id.nav_messages) {
                    drawerLayout.closeDrawers();
                    startActivity(new Intent(this, AtividadeListaChat.class));
                    return true;
                } else if (id == R.id.nav_help) {
                    drawerLayout.closeDrawers();
                    // TODO: Implementar tela de ajuda
                    Toast.makeText(this, getString(R.string.info_no_data), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_about) {
                    drawerLayout.closeDrawers();
                    // TODO: Implementar tela sobre
                    Toast.makeText(this, getString(R.string.info_no_data), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_logout) {
                    drawerLayout.closeDrawers();
                    // Parar notificações antes do logout
                    InstrumentalizaApplication.getInstance().pararNotificacoes();
                    GerenciadorFirebase.sair();
                    startActivity(new Intent(this, AtividadePrincipal.class));
                    finish();
                    return true;
                }
                return false;
            });

            // Configurar views
            campoBusca = findViewById(R.id.searchEditText);
            listaInstrumentos = findViewById(R.id.instrumentsRecyclerView);
            MaterialButton dateFilterButton = findViewById(R.id.dateFilterButton);
            MaterialButton clearDateFilterButton = findViewById(R.id.clearDateFilterButton);

            // Configurar RecyclerView
            listaInstrumentos.setLayoutManager(new LinearLayoutManager(this));
            adaptadorInstrumento = new AdaptadorInstrumentoFirebase(new ArrayList<>(), autenticacao.getCurrentUser().getUid(), this);
            listaInstrumentos.setAdapter(adaptadorInstrumento);

            // Configurar FAB
            FloatingActionButton addInstrumentFab = findViewById(R.id.addInstrumentFab);
            addInstrumentFab.setOnClickListener(v -> {
                Intent intent = new Intent(AtividadeInstrumentos.this, AdicionarInstrumentoActivity.class);
                startActivity(intent);
            });

            // Configurar busca
            campoBusca.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    consultaAtual = s.toString();
                    carregarInstrumentos();
                }
            });

            // Filtro por intervalo de datas usando Material Date Range Picker
            if (dateFilterButton != null) {
                dateFilterButton.setOnClickListener(v -> {
                    MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                            MaterialDatePicker.Builder.dateRangePicker();
                    
                    // Título em português
                    builder.setTitleText(getString(R.string.date_picker_title_period));
                    // Botões em português (definidos no builder)
                    builder.setPositiveButtonText(getString(R.string.action_save_caps));
                    builder.setNegativeButtonText(getString(R.string.action_cancel_caps));
                    
                    if (dataInicioFiltroUtc != null && dataFimFiltroUtc != null) {
                        builder.setSelection(new androidx.core.util.Pair<>(dataInicioFiltroUtc, dataFimFiltroUtc));
                    }
                    
                    MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
                    
                    picker.addOnPositiveButtonClickListener(selection -> {
                        if (selection != null) {
                            dataInicioFiltroUtc = selection.first;
                            dataFimFiltroUtc = selection.second;
                            dateFilterButton.setText(picker.getHeaderText());
                            clearDateFilterButton.setVisibility(View.VISIBLE);
                            carregarInstrumentos();
                        }
                    });
                    picker.addOnNegativeButtonClickListener(dialog -> {
                        // cancelar não altera
                    });
                    picker.show(getSupportFragmentManager(), "date_range_picker");
                });
            }

            // Botão para limpar filtro de datas
            if (clearDateFilterButton != null) {
                clearDateFilterButton.setOnClickListener(v -> {
                    dataInicioFiltroUtc = null;
                    dataFimFiltroUtc = null;
                    dateFilterButton.setText(getString(R.string.dates));
                    clearDateFilterButton.setVisibility(View.GONE);
                    carregarInstrumentos();
                });
            }

            // Adicionar listeners de clique nos botões de categoria
            findViewById(R.id.catCordas).setOnClickListener(v -> {
                categoriaAtual = getString(R.string.category_strings);
                carregarInstrumentos();
            });
            findViewById(R.id.catTeclas).setOnClickListener(v -> {
                categoriaAtual = getString(R.string.category_teclas);
                carregarInstrumentos();
            });
            findViewById(R.id.catPercussao).setOnClickListener(v -> {
                categoriaAtual = getString(R.string.category_percussao);
                Log.d("FiltroCategoria", "Clique em Percussão - categoriaAtual: '" + categoriaAtual + "'");
                carregarInstrumentos();
            });
            findViewById(R.id.catSopros).setOnClickListener(v -> {
                categoriaAtual = getString(R.string.category_sopro);
                carregarInstrumentos();
            });
            findViewById(R.id.catAcessorios).setOnClickListener(v -> {
                categoriaAtual = getString(R.string.category_acessorios);
                Log.d("FiltroCategoria", "Clique em Acessórios - categoriaAtual: '" + categoriaAtual + "'");
                carregarInstrumentos();
            });
            findViewById(R.id.catTodos).setOnClickListener(v -> {
                categoriaAtual = "";
                carregarInstrumentos();
            });

            // Configurar ordenação
            MaterialButton sortPriceAscButton = findViewById(R.id.sortPriceAscButton);
            MaterialButton sortPriceDescButton = findViewById(R.id.sortPriceDescButton);
            
            // Estado inicial: MENOR PREÇO selecionado
            atualizarEstadosBotoesOrdenacao(true);
            
            sortPriceAscButton.setOnClickListener(v -> {
                ordenarPorPrecoCrescente = true;
                atualizarEstadosBotoesOrdenacao(true);
                carregarInstrumentos();
            });
            
            sortPriceDescButton.setOnClickListener(v -> {
                ordenarPorPrecoCrescente = false;
                atualizarEstadosBotoesOrdenacao(false);
                carregarInstrumentos();
            });

            // Carregar instrumentos
            carregarInstrumentos();
            
            // Verificar solicitações não lidas na inicialização
            verificarSolicitacoesNaoLidas();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar foto de perfil no header quando voltar de outras telas
        atualizarImagemPerfilHeader();
        // Verificar solicitações não lidas sempre que a tela voltar ao foco
        verificarSolicitacoesNaoLidas();
    }

    private void atualizarImagemPerfilHeader() {
        try {
            NavigationView navigationView = findViewById(R.id.navigationView);
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                CircleImageView navProfileImageView = headerView.findViewById(R.id.navProfileImageView);
                TextView navUserName = headerView.findViewById(R.id.navUserName);
                
                FirebaseUser currentUser = autenticacao.getCurrentUser();
                if (currentUser != null) {
                    // Recarregar dados do usuário
                    GerenciadorFirebase.obterDadosUsuario(currentUser.getUid())
                            .thenAccept(userData -> {
                                if (userData != null && !userData.isEmpty()) {
                                    String displayName = (String) userData.get("name");
                                    String profileImageUrl = (String) userData.get("profileImageUrl");
                                    
                                    runOnUiThread(() -> {
                                        if (displayName != null && !displayName.isEmpty()) {
                                            navUserName.setText(displayName);
                                        } else {
                                            navUserName.setText(getString(R.string.user_default_name));
                                        }
                                        
                                        // Recarregar foto de perfil
                                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                            Log.d(TAG, "Recarregando foto: " + profileImageUrl);
                                            Glide.with(AtividadeInstrumentos.this)
                                                    .load(profileImageUrl)
                                                    .placeholder(R.drawable.ic_profile)
                                                    .error(R.drawable.ic_profile)
                                                    .into(navProfileImageView);
                                        }
                                    });
                                }
                            })
                            .exceptionally(throwable -> {
                                Log.e(TAG, "Erro ao recarregar dados do usuário: " + throwable.getMessage());
                                return null;
                            });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recarregar foto do header: " + e.getMessage());
        }
    }

    /**
     * Carrega e exibe os instrumentos baseado nos filtros ativos
     * 
     * Este método é o coração da funcionalidade de listagem, aplicando todos os
     * filtros configurados pelo usuário:
     * - Busca por texto (nome/descrição)
     * - Filtro por categoria
     * - Filtro por disponibilidade em período específico
     * - Ordenação por preço
     * 
     * Lógica de filtros:
     * 1. Determina qual consulta Firebase usar baseado nos filtros ativos
     * 2. Aplica filtros locais quando necessário (categoria + busca)
     * 3. Filtra por disponibilidade baseado nas faixas indisponíveis
     * 4. Ordena por preço conforme configuração do usuário
     * 5. Atualiza o adaptador com os resultados
     * 
     * Tratamento de erros:
     * - Exibe toast com mensagem de erro em caso de falha
     * - Logs detalhados para debugging
     */
    private void carregarInstrumentos() {
        Log.d("FiltroCategoria", "categoriaAtual: '" + categoriaAtual + "'");
        
        CompletableFuture<List<DocumentSnapshot>> future;
        
        // Determinar qual consulta Firebase usar baseado nos filtros ativos
        if (consultaAtual.isEmpty() && categoriaAtual.isEmpty()) {
            // Sem busca e sem filtro - carregar todos
            future = GerenciadorFirebase.obterTodosInstrumentos();
        } else if (consultaAtual.isEmpty()) {
            // Apenas filtro de categoria - normalizar antes de enviar para Firebase
            String categoriaNormalizada = normalizarCategoria(categoriaAtual);
            Log.d("FiltroCategoria", "Enviando categoria normalizada para Firebase: '" + categoriaNormalizada + "'");
            future = GerenciadorFirebase.obterInstrumentosPorCategoria(categoriaNormalizada);
        } else if (categoriaAtual.isEmpty()) {
            // Apenas busca por texto
            future = GerenciadorFirebase.buscarInstrumentos(consultaAtual);
        } else {
            // Busca e filtro - fazer busca geral e filtrar localmente
            future = GerenciadorFirebase.buscarInstrumentos(consultaAtual);
        }

        future.thenAccept(instruments -> {
            Log.d("FiltroCategoria", "Instrumentos retornados: " + instruments.size());
            
            // Se temos categoria e busca, filtrar localmente
            if (!consultaAtual.isEmpty() && !categoriaAtual.isEmpty()) {
                String categoriaNormalizada = normalizarCategoria(categoriaAtual);
                Log.d("FiltroCategoria", "Filtrando localmente - categoriaAtual: '" + categoriaAtual + "' -> normalizada: '" + categoriaNormalizada + "'");
                instruments.removeIf(doc -> {
                    String category = (String) doc.get("category");
                    String normalizedCategory = normalizarCategoria(category);
                    boolean shouldRemove = !categoriaNormalizada.equals(normalizedCategory);
                    Log.d("FiltroCategoria", "Doc: '" + category + "' -> normalizada: '" + normalizedCategory + "' -> remove: " + shouldRemove);
                    return shouldRemove;
                });
            }

            // Filtro por intervalo de datas (excluir instrumentos indisponíveis no período)
            if (dataInicioFiltroUtc != null && dataFimFiltroUtc != null) {
                final long start = dataInicioFiltroUtc;
                final long end = dataFimFiltroUtc;
                instruments.removeIf(doc -> {
                    Object rangesObj = doc.get("unavailableRanges");
                    if (!(rangesObj instanceof java.util.List)) {
                        return false; // sem faixas: disponível
                    }
                    @SuppressWarnings("unchecked")
                    List<Object> ranges = (List<Object>) rangesObj;
                    for (Object rangeObj : ranges) {
                        if (rangeObj instanceof Map) {
                            Map<String, Object> range = (Map<String, Object>) rangeObj;
                            Object s = range.get("startDate");
                            Object e = range.get("endDate");
                            long sMs = toMillis(s);
                            long eMs = toMillis(e);
                            // sobreposição: start <= eMs && end >= sMs
                            if (sMs != Long.MIN_VALUE && eMs != Long.MIN_VALUE) {
                                if (start <= eMs && end >= sMs) {
                                    return true; // indisponível nesse período
                                }
                            }
                        }
                    }
                    return false;
                });
            }
            
            // Ordenar por preço se necessário
            if (ordenarPorPrecoCrescente) {
                instruments.sort((a, b) -> {
                    Double priceA = (Double) a.get("price");
                    Double priceB = (Double) b.get("price");
                    return priceA.compareTo(priceB);
                });
            } else {
                instruments.sort((a, b) -> {
                    Double priceA = (Double) a.get("price");
                    Double priceB = (Double) b.get("price");
                    return priceB.compareTo(priceA);
                });
            }
            
            runOnUiThread(() -> adaptadorInstrumento.atualizarInstrumentos(instruments));
        }).exceptionally(throwable -> {
            Log.e("InstrumentsActivity", "Erro ao carregar instrumentos: " + throwable.getMessage());
            runOnUiThread(() -> 
                Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show()
            );
            return null;
        });
    }

    /**
     * Callback chamado quando o usuário clica em um instrumento
     * 
     * Navega para a tela de detalhes do instrumento selecionado,
     * passando o ID do instrumento como parâmetro.
     * 
     * @param instrument DocumentSnapshot do instrumento clicado
     */
    @Override
    public void aoClicarInstrumento(DocumentSnapshot instrument) {
        Intent intent = new Intent(this, AtividadeDetalhesInstrumento.class);
        intent.putExtra("instrument_id", instrument.getId());
        startActivity(intent);
    }

    /**
     * Callback chamado quando o usuário clica no botão de editar
     * 
     * Navega para a tela de adição/edição de instrumento em modo de edição,
     * passando o ID do instrumento para carregar os dados existentes.
     * 
     * @param instrument DocumentSnapshot do instrumento a ser editado
     */
    @Override
    public void aoClicarEditar(DocumentSnapshot instrument) {
        Intent intent = new Intent(this, AdicionarInstrumentoActivity.class);
        intent.putExtra("instrument_id", instrument.getId());
        startActivity(intent);
    }

    /**
     * Callback chamado quando o usuário clica no botão de deletar
     * 
     * Exibe um diálogo de confirmação antes de excluir o instrumento.
     * Após confirmação, chama o GerenciadorFirebase para deletar o instrumento
     * e recarrega a lista em caso de sucesso.
     * 
     * @param instrument DocumentSnapshot do instrumento a ser deletado
     */
    @Override
    public void aoClicarDeletar(DocumentSnapshot instrument) {
        // Mostrar diálogo de confirmação
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_instrument_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    String instrumentId = instrument.getId();
                    GerenciadorFirebase.deletarInstrumento(instrumentId)
                            .thenAccept(success -> {
                                if (success) {
                                    Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                                    carregarInstrumentos(); // Recarregar lista
                                } else {
                                    Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .exceptionally(throwable -> {
                                Log.e(TAG, "Erro ao excluir instrumento: " + throwable.getMessage(), throwable);
                                Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                return null;
                            });
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    
    /**
     * Callback chamado quando o usuário clica no botão de favorito
     * 
     * Gerencia a adição/remoção de instrumentos da lista de favoritos do usuário.
     * Atualiza o estado do botão e exibe feedback visual ao usuário.
     * 
     * @param instrument DocumentSnapshot do instrumento a ser favoritado/desfavoritado
     * @param isFavorite true se o instrumento está nos favoritos (será removido), false caso contrário
     */
    @Override
    public void aoClicarFavorito(DocumentSnapshot instrument, boolean isFavorite) {
        String instrumentId = instrument.getId();
        String userId = autenticacao.getCurrentUser().getUid();
        
        if (isFavorite) {
            // Remover dos favoritos
            GerenciadorFirebase.removerDosFavoritos(userId, instrumentId)
                    .thenAccept(success -> {
                        if (success) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show();
                                // Atualizar o botão no adapter
                                if (adaptadorInstrumento != null) {
                                    adaptadorInstrumento.notifyDataSetChanged();
                                }
                            });
                        } else {
                            runOnUiThread(() -> 
                                Toast.makeText(this, getString(R.string.error_remove_favorite), Toast.LENGTH_SHORT).show()
                            );
                        }
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Erro ao remover dos favoritos: " + throwable.getMessage(), throwable);
                        runOnUiThread(() -> 
                            Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                        return null;
                    });
        } else {
            // Adicionar aos favoritos
            GerenciadorFirebase.adicionarAosFavoritos(userId, instrumentId)
                    .thenAccept(success -> {
                        if (success) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
                                // Atualizar o botão no adapter
                                if (adaptadorInstrumento != null) {
                                    adaptadorInstrumento.notifyDataSetChanged();
                                }
                            });
                        } else {
                            runOnUiThread(() -> 
                                Toast.makeText(this, getString(R.string.error_add_favorite), Toast.LENGTH_SHORT).show()
                            );
                        }
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Erro ao adicionar aos favoritos: " + throwable.getMessage(), throwable);
                        runOnUiThread(() -> 
                            Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                        return null;
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_instruments, menu);
        
        // Configurar item de solicitações
        menuItemRequests = menu.findItem(R.id.action_requests);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, AtividadePrincipal.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_requests) {
            // Marcar todas as solicitações como lidas quando o usuário acessa a tela
            marcarTodasSolicitacoesComoLidas();
            
            // Abrir tela de solicitações (todas as solicitações do usuário)
            Intent intent = new Intent(this, AtividadeListaSolicitacoes.class);
            // Não passar dados de instrumento específico - mostrar todas as solicitações
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Verifica se há solicitações não lidas e atualiza o ícone
     */
    private void verificarSolicitacoesNaoLidas() {
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        if (usuarioAtual == null) {
            Log.w(TAG, "Usuário não está logado");
            return;
        }
        String userId = usuarioAtual.getUid();
        
        GerenciadorFirebase.buscarSolicitacoesProprietario(userId)
                .thenAccept(solicitacoes -> {
                    runOnUiThread(() -> {
                        boolean hasUnread = false;
                        
                        for (DocumentSnapshot solicitacao : solicitacoes) {
                            Boolean lida = solicitacao.getBoolean("lida");
                            if (lida == null || !lida) {
                                hasUnread = true;
                                break;
                            }
                        }
                        
                        atualizarIconeNotificacao(hasUnread);
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao verificar solicitações não lidas: " + erro.getMessage(), erro);
                    return null;
                });
    }
    
    /**
     * Atualiza o ícone de notificação com badge se houver solicitações não lidas
     */
    private void atualizarIconeNotificacao(boolean hasUnread) {
        hasUnreadRequests = hasUnread;
        
        if (menuItemRequests != null) {
            if (hasUnread) {
                // Mostrar ícone com badge de notificação
                menuItemRequests.setIcon(R.drawable.ic_notification_badge);
            } else {
                // Mostrar ícone normal
                menuItemRequests.setIcon(R.drawable.ic_request);
            }
        }
    }
    
    
    /**
     * Marca todas as solicitações recebidas como lidas
     * Chamado quando o usuário acessa a tela de solicitações
     */
    private void marcarTodasSolicitacoesComoLidas() {
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        if (usuarioAtual == null) {
            Log.w(TAG, "Usuário não está logado");
            return;
        }
        
        String userId = usuarioAtual.getUid();
        
        GerenciadorFirebase.marcarTodasSolicitacoesComoLidas(userId)
                .thenAccept(quantidadeMarcadas -> {
                    // Atualizar o badge imediatamente após marcar como lidas
                    runOnUiThread(() -> {
                        verificarSolicitacoesNaoLidas();
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao marcar solicitações como lidas: " + erro.getMessage(), erro);
                    return null;
                });
    }
    

    /**
     * Atualiza o estado visual dos botões de ordenação por preço
     * 
     * Aplica cores diferentes para indicar qual opção de ordenação está ativa:
     * - Botão selecionado: cor de destaque
     * - Botão não selecionado: cor padrão
     * 
     * @param isAscSelected true se ordenação crescente está selecionada, false para decrescente
     */
    private void atualizarEstadosBotoesOrdenacao(boolean isAscSelected) {
        MaterialButton sortPriceAscButton = findViewById(R.id.sortPriceAscButton);
        MaterialButton sortPriceDescButton = findViewById(R.id.sortPriceDescButton);
        
        if (isAscSelected) {
            // MENOR PREÇO selecionado
            sortPriceAscButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_selected_bg, null));
            sortPriceAscButton.setTextColor(getResources().getColor(R.color.button_selected_text, null));
            
            sortPriceDescButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected_bg, null));
            sortPriceDescButton.setTextColor(getResources().getColor(R.color.button_unselected_text, null));
        } else {
            // MAIOR PREÇO selecionado
            sortPriceAscButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_unselected_bg, null));
            sortPriceAscButton.setTextColor(getResources().getColor(R.color.button_unselected_text, null));
            
            sortPriceDescButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_selected_bg, null));
            sortPriceDescButton.setTextColor(getResources().getColor(R.color.button_selected_text, null));
        }
    }

    /**
     * Sobrescreve o comportamento do botão voltar
     * 
     * Como esta é a tela principal do aplicativo quando o usuário está logado,
     * o botão voltar é desabilitado para evitar que o usuário saia acidentalmente
     * do aplicativo. O logout deve ser feito através do menu lateral.
     */
    @Override
    public void onBackPressed() {
        // Não faz nada, pois esta é a tela principal do app quando logado
    }
} 