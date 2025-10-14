package com.example.instrumentaliza;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * FragmentoAvaliacoesPerfil - Fragmento de avaliações do perfil
 * 
 * Este fragmento exibe as avaliações recebidas pelo usuário como anunciante.
 * Atualmente mostra uma mensagem informativa sobre implementação futura.
 * 
 * Funcionalidades planejadas:
 * - Exibição de avaliações recebidas
 * - Sistema de estrelas e comentários
 * - Histórico de avaliações
 * 
 * @author Jhonata
 * @version 1.0
 */
public class FragmentoAvaliacoesPerfil extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("ProfileRatingsFragment", "onCreateView chamado");
        View view = inflater.inflate(R.layout.fragment_profile_ratings, container, false);
        
        // Por enquanto, apenas exibir uma mensagem de implementação futura
        TextView textoMensagem = view.findViewById(R.id.messageTextView);
        if (textoMensagem != null) {
            textoMensagem.setText("Funcionalidade de avaliações será implementada em breve!\n\nAqui você poderá ver todas as avaliações que outros usuários fizeram sobre você como anunciante.");
            Log.d("ProfileRatingsFragment", "Mensagem definida com sucesso");
        } else {
            Log.e("ProfileRatingsFragment", "textoMensagem é null");
        }
        
        Log.d("ProfileRatingsFragment", "onCreateView concluído");
        return view;
    }
}
