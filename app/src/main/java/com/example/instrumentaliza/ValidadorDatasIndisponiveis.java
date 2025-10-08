package com.example.instrumentaliza;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.material.datepicker.CalendarConstraints;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Validador customizado para marcar visualmente datas indisponíveis no MaterialDatePicker
 * 
 * Este validador implementa CalendarConstraints.DateValidator e Parcelable para permitir
 * que o MaterialDatePicker desabilite visualmente as datas que não estão disponíveis
 * para reserva (já reservadas ou marcadas manualmente como indisponíveis).
 * 
 * @author Jhonata
 */
public class ValidadorDatasIndisponiveis implements CalendarConstraints.DateValidator, Parcelable {
    
    private final List<Long> datasIndisponiveis;
    
    /**
     * Construtor principal
     * 
     * @param datasIndisponiveis Lista de timestamps (em milissegundos) das datas indisponíveis
     */
    public ValidadorDatasIndisponiveis(List<Long> datasIndisponiveis) {
        this.datasIndisponiveis = datasIndisponiveis != null ? new ArrayList<>(datasIndisponiveis) : new ArrayList<>();
    }
    
    /**
     * Construtor para Parcelable
     */
    protected ValidadorDatasIndisponiveis(Parcel in) {
        datasIndisponiveis = new ArrayList<>();
        in.readList(datasIndisponiveis, Long.class.getClassLoader());
    }
    
    /**
     * Verifica se uma data específica está disponível
     * 
     * @param date Timestamp da data a ser verificada
     * @return true se a data estiver disponível, false se estiver indisponível
     */
    @Override
    public boolean isValid(long date) {
        // Converter para início do dia para comparação precisa
        Date dataVerificacao = new Date(date);
        long timestampInicioDia = inicioDoDia(dataVerificacao);
        
        return !datasIndisponiveis.contains(timestampInicioDia);
    }
    
    /**
     * Converte uma data para o início do dia (00:00:00.000)
     * 
     * @param data Data a ser convertida
     * @return Timestamp do início do dia
     */
    private long inicioDoDia(Date data) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(data);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    
    /**
     * Adiciona uma data à lista de indisponíveis
     * 
     * @param data Data a ser marcada como indisponível
     */
    public void adicionarDataIndisponivel(Date data) {
        long timestamp = inicioDoDia(data);
        if (!datasIndisponiveis.contains(timestamp)) {
            datasIndisponiveis.add(timestamp);
        }
    }
    
    /**
     * Remove uma data da lista de indisponíveis
     * 
     * @param data Data a ser marcada como disponível
     */
    public void removerDataIndisponivel(Date data) {
        long timestamp = inicioDoDia(data);
        datasIndisponiveis.remove(timestamp);
    }
    
    /**
     * Verifica se uma data específica está indisponível
     * 
     * @param data Data a ser verificada
     * @return true se a data estiver indisponível, false caso contrário
     */
    public boolean isDataIndisponivel(Date data) {
        long timestamp = inicioDoDia(data);
        return datasIndisponiveis.contains(timestamp);
    }
    
    /**
     * Retorna o número de datas indisponíveis
     * 
     * @return Quantidade de datas indisponíveis
     */
    public int getQuantidadeDatasIndisponiveis() {
        return datasIndisponiveis.size();
    }
    
    // Implementação do Parcelable
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(datasIndisponiveis);
    }
    
    public static final Creator<ValidadorDatasIndisponiveis> CREATOR = new Creator<ValidadorDatasIndisponiveis>() {
        @Override
        public ValidadorDatasIndisponiveis createFromParcel(Parcel in) {
            return new ValidadorDatasIndisponiveis(in);
        }
        
        @Override
        public ValidadorDatasIndisponiveis[] newArray(int size) {
            return new ValidadorDatasIndisponiveis[size];
        }
    };
}
