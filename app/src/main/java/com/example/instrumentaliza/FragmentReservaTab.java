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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FragmentReservaTab - Fragment para exibição de reservas
 * 
 * Este fragment exibe uma lista de reservas do usuário, diferenciando entre
 * reservas de instrumentos próprios ("meus_instrumentos") e reservas de
 * instrumentos alugados ("meus_interesses").
 * 
 * Funcionalidades:
 * - Exibição de reservas em RecyclerView
 * - Diferenciação entre tipos de reserva
 * - Navegação para avaliação de instrumentos/usuários
 * - Estado vazio quando não há reservas
 * - Atualização automática dos dados
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FragmentReservaTab extends Fragment implements AdaptadorReservas.OnReservaClickListener {
    private static final String TAG = "FragmentReservaTab";
    
    private RecyclerView listaReservas;
    private MaterialCardView layoutEstadoVazio;
    private AdaptadorReservas adaptadorReservas;
    private List<DocumentSnapshot> reservas = new ArrayList<>();
    private String tipoReserva; // "meus_instrumentos" ou "meus_interesses"
    private String usuarioId;

    public static FragmentReservaTab newInstance(String tipoReserva) {
        FragmentReservaTab fragment = new FragmentReservaTab();
        Bundle args = new Bundle();
        args.putString("tipo_reserva", tipoReserva);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tipoReserva = getArguments().getString("tipo_reserva");
        }
        usuarioId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Fragment criado para tipo: " + tipoReserva + " - Usuário: " + usuarioId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reserva_tab, container, false);
        
        listaReservas = view.findViewById(R.id.listaReservas);
        layoutEstadoVazio = view.findViewById(R.id.emptyStateCard);
        
        // Configurar RecyclerView
        adaptadorReservas = new AdaptadorReservas(reservas, this, tipoReserva);
        listaReservas.setLayoutManager(new LinearLayoutManager(getContext()));
        listaReservas.setAdapter(adaptadorReservas);
        
        Log.d(TAG, "View criada - listaReservas: " + (listaReservas != null) + ", layoutEstadoVazio: " + (layoutEstadoVazio != null));
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        carregarReservas();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Recarregar reservas quando o fragmento volta a ser visível
        // Isso garante que avaliações recentes sejam refletidas
        if (usuarioId != null) {
            Log.d(TAG, "Fragmento retomado, recarregando reservas");
            carregarReservas();
        }
    }

    public void carregarReservas() {
        Log.d(TAG, "Carregando reservas do tipo: " + tipoReserva + " para usuário: " + usuarioId);
        
        if (tipoReserva == null || usuarioId == null) {
            Log.e(TAG, "Tipo de reserva ou usuário não definido");
            mostrarEstadoVazio();
            return;
        }

        // Primeiro, atualizar reservas que não possuem ownerId
        GerenciadorFirebase.atualizarReservasSemOwnerId(usuarioId)
            .thenCompose(atualizadas -> {
                Log.d(TAG, "Reservas sem ownerId atualizadas: " + atualizadas);
                
                // Buscar reservas onde o usuário é locatário
                return GerenciadorFirebase.buscarReservasUsuario(usuarioId);
            })
            .thenAccept(reservasComoLocatario -> {
                if (getActivity() == null) return;
                
                // Buscar reservas onde o usuário é proprietário
                buscarReservasComoProprietario()
                    .thenAccept(reservasComoProprietario -> {
                        if (getActivity() == null) return;
                        
                        getActivity().runOnUiThread(() -> {
                            // Combinar todas as reservas
                            List<DocumentSnapshot> todasReservas = new ArrayList<>();
                            todasReservas.addAll(reservasComoLocatario);
                            todasReservas.addAll(reservasComoProprietario);
                            
                            List<DocumentSnapshot> reservasFiltradas = filtrarReservasPorTipo(todasReservas);
                            Log.d(TAG, "Reservas filtradas para " + tipoReserva + ": " + reservasFiltradas.size() + " de " + todasReservas.size());
                            
                            reservas.clear();
                            reservas.addAll(reservasFiltradas);
                            adaptadorReservas.notifyDataSetChanged();
                            
                            if (reservas.isEmpty()) {
                                mostrarEstadoVazio();
                            } else {
                                mostrarLista();
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Erro ao buscar reservas como proprietário: " + throwable.getMessage());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Usar apenas as reservas como locatário
                                List<DocumentSnapshot> reservasFiltradas = filtrarReservasPorTipo(reservasComoLocatario);
                                reservas.clear();
                                reservas.addAll(reservasFiltradas);
                                adaptadorReservas.notifyDataSetChanged();
                                
                                if (reservas.isEmpty()) {
                                    mostrarEstadoVazio();
                                } else {
                                    mostrarLista();
                                }
                            });
                        }
                        return null;
                    });
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Erro ao carregar reservas: " + throwable.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro ao carregar reservas", Toast.LENGTH_SHORT).show();
                        mostrarEstadoVazio();
                    });
                }
                return null;
            });
    }

    private List<DocumentSnapshot> filtrarReservasPorTipo(List<DocumentSnapshot> todasReservas) {
        List<DocumentSnapshot> reservasFiltradas = new ArrayList<>();
        
        for (DocumentSnapshot reserva : todasReservas) {
            String ownerId = reserva.getString("ownerId");
            String userId = reserva.getString("userId");
            
            boolean isMeuInstrumento = usuarioId.equals(ownerId);
            boolean isMeuInteresse = usuarioId.equals(userId);
            
            Log.d(TAG, "Reserva " + reserva.getId() + " - Owner: " + ownerId + ", User: " + userId);
            Log.d(TAG, "  - É meu instrumento: " + isMeuInstrumento + ", É meu interesse: " + isMeuInteresse);
            
            if ("meus_instrumentos".equals(tipoReserva) && isMeuInstrumento) {
                reservasFiltradas.add(reserva);
                Log.d(TAG, "  -> ADICIONADO como meu instrumento");
            } else if ("meus_interesses".equals(tipoReserva) && isMeuInteresse) {
                reservasFiltradas.add(reserva);
                Log.d(TAG, "  -> ADICIONADO como meu interesse");
            } else {
                Log.d(TAG, "  -> IGNORADO");
            }
        }
        
        Log.d(TAG, "Resultado da filtragem: " + reservasFiltradas.size() + " reservas");
        return reservasFiltradas;
    }

    private void mostrarEstadoVazio() {
        if (listaReservas != null && layoutEstadoVazio != null) {
            listaReservas.setVisibility(View.GONE);
            layoutEstadoVazio.setVisibility(View.VISIBLE);
            Log.d(TAG, "Mostrando estado vazio");
        }
    }

    private void mostrarLista() {
        if (listaReservas != null && layoutEstadoVazio != null) {
            listaReservas.setVisibility(View.VISIBLE);
            layoutEstadoVazio.setVisibility(View.GONE);
            Log.d(TAG, "Mostrando lista com " + reservas.size() + " reservas");
        }
    }

    @Override
    public void onReservaClick(DocumentSnapshot reserva) {
        // Implementar navegação para detalhes da reserva se necessário
        Log.d(TAG, "Reserva clicada: " + reserva.getId());
    }

    @Override
    public void onAvaliarReserva(DocumentSnapshot reserva) {
        Log.d(TAG, "Avaliar reserva: " + reserva.getId());
        
        if (getActivity() == null) {
            Log.e(TAG, "Activity é null, não é possível navegar");
            return;
        }
        
        // Determinar o tipo de avaliação baseado na aba
        if ("meus_instrumentos".equals(tipoReserva)) {
            // Na aba "Meus Instrumentos", avaliar o LOCATÁRIO
            navegarParaAvaliacaoUsuario(reserva);
        } else {
            // Na aba "Meus Interesses", avaliar o INSTRUMENTO (como antes)
            navegarParaAvaliacaoInstrumento(reserva);
        }
    }
    
    /**
     * Navega para a tela de avaliação de usuário (locatário)
     */
    private void navegarParaAvaliacaoUsuario(DocumentSnapshot reserva) {
        Log.d(TAG, "Navegando para avaliação de usuário");
        
        Intent intent = new Intent(getActivity(), AtividadeAvaliarUsuario.class);
        intent.putExtra("reserva_id", reserva.getId());
        
        // Adicionar dados extras se disponíveis
        String instrumentoId = reserva.getString("instrumentId");
        String userId = reserva.getString("userId"); // O locatário
        
        if (instrumentoId != null) {
            intent.putExtra("instrumento_id", instrumentoId);
        }
        if (userId != null) {
            intent.putExtra("locatario_id", userId);
        }
        
        Log.d(TAG, "Navegando para avaliação de usuário - Reserva: " + reserva.getId() + 
              ", Instrumento: " + instrumentoId + ", Locatário: " + userId);
        
        startActivity(intent);
    }
    
    /**
     * Navega para a tela de avaliação de instrumento
     */
    private void navegarParaAvaliacaoInstrumento(DocumentSnapshot reserva) {
        Log.d(TAG, "Navegando para avaliação de instrumento");
        
        Intent intent = new Intent(getActivity(), AtividadeAvaliarAluguel.class);
        intent.putExtra("reserva_id", reserva.getId());
        
        // Adicionar dados extras se disponíveis
        String instrumentoId = reserva.getString("instrumentId");
        String ownerId = reserva.getString("ownerId");
        
        if (instrumentoId != null) {
            intent.putExtra("instrumento_id", instrumentoId);
        }
        if (ownerId != null) {
            intent.putExtra("proprietario_id", ownerId);
        }
        
        Log.d(TAG, "Navegando para avaliação de instrumento - Reserva: " + reserva.getId() + 
              ", Instrumento: " + instrumentoId + ", Proprietário: " + ownerId);
        
        startActivity(intent);
    }

    /**
     * Busca reservas onde o usuário é proprietário do instrumento
     */
    private CompletableFuture<List<DocumentSnapshot>> buscarReservasComoProprietario() {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando reservas onde usuário é proprietário: " + usuarioId);
        
        // Primeiro, verificar se o usuário tem instrumentos próprios
        FirebaseFirestore.getInstance()
                .collection("instruments")
                .whereEqualTo("ownerId", usuarioId)
                .get()
                .addOnSuccessListener(instrumentsSnapshot -> {
                    Log.d(TAG, "Instrumentos próprios do usuário: " + instrumentsSnapshot.size());
                    
                    if (instrumentsSnapshot.isEmpty()) {
                        Log.d(TAG, "Usuário não tem instrumentos próprios - nenhuma reserva como proprietário possível");
                        futuro.complete(new ArrayList<>());
                        return;
                    }
                    
                    // Agora buscar reservas onde o usuário é proprietário
                    FirebaseFirestore.getInstance()
                            .collection("reservations")
                            .whereEqualTo("ownerId", usuarioId)
                            .get()
                            .addOnSuccessListener(snapshotConsulta -> {
                                List<DocumentSnapshot> reservas = snapshotConsulta.getDocuments();
                                Log.d(TAG, "Reservas como proprietário encontradas: " + reservas.size());
                                
                                // Log detalhado de cada reserva encontrada
                                for (int i = 0; i < reservas.size(); i++) {
                                    DocumentSnapshot reserva = reservas.get(i);
                                    Log.d(TAG, "Reserva " + (i+1) + ": " + reserva.getId());
                                    Log.d(TAG, "  - userId: " + reserva.getString("userId"));
                                    Log.d(TAG, "  - ownerId: " + reserva.getString("ownerId"));
                                    Log.d(TAG, "  - instrumentId: " + reserva.getString("instrumentId"));
                                    Log.d(TAG, "  - status: " + reserva.getString("status"));
                                    Log.d(TAG, "  - totalPrice: " + reserva.getDouble("totalPrice"));
                                }
                                
                                futuro.complete(reservas);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao buscar reservas como proprietário: " + erro.getMessage());
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar instrumentos próprios: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
}
