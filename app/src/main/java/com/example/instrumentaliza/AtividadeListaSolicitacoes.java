package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AtividadeListaSolicitacoes - Tela de lista de solicitações de reserva
 * 
 * Esta tela exibe todas as solicitações de reserva recebidas para um instrumento específico,
 * permitindo ao proprietário visualizar os detalhes de cada solicitação e decidir
 * se aceita ou recusa a solicitação.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de solicitações pendentes
 * - Informações do solicitante (nome, email)
 * - Período solicitado e preço total
 * - Status da solicitação (PENDENTE, ACEITA, RECUSADA)
 * - Navegação para detalhes da solicitação
 * - Estado vazio quando não há solicitações
 * - Atualização automática da lista
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Adaptador customizado para solicitações
 * - Carregamento assíncrono do Firebase
 * - Interface de callback para interações
 * - Filtro por instrumento específico
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeListaSolicitacoes extends AppCompatActivity implements AdaptadorSolicitacoes.OnSolicitacaoClickListener {
    
    // Constantes
    private static final String TAG = "ListaSolicitacoes";
    
    // Componentes da interface
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdaptadorSolicitacaoTabs adaptadorSolicitacaoTabs;
    
    // Firebase
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Inicializa todos os componentes da interface, configura a toolbar,
     * obtém os dados do Intent e carrega as solicitações do Firebase.
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_solicitacoes);
        
        Log.d(TAG, "=== ATIVIDADE LISTA SOLICITAÇÕES INICIADA ===");
        
        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Solicitações de Reserva");
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
        
        // Inicializar views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Configurar ViewPager2 e TabLayout
        adaptadorSolicitacaoTabs = new AdaptadorSolicitacaoTabs(this);
        viewPager.setAdapter(adaptadorSolicitacaoTabs);

        // Conectar TabLayout com ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Recebidas");
                    break;
                case 1:
                    tab.setText("Enviadas");
                    break;
            }
        }).attach();
    }

    /**
     * Recarrega as abas de solicitações
     * 
     * Método público para ser chamado pelos fragments quando necessário
     */
    public void recarregarAbas() {
        Log.d(TAG, "Recarregando abas de solicitações");
        // Notificar fragments para recarregar dados
        for (int i = 0; i < adaptadorSolicitacaoTabs.getItemCount(); i++) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (fragment instanceof FragmentSolicitacaoTab) {
                ((FragmentSolicitacaoTab) fragment).carregarSolicitacoes();
            }
        }
    }

    /**
     * Manipula o clique em uma solicitação
     * 
     * Abre a tela de detalhes da solicitação para que o proprietário
     * possa aceitar ou recusar a solicitação.
     * 
     * @param solicitacao Documento da solicitação no Firestore
     */
    @Override
    public void onSolicitacaoClick(DocumentSnapshot solicitacao) {
        Log.d(TAG, "Abrindo detalhes da solicitação: " + solicitacao.getId());
        
        Intent intent = new Intent(this, AtividadeDetalhesSolicitacao.class);
        intent.putExtra("solicitacao_id", solicitacao.getId());
        intent.putExtra("instrumento_id", solicitacao.getString("instrumentoId"));
        intent.putExtra("proprietario_id", solicitacao.getString("proprietarioId"));
        intent.putExtra("locatario_id", solicitacao.getString("locatarioId"));
        startActivity(intent);
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
     * Atualiza a lista quando a atividade retorna do foco
     * 
     * Recarrega as solicitações para mostrar possíveis mudanças
     * de status (aceita/recusada) feitas em outras telas.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Atividade retomada, recarregando solicitações");
        recarregarAbas();
    }
}
