package com.example.instrumentaliza;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * AdaptadorChatTabs - Adaptador para ViewPager2 das abas de chat
 * 
 * Gerencia os fragments das abas "Meus Anúncios" e "Meus Interesses"
 */
public class AdaptadorChatTabs extends FragmentStateAdapter {
    
    public AdaptadorChatTabs(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return FragmentChatTab.newInstance(FragmentChatTab.TIPO_MEUS_ANUNCIOS);
            case 1:
                return FragmentChatTab.newInstance(FragmentChatTab.TIPO_MEUS_INTERESSES);
            default:
                return FragmentChatTab.newInstance(FragmentChatTab.TIPO_MEUS_ANUNCIOS);
        }
    }
    
    @Override
    public int getItemCount() {
        return 2; // Meus Anúncios e Meus Interesses
    }
}
