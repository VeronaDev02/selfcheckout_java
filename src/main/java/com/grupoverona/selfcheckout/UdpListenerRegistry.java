package com.grupoverona.selfcheckout;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe auxiliar para registrar e gerenciar listeners UDP ativos
 */
public class UdpListenerRegistry {
    // Lista de listeners ativos
    private static final List<UdpListener> activeListeners = new ArrayList<>();

    /**
     * Adiciona um listener ao registro
     * @param listener O listener a ser adicionado
     */
    public static synchronized void addListener(UdpListener listener) {
        if (!activeListeners.contains(listener)) {
            activeListeners.add(listener);
        }
    }

    /**
     * Remove um listener do registro
     * @param listener O listener a ser removido
     */
    public static synchronized void removeListener(UdpListener listener) {
        activeListeners.remove(listener);
    }

    /**
     * Obtém array com todos os listeners ativos
     * @return Array com os listeners ativos
     */
    public static synchronized UdpListener[] getActiveListeners() {
        return activeListeners.toArray(new UdpListener[0]);
    }
}