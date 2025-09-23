package com.example.instrumentaliza;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * AdaptadorTabsPerfil - Adaptador para ViewPager2 das abas do perfil
 * 
 * Este adaptador gerencia as abas (tabs) da tela de perfil do usuário,
 * controlando qual fragmento é exibido em cada posição do ViewPager2.
 * 
 * Abas disponíveis:
 * - Tab 0: FragmentoDadosPerfil - Dados pessoais do usuário
 * - Tab 1: FragmentoAvaliacoesPerfil - Avaliações recebidas pelo usuário
 * 
 * Características técnicas:
 * - Herda de FragmentStateAdapter para gerenciar fragments
 * - Implementa padrão de tabs com ViewPager2
 * - Cria fragments dinamicamente conforme necessário
 * - Otimizado para performance com lazy loading
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdaptadorTabsPerfil extends FragmentStateAdapter {

    /**
     * Construtor do adaptador
     * 
     * @param fragmentActivity Activity que contém o ViewPager2
     */
    public AdaptadorTabsPerfil(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Cria o fragmento correspondente à posição da aba
     * 
     * @param position Posição da aba (0 = Dados, 1 = Avaliações)
     * @return Fragment correspondente à posição
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FragmentoDadosPerfil(); // Tab "Meus Dados"
            case 1:
                return new FragmentoAvaliacoesPerfil(); // Tab "Minhas Avaliações"
            default:
                return new FragmentoDadosPerfil();
        }
    }

    /**
     * Retorna o número total de abas
     * 
     * @return Número de abas (2)
     */
    @Override
    public int getItemCount() {
        return 2; // Dois tabs
    }
}
