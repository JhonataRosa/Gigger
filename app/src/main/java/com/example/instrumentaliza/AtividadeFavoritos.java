package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
 * AtividadeFavoritos - Tela de instrumentos favoritos
 * 
 * Esta tela exibe todos os instrumentos que o usuário marcou como favoritos,
 * permitindo visualização rápida e acesso direto aos instrumentos preferidos.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de instrumentos favoritos
 * - Navegação para detalhes dos instrumentos
 * - Remoção de favoritos
 * - Estado vazio quando não há favoritos
 * - Atualização automática da lista
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para instrumentos
 * - Carregamento assíncrono do Firebase
 * - Interface de callback para interações
 * - Tratamento de estados vazios
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeFavoritos extends AppCompatActivity implements AdaptadorInstrumentoFirebase.OnInstrumentClickListener {
    
    // Constantes
    private static final String TAG = "Favoritos";
    
    // Componentes da interface
    private RecyclerView listaFavoritos;
    private View layoutEstadoVazio;
    private AdaptadorInstrumentoFirebase adaptadorFavoritos;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de favoritos, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração do RecyclerView e adaptador
     * - Carregamento dos instrumentos favoritos
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.my_favorites));
        }

        // Inicializar views
        listaFavoritos = findViewById(R.id.favoritesRecyclerView);
        layoutEstadoVazio = findViewById(R.id.emptyStateLayout);

        // Configurar RecyclerView
        listaFavoritos.setLayoutManager(new LinearLayoutManager(this));
        adaptadorFavoritos = new AdaptadorInstrumentoFirebase(new ArrayList<>(), autenticacao.getCurrentUser().getUid(), this);
        listaFavoritos.setAdapter(adaptadorFavoritos);

        // Carregar favoritos
        carregarFavoritos();
    }

    private void carregarFavoritos() {
        FirebaseUser currentUser = autenticacao.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Carregando favoritos para usuário: " + currentUser.getUid());

        GerenciadorFirebase.obterInstrumentosFavoritos(currentUser.getUid())
                .thenAccept(instruments -> {
                    runOnUiThread(() -> {
                        if (instruments.isEmpty()) {
                            mostrarEstadoVazio();
                        } else {
                            esconderEstadoVazio();
                            adaptadorFavoritos.atualizarInstrumentos(instruments);
                        }
                        Log.d(TAG, "Favoritos carregados: " + instruments.size());
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar favoritos: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        mostrarEstadoVazio();
                    });
                    return null;
                });
    }

    private void mostrarEstadoVazio() {
        layoutEstadoVazio.setVisibility(View.VISIBLE);
    }

    private void esconderEstadoVazio() {
        layoutEstadoVazio.setVisibility(View.GONE);
    }

    @Override
    public void aoClicarInstrumento(DocumentSnapshot instrument) {
        // Abrir detalhes do instrumento
        Intent intent = new Intent(this, AtividadeDetalhesInstrumento.class);
        intent.putExtra("instrument_id", instrument.getId());
        startActivity(intent);
    }

    @Override
    public void aoClicarEditar(DocumentSnapshot instrument) {
        // Não permitir edição na tela de favoritos
        Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aoClicarDeletar(DocumentSnapshot instrument) {
        // Não permitir exclusão na tela de favoritos
        Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_SHORT).show();
    }

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
                                // Recarregar favoritos
                                carregarFavoritos();
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
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
