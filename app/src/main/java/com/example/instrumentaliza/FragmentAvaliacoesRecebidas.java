package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento para exibir avaliações recebidas pelo usuário
 */
public class FragmentAvaliacoesRecebidas extends Fragment {
    
    private static final String TAG = "FragmentAvaliacoesRecebidas";
    
    private RecyclerView listaAvaliacoes;
    private MaterialCardView emptyStateCard;
    private AdaptadorAvaliacoesRecebidas adaptadorAvaliacoes;
    private FirebaseUser usuarioAtual;
    
    private List<DocumentSnapshot> avaliacoes = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avaliacoes_recebidas, container, false);
        
        // Inicializar componentes
        listaAvaliacoes = view.findViewById(R.id.listaAvaliacoes);
        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        
        // Configurar RecyclerView
        adaptadorAvaliacoes = new AdaptadorAvaliacoesRecebidas(avaliacoes);
        listaAvaliacoes.setLayoutManager(new LinearLayoutManager(getContext()));
        listaAvaliacoes.setAdapter(adaptadorAvaliacoes);
        
        // Obter usuário atual
        usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        
        Log.d(TAG, "View criada - listaAvaliacoes: " + (listaAvaliacoes != null) + ", emptyStateCard: " + (emptyStateCard != null));
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        carregarAvaliacoes();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Recarregar avaliações quando o fragmento volta a ser visível
        if (usuarioAtual != null) {
            Log.d(TAG, "Fragmento retomado, recarregando avaliações");
            carregarAvaliacoes();
        }
    }
    
    /**
     * Carrega as avaliações recebidas pelo usuário
     */
    public void carregarAvaliacoes() {
        if (usuarioAtual == null) {
            Log.e(TAG, "Usuário não autenticado");
            mostrarEstadoVazio();
            return;
        }
        
        Log.d(TAG, "Carregando avaliações recebidas como LOCATÁRIO para usuário: " + usuarioAtual.getUid());
        
        GerenciadorFirebase.obterAvaliacoesRecebidas(usuarioAtual.getUid())
                .thenAccept(avaliacoesRecebidas -> {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        this.avaliacoes.clear();
                        this.avaliacoes.addAll(avaliacoesRecebidas);
                        
                        Log.d(TAG, "Avaliações recebidas como LOCATÁRIO carregadas: " + avaliacoesRecebidas.size());
                        
                        if (avaliacoesRecebidas.isEmpty()) {
                            mostrarEstadoVazio();
                        } else {
                            mostrarListaAvaliacoes();
                        }
                        
                        adaptadorAvaliacoes.atualizarAvaliacoes(this.avaliacoes);
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao carregar avaliações: " + erro.getMessage(), erro);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::mostrarEstadoVazio);
                    }
                    return null;
                });
    }
    
    /**
     * Mostra a lista de avaliações
     */
    private void mostrarListaAvaliacoes() {
        if (listaAvaliacoes != null && emptyStateCard != null) {
            listaAvaliacoes.setVisibility(View.VISIBLE);
            emptyStateCard.setVisibility(View.GONE);
            Log.d(TAG, "Mostrando lista de avaliações");
        }
    }
    
    /**
     * Mostra o estado vazio
     */
    private void mostrarEstadoVazio() {
        if (listaAvaliacoes != null && emptyStateCard != null) {
            listaAvaliacoes.setVisibility(View.GONE);
            emptyStateCard.setVisibility(View.VISIBLE);
            Log.d(TAG, "Mostrando estado vazio");
        }
    }
}
