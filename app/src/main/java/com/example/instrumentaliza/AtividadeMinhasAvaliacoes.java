package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * AtividadeMinhasAvaliacoes - Tela de avaliações recebidas
 * 
 * Esta tela exibe todas as avaliações recebidas pelo usuário logado
 * como proprietário de instrumentos, incluindo nota, comentário e
 * informações sobre o instrumento e locatário.
 * 
 * Funcionalidades:
 * - Exibição de lista de avaliações recebidas
 * - Ordenação por data (mais recentes primeiro)
 * - Informações detalhadas de cada avaliação
 * - Estado vazio quando não há avaliações
 * - Atualização automática da lista
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para avaliações
 * - Carregamento assíncrono do Firebase
 * - Interface responsiva
 * - Logs detalhados para debug
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeMinhasAvaliacoes extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "MinhasAvaliacoes";
    
    // Componentes da interface
    private RecyclerView listaAvaliacoes;
    private AdaptadorAvaliacoes adaptadorAvaliacoes;
    private TextView textoEstadoVazio;
    
    // Autenticação
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;
    
    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de avaliações, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração do RecyclerView e adaptador
     * - Carregamento das avaliações do usuário
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minhas_avaliacoes);
        
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
        
        // Configurar Toolbar
        configurarToolbar();
        
        // Inicializar componentes
        inicializarComponentes();
        
        // Carregar avaliações do usuário
        carregarMinhasAvaliacoes();
        
        Log.d(TAG, "Atividade criada para usuário: " + usuarioAtual.getUid());
    }
    
    /**
     * Configura a toolbar com título e botão de voltar
     */
    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Minhas Avaliações");
        }
    }
    
    /**
     * Inicializa todos os componentes da interface
     */
    private void inicializarComponentes() {
        listaAvaliacoes = findViewById(R.id.listaAvaliacoes);
        textoEstadoVazio = findViewById(R.id.textoEstadoVazio);
        
        // Configurar RecyclerView
        listaAvaliacoes.setLayoutManager(new LinearLayoutManager(this));
        adaptadorAvaliacoes = new AdaptadorAvaliacoes(new ArrayList<>());
        listaAvaliacoes.setAdapter(adaptadorAvaliacoes);
        
        // Configurar estado vazio
        textoEstadoVazio.setText("Você ainda não recebeu nenhuma avaliação.\n\n" +
                                "Quando alguém alugar seus instrumentos e avaliar a experiência, " +
                                "as avaliações aparecerão aqui.");
        textoEstadoVazio.setVisibility(View.GONE);
    }
    
    /**
     * Carrega as avaliações recebidas pelo usuário
     */
    private void carregarMinhasAvaliacoes() {
        Log.d(TAG, "Carregando avaliações do usuário: " + usuarioAtual.getUid());
        
        GerenciadorFirebase.obterAvaliacoesProprietario(usuarioAtual.getUid())
                .thenAccept(avaliacoes -> {
                    runOnUiThread(() -> {
                        if (avaliacoes.isEmpty()) {
                            mostrarEstadoVazio();
                        } else {
                            esconderEstadoVazio();
                            adaptadorAvaliacoes.atualizarAvaliacoes(avaliacoes);
                        }
                        Log.d(TAG, "Avaliações carregadas: " + avaliacoes.size());
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar avaliações: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao carregar avaliações: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        mostrarEstadoVazio();
                    });
                    return null;
                });
    }
    
    /**
     * Mostra o estado vazio quando não há avaliações
     */
    private void mostrarEstadoVazio() {
        listaAvaliacoes.setVisibility(View.GONE);
        textoEstadoVazio.setVisibility(View.VISIBLE);
    }
    
    /**
     * Esconde o estado vazio quando há avaliações
     */
    private void esconderEstadoVazio() {
        listaAvaliacoes.setVisibility(View.VISIBLE);
        textoEstadoVazio.setVisibility(View.GONE);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar avaliações quando a tela é retomada
        carregarMinhasAvaliacoes();
    }
}
