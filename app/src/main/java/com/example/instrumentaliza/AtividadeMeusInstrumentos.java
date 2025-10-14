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
 * AtividadeMeusInstrumentos - Tela de meus instrumentos
 * 
 * Esta tela exibe todos os instrumentos que pertencem ao usuário logado,
 * permitindo gerenciar seus próprios instrumentos, visualizar detalhes,
 * editar informações e excluir instrumentos.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de instrumentos do usuário
 * - Navegação para detalhes dos instrumentos
 * - Edição de instrumentos existentes
 * - Exclusão de instrumentos
 * - Estado vazio quando não há instrumentos
 * - Atualização automática da lista
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para instrumentos próprios
 * - Carregamento assíncrono do Firebase
 * - Interface de callback para interações
 * - Filtro por proprietário (ownerId)
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeMeusInstrumentos extends AppCompatActivity implements AdaptadorMeusInstrumentos.OnMyInstrumentClickListener {
    
    // Constantes
    private static final String TAG = "MeusInstrumentos";
    
    // Componentes da interface
    private RecyclerView listaMeusInstrumentos;
    private AdaptadorMeusInstrumentos adaptadorMeusInstrumentos;
    private GerenciadorNotificacoes gerenciadorNotificacoes;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de meus instrumentos, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração do RecyclerView e adaptador
     * - Carregamento dos instrumentos do usuário
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_instruments);

        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();
        
        // Inicializar gerenciador de notificações
        gerenciadorNotificacoes = new GerenciadorNotificacoes(this);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.my_instruments));
        }

        // Inicializar views
        listaMeusInstrumentos = findViewById(R.id.myInstrumentsRecyclerView);

        // Configurar RecyclerView
        listaMeusInstrumentos.setLayoutManager(new LinearLayoutManager(this));
        adaptadorMeusInstrumentos = new AdaptadorMeusInstrumentos(new ArrayList<>(), this);
        listaMeusInstrumentos.setAdapter(adaptadorMeusInstrumentos);

        // Carregar instrumentos do usuário
        carregarMeusInstrumentos();
    }

    private void carregarMeusInstrumentos() {
        FirebaseUser currentUser = autenticacao.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Carregando instrumentos do usuário: " + currentUser.getUid());

        GerenciadorFirebase.obterInstrumentosPorProprietario(currentUser.getUid())
                .thenAccept(instruments -> {
                    runOnUiThread(() -> {
                        if (instruments.isEmpty()) {
                            mostrarEstadoVazio();
                        } else {
                            esconderEstadoVazio();
                            adaptadorMeusInstrumentos.atualizarInstrumentos(instruments);
                        }
                        Log.d(TAG, "Instrumentos carregados: " + instruments.size());
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao carregar instrumentos: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        mostrarEstadoVazio();
                    });
                    return null;
                });
    }

    private void mostrarEstadoVazio() {
        // Como não temos mais o emptyStateLayout, apenas escondemos o RecyclerView
        listaMeusInstrumentos.setVisibility(View.GONE);
        // Mostrar mensagem de estado vazio
        Toast.makeText(this, getString(R.string.empty_instruments), Toast.LENGTH_LONG).show();
    }

    private void esconderEstadoVazio() {
        listaMeusInstrumentos.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInstrumentClick(DocumentSnapshot instrument) {
        // Abrir detalhes do instrumento
        Intent intent = new Intent(this, AtividadeDetalhesInstrumento.class);
        intent.putExtra("instrument_id", instrument.getId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(DocumentSnapshot instrument) {
        // Abrir tela de edição do instrumento
        Intent intent = new Intent(this, AtividadeEditarInstrumento.class);
        intent.putExtra("instrument_id", instrument.getId());
        startActivityForResult(intent, 1); // Request code 1 para identificar retorno
    }

    @Override
    public void onDeleteClick(DocumentSnapshot instrument) {
        // Confirmar exclusão do instrumento
        String instrumentId = instrument.getId();
        String instrumentName = instrument.getString("name");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete))
                .setPositiveButton(getString(R.string.action_delete), (dialog, which) -> {
                    deletarInstrumento(instrumentId);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deletarInstrumento(String instrumentId) {
        GerenciadorFirebase.deletarInstrumento(instrumentId)
                .thenAccept(success -> {
                    if (success) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, getString(R.string.instrument_deleted), Toast.LENGTH_SHORT).show();
                            // Recarregar lista
                            carregarMeusInstrumentos();
                        });
                    } else {
                        runOnUiThread(() -> 
                            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                        );
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao excluir instrumento: " + throwable.getMessage(), throwable);
                    runOnUiThread(() -> 
                        Toast.makeText(this, getString(R.string.error_generic) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                    return null;
                });
    }
    
    /**
     * Manipula o retorno de atividades (como edição de instrumento)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1) { // Código da edição de instrumento
            if (resultCode == RESULT_OK) {
                // Recarregar a lista de instrumentos
                Log.d(TAG, "Instrumento editado com sucesso, recarregando lista");
                carregarMeusInstrumentos();
            }
        }
    }


    /**
     * Manipula o clique no botão "Solicitações"
     * 
     * Abre a tela de lista de solicitações de reserva para o instrumento selecionado,
     * permitindo ao proprietário ver e gerenciar as solicitações recebidas.
     * 
     * @param instrumento Documento do instrumento no Firestore
     */
    @Override
    public void onRequestsClick(DocumentSnapshot instrumento) {
        Log.d(TAG, "Abrindo solicitações para: " + instrumento.getId());
        
        Intent intent = new Intent(this, AtividadeListaSolicitacoes.class);
        intent.putExtra("instrument_id", instrumento.getId());
        intent.putExtra("instrument_name", instrumento.getString("name"));
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Iniciar listener de notificações quando a tela está ativa
        if (gerenciadorNotificacoes != null) {
            gerenciadorNotificacoes.iniciarListenerSolicitacoes();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Parar listener de notificações quando a tela não está ativa
        if (gerenciadorNotificacoes != null) {
            gerenciadorNotificacoes.pararListenerSolicitacoes();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar recursos do gerenciador de notificações
        if (gerenciadorNotificacoes != null) {
            gerenciadorNotificacoes.pararListenerSolicitacoes();
        }
    }
}
