package com.example.instrumentaliza;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FragmentSolicitacaoTab extends Fragment implements AdaptadorSolicitacoes.OnSolicitacaoClickListener {
    private static final String TAG = "FragmentSolicitacaoTab";
    
    private RecyclerView listaSolicitacoes;
    private MaterialCardView layoutEstadoVazio;
    private AdaptadorSolicitacoes adaptadorSolicitacoes;
    private List<DocumentSnapshot> solicitacoes = new ArrayList<>();
    private String tipoSolicitacao; // "solicitacoes_recebidas" ou "solicitacoes_enviadas"
    private String usuarioId;

    public static FragmentSolicitacaoTab newInstance(String tipoSolicitacao) {
        FragmentSolicitacaoTab fragment = new FragmentSolicitacaoTab();
        Bundle args = new Bundle();
        args.putString("tipo_solicitacao", tipoSolicitacao);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tipoSolicitacao = getArguments().getString("tipo_solicitacao");
        }
        FirebaseUser usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        usuarioId = usuarioAtual != null ? usuarioAtual.getUid() : null;
        Log.d(TAG, "Fragment criado para tipo: " + tipoSolicitacao + " - Usuário: " + usuarioId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_solicitacao_tab, container, false);
        
        listaSolicitacoes = view.findViewById(R.id.listaSolicitacoes);
        layoutEstadoVazio = view.findViewById(R.id.emptyStateCard);
        
        // Configurar RecyclerView
        adaptadorSolicitacoes = new AdaptadorSolicitacoes(solicitacoes, this);
        listaSolicitacoes.setLayoutManager(new LinearLayoutManager(getContext()));
        listaSolicitacoes.setAdapter(adaptadorSolicitacoes);
        
        Log.d(TAG, "View criada - listaSolicitacoes: " + (listaSolicitacoes != null) + ", layoutEstadoVazio: " + (layoutEstadoVazio != null));
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        carregarSolicitacoes();
    }

    public void carregarSolicitacoes() {
        Log.d(TAG, "Carregando solicitações do tipo: " + tipoSolicitacao + " para usuário: " + usuarioId);
        
        if (tipoSolicitacao == null || usuarioId == null) {
            Log.e(TAG, "Tipo de solicitação ou usuário não definido");
            mostrarEstadoVazio();
            return;
        }

        // Primeiro, verificar e atualizar solicitações expiradas
        GerenciadorFirebase.verificarEAtualizarSolicitacoesExpiradas(usuarioId)
            .thenCompose(atualizadas -> {
                Log.d(TAG, "Solicitações expiradas atualizadas: " + atualizadas);
                
                // Buscar solicitações baseado no tipo
                CompletableFuture<List<DocumentSnapshot>> buscaFutura;
                
                if ("solicitacoes_recebidas".equals(tipoSolicitacao)) {
                    // Buscar solicitações onde o usuário é o proprietário
                    buscaFutura = buscarSolicitacoesRecebidas();
                } else {
                    // Buscar solicitações onde o usuário é o solicitante
                    buscaFutura = buscarSolicitacoesEnviadas();
                }
                
                return buscaFutura;
            })
            .thenAccept(todasSolicitacoes -> {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Solicitações encontradas para " + tipoSolicitacao + ": " + todasSolicitacoes.size());
                    
                    // Ordenar as solicitações
                    List<DocumentSnapshot> solicitacoesOrdenadas = ordenarSolicitacoes(todasSolicitacoes);
                    
                    solicitacoes.clear();
                    solicitacoes.addAll(solicitacoesOrdenadas);
                    adaptadorSolicitacoes.notifyDataSetChanged();
                    
                    if (solicitacoes.isEmpty()) {
                        mostrarEstadoVazio();
                    } else {
                        mostrarLista();
                    }
                });
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Erro ao carregar solicitações: " + throwable.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro ao carregar solicitações", Toast.LENGTH_SHORT).show();
                        mostrarEstadoVazio();
                    });
                }
                return null;
            });
    }

    /**
     * Ordena as solicitações conforme regra:
     * 1. PENDENTE (antigas primeiro)
     * 2. ACEITA/RECUSADA (recentes primeiro)
     */
    private List<DocumentSnapshot> ordenarSolicitacoes(List<DocumentSnapshot> todasSolicitacoes) {
        Log.d(TAG, "Ordenando " + todasSolicitacoes.size() + " solicitações");
        
        List<DocumentSnapshot> pendentes = new ArrayList<>();
        List<DocumentSnapshot> outras = new ArrayList<>();
        
        // Separar por status
        for (DocumentSnapshot doc : todasSolicitacoes) {
            String status = doc.getString("status");
            if ("PENDENTE".equals(status)) {
                pendentes.add(doc);
            } else {
                outras.add(doc);
            }
        }
        
        // Ordenar PENDENTE por data de criação (antigas primeiro)
        pendentes.sort((a, b) -> {
            com.google.firebase.Timestamp timestampA = a.getTimestamp("dataCriacao");
            com.google.firebase.Timestamp timestampB = b.getTimestamp("dataCriacao");
            
            if (timestampA == null || timestampB == null) return 0;
            
            return timestampA.compareTo(timestampB); // Antigas primeiro
        });
        
        // Ordenar ACEITA/RECUSADA por data de criação (recentes primeiro)
        outras.sort((a, b) -> {
            com.google.firebase.Timestamp timestampA = a.getTimestamp("dataCriacao");
            com.google.firebase.Timestamp timestampB = b.getTimestamp("dataCriacao");
            
            if (timestampA == null || timestampB == null) return 0;
            
            return timestampB.compareTo(timestampA); // Recentes primeiro
        });
        
        // Combinar: PENDENTE primeiro, depois outras
        List<DocumentSnapshot> resultado = new ArrayList<>();
        resultado.addAll(pendentes);
        resultado.addAll(outras);
        
        Log.d(TAG, "Ordenação concluída: " + pendentes.size() + " pendentes + " + outras.size() + " outras");
        
        return resultado;
    }

    /**
     * Busca solicitações onde o usuário é o proprietário (recebidas)
     */
    private CompletableFuture<List<DocumentSnapshot>> buscarSolicitacoesRecebidas() {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando solicitações recebidas para usuário: " + usuarioId);
        
        FirebaseFirestore.getInstance()
                .collection("solicitacoes")
                .whereEqualTo("proprietarioId", usuarioId)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> solicitacoes = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Solicitações recebidas encontradas: " + solicitacoes.size());
                    futuro.complete(solicitacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações recebidas: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }

    /**
     * Busca solicitações onde o usuário é o solicitante (enviadas)
     */
    private CompletableFuture<List<DocumentSnapshot>> buscarSolicitacoesEnviadas() {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando solicitações enviadas por usuário: " + usuarioId);
        
        FirebaseFirestore.getInstance()
                .collection("solicitacoes")
                .whereEqualTo("solicitanteId", usuarioId)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> solicitacoes = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Solicitações enviadas encontradas: " + solicitacoes.size());
                    futuro.complete(solicitacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações enviadas: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }

    private void mostrarEstadoVazio() {
        if (listaSolicitacoes != null && layoutEstadoVazio != null) {
            listaSolicitacoes.setVisibility(View.GONE);
            layoutEstadoVazio.setVisibility(View.VISIBLE);
            Log.d(TAG, "Mostrando estado vazio");
        }
    }

    private void mostrarLista() {
        if (listaSolicitacoes != null && layoutEstadoVazio != null) {
            listaSolicitacoes.setVisibility(View.VISIBLE);
            layoutEstadoVazio.setVisibility(View.GONE);
            Log.d(TAG, "Mostrando lista com " + solicitacoes.size() + " solicitações");
        }
    }

    @Override
    public void onSolicitacaoClick(DocumentSnapshot solicitacao) {
        // Implementar navegação para detalhes da solicitação
        Log.d(TAG, "Solicitação clicada: " + solicitacao.getId());
        
        // Navegar para tela de detalhes
        Intent intent = new Intent(getContext(), AtividadeDetalhesSolicitacao.class);
        intent.putExtra("solicitacao_id", solicitacao.getId());
        intent.putExtra("instrumento_id", solicitacao.getString("instrumentoId"));
        intent.putExtra("proprietario_id", solicitacao.getString("proprietarioId"));
        intent.putExtra("locatario_id", solicitacao.getString("solicitanteId"));
        startActivity(intent);
    }
}
