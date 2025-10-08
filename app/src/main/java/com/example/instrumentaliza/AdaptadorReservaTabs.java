package com.example.instrumentaliza;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdaptadorReservaTabs extends FragmentStateAdapter {
    
    public AdaptadorReservaTabs(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return FragmentReservaTab.newInstance("meus_instrumentos");
            case 1:
                return FragmentReservaTab.newInstance("meus_interesses");
            default:
                return FragmentReservaTab.newInstance("meus_instrumentos");
        }
    }

    @Override
    public int getItemCount() {
        return 2; // "Meus Instrumentos" e "Meus Interesses"
    }
}
