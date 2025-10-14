package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AtividadeMinhasReservas - Tela de minhas reservas
 * 
 * Esta tela exibe todas as reservas ativas do usuário logado, incluindo
 * reservas que foram aceitas através do sistema de solicitações.
 * 
 * Funcionalidades principais:
 * - Exibição de lista de reservas ativas do usuário
 * - Diferenciação por status (CONFIRMED, PENDING, CANCELLED)
 * - Formatação de datas e preços
 * - Estado vazio quando não há reservas
 * - Integração com Firebase Firestore
 * - Navegação de volta para tela anterior
 * 
 * Características técnicas:
 * - RecyclerView com LinearLayoutManager
 * - Firebase Firestore para dados
 * - Adaptador customizado para reservas
 * - Firebase Auth para autenticação
 * - Tratamento de estados vazios
 * 
 * @author Jhonata
 * @version 2.0 - Atualizado para Firebase
 */
public class AtividadeMinhasReservas extends AppCompatActivity implements AdaptadorReservas.OnReservaClickListener {
    
    // Constantes
    private static final String TAG = "MinhasReservas";
    private static final String STATUS_PENDENTE = "PENDENTE";
    private static final String STATUS_CONFIRMADA = "CONFIRMADA";
    private static final String STATUS_CANCELADA = "CANCELADA";
    private static final String STATUS_CONCLUIDA = "CONCLUIDA";

    // Componentes da interface
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdaptadorReservaTabs adaptadorReservaTabs;
    
    // Firebase e dados
    private FirebaseAuth autenticacao;
    private FirebaseUser usuarioAtual;

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de minhas reservas, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do gerenciador de sessão
     * - Configuração do RecyclerView e adaptador
     * - Carregamento das reservas do usuário
     * - Configuração do estado vazio
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        Log.d(TAG, "=== ATIVIDADE MINHAS RESERVAS INICIADA ===");

        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        autenticacao = FirebaseAuth.getInstance();
        usuarioAtual = autenticacao.getCurrentUser();

        Log.d(TAG, "=== INICIALIZAÇÃO ===");
        Log.d(TAG, "Autenticação inicializada: " + (autenticacao != null ? "SIM" : "NÃO"));
        Log.d(TAG, "Usuário atual: " + (usuarioAtual != null ? usuarioAtual.getUid() : "NULL"));
        Log.d(TAG, "Email do usuário: " + (usuarioAtual != null ? usuarioAtual.getEmail() : "NULL"));

        // Verificar se o usuário está logado
        if (usuarioAtual == null) {
            Log.e(TAG, "Usuário não está logado - finalizando atividade");
            finish();
            return;
        }

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Inicializar views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Configurar ViewPager2 e TabLayout
        adaptadorReservaTabs = new AdaptadorReservaTabs(this);
        viewPager.setAdapter(adaptadorReservaTabs);

        // Conectar TabLayout com ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Meus Instrumentos");
                    break;
                case 1:
                    tab.setText("Meus Interesses");
                    break;
            }
        }).attach();
    }

    /**
     * Recarrega as abas de reservas
     * 
     * Método público para ser chamado pelos fragments quando necessário
     */
    public void recarregarAbas() {
        Log.d(TAG, "Recarregando abas de reservas");
        // Notificar fragments para recarregar dados
        for (int i = 0; i < adaptadorReservaTabs.getItemCount(); i++) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + i);
            if (fragment instanceof FragmentReservaTab) {
                ((FragmentReservaTab) fragment).carregarReservas();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Implementa o listener para cliques em reservas
     * 
     * @param reserva Documento da reserva clicada
     */
    public void onReservaClick(DocumentSnapshot reserva) {
        Log.d(TAG, "Reserva clicada: " + reserva.getId());
        // TODO: Implementar navegação para detalhes da reserva
        Toast.makeText(this, "Detalhes da reserva em breve!", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onAvaliarReserva(DocumentSnapshot reserva) {
        Log.d(TAG, "Avaliando reserva: " + reserva.getId());
        
        // Abrir tela de avaliação
        Intent intent = new Intent(this, AtividadeAvaliarAluguel.class);
        intent.putExtra("reserva_id", reserva.getId());
        intent.putExtra("instrumento_id", reserva.getString("instrumentId"));
        intent.putExtra("instrumento_nome", "Instrumento"); // Será carregado na tela
        intent.putExtra("proprietario_id", reserva.getString("ownerId"));
        intent.putExtra("proprietario_nome", "Proprietário"); // Será carregado na tela
        
        startActivity(intent);
    }

} 