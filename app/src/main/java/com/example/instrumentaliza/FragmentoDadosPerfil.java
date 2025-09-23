package com.example.instrumentaliza;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * FragmentoDadosPerfil - Fragmento de dados do perfil
 * 
 * Este fragmento exibe as informações pessoais do usuário logado,
 * incluindo nome, email e telefone. É usado dentro da tela de
 * perfil para mostrar os dados básicos do usuário.
 * 
 * Funcionalidades principais:
 * - Exibição de dados pessoais do usuário
 * - Carregamento automático de dados do Firebase
 * - Atualização de dados quando necessário
 * - Tratamento de erros de carregamento
 * 
 * Características técnicas:
 * - Fragment do Android com ciclo de vida próprio
 * - Carregamento assíncrono de dados do Firebase
 * - Tratamento de estados de loading e erro
 * - Logs detalhados para debugging
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FragmentoDadosPerfil extends Fragment {
    
    // Constantes
    private static final String TAG = "DadosPerfil";
    
    // Componentes da interface
    private TextView textoNome, textoEmail, textoTelefone;
    
    // Autenticação
    private FirebaseAuth autenticacao;

    /**
     * Método chamado para criar a view do fragmento
     * 
     * @param inflater LayoutInflater para inflar a view
     * @param container ViewGroup pai do fragmento
     * @param savedInstanceState Estado anterior do fragmento
     * @return View inflada do fragmento
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "ProfileDataFragment onCreateView chamado");
        View view = inflater.inflate(R.layout.fragment_profile_data, container, false);
        
        // Inicializar Firebase
        autenticacao = FirebaseAuth.getInstance();
        
        // Inicializar views
        textoNome = view.findViewById(R.id.nameTextView);
        textoEmail = view.findViewById(R.id.emailTextView);
        textoTelefone = view.findViewById(R.id.phoneTextView);
        
        Log.d(TAG, "Views inicializadas: " + (textoNome != null ? "nome OK" : "nome NULL"));
        
        // Carregar dados do usuário
        carregarDadosUsuario();
        
        Log.d(TAG, "ProfileDataFragment onCreateView concluído");
        return view;
    }

    /**
     * Carrega os dados do usuário logado
     * 
     * Este método busca as informações do usuário no Firebase Firestore
     * e atualiza a interface com os dados encontrados. Trata erros
     * de carregamento e estados de usuário não encontrado.
     */
    private void carregarDadosUsuario() {
        Log.d(TAG, "loadUserData iniciado");
        try {
            FirebaseUser currentUser = autenticacao.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Usuário atual encontrado: " + currentUser.getEmail());
                
                // Exibir dados básicos do Firebase Auth
                String displayName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Não informado";
                String email = currentUser.getEmail();
                
                textoNome.setText(displayName);
                textoEmail.setText(email);
                
                Log.d(TAG, "Dados básicos definidos - Nome: " + displayName + ", Email: " + email);
                
                // Tentar carregar dados adicionais do Firestore
                GerenciadorFirebase.obterDadosUsuario(currentUser.getUid())
                        .thenAccept(userData -> {
                            Log.d(TAG, "Dados do Firestore recebidos: " + (userData != null ? userData.size() : "null"));
                            if (userData != null && !userData.isEmpty()) {
                                String firestoreName = (String) userData.get("name");
                                String phone = (String) userData.get("phone");
                                
                                Log.d(TAG, "Dados do Firestore - Nome: " + firestoreName + ", Telefone: " + phone);
                                
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (firestoreName != null && !firestoreName.isEmpty()) {
                                            textoNome.setText(firestoreName);
                                            Log.d(TAG, "Nome atualizado para: " + firestoreName);
                                        }
                                        if (phone != null && !phone.isEmpty()) {
                                            textoTelefone.setText(phone);
                                        } else {
                                            textoTelefone.setText("Não informado");
                                        }
                                        Log.d(TAG, "Todos os campos atualizados na UI");
                                    });
                                }
                            } else {
                                Log.d(TAG, "Dados do Firestore estão vazios");
                            }
                        })
                        .exceptionally(throwable -> {
                            Log.e(TAG, "Erro ao carregar dados do Firestore: " + throwable.getMessage(), throwable);
                            return null;
                        });
            } else {
                Log.e(TAG, "Usuário atual é null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar dados do usuário: " + e.getMessage(), e);
            if (getActivity() != null) {
                Toast.makeText(getActivity(), getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
