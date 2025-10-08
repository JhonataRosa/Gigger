package com.example.instrumentaliza;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdaptadorSolicitacaoTabs extends FragmentStateAdapter {
    
    public AdaptadorSolicitacaoTabs(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return FragmentSolicitacaoTab.newInstance("solicitacoes_recebidas");
            case 1:
                return FragmentSolicitacaoTab.newInstance("solicitacoes_enviadas");
            default:
                return FragmentSolicitacaoTab.newInstance("solicitacoes_recebidas");
        }
    }

    @Override
    public int getItemCount() {
        return 2; // "Solicitações Recebidas" e "Solicitações Enviadas"
    }
}
