package com.grupoverona.selfcheckout.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro centralizado de listeners UDP ativos.
 * Esta classe permite que múltiplos listeners compartilhem
 * um único socket, otimizando recursos e consumo de energia.
 */
public class UdpListenerRegistry {

    // Lista thread-safe de listeners ativos
    private static final List<UdpListener> activeListeners =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Adiciona um listener ao registro
     * @param listener O listener a ser adicionado
     */
    public static void addListener(UdpListener listener) {
        synchronized (activeListeners) {
            if (!activeListeners.contains(listener)) {
                activeListeners.add(listener);
                System.out.println("Listener registrado para " + listener.getRemoteIpAddress() +
                        " na porta " + listener.getPort());
            }
        }
    }

    /**
     * Remove um listener do registro
     * @param listener O listener a ser removido
     */
    public static void removeListener(UdpListener listener) {
        synchronized (activeListeners) {
            if (activeListeners.remove(listener)) {
                System.out.println("Listener removido para " + listener.getRemoteIpAddress() +
                        " na porta " + listener.getPort());
            }
        }
    }

    /**
     * Obtém array com todos os listeners ativos
     * @return Array com os listeners ativos
     */
    public static UdpListener[] getActiveListeners() {
        synchronized (activeListeners) {
            return activeListeners.toArray(new UdpListener[0]);
        }
    }

    /**
     * Obtém listeners ativos para um IP específico
     * @param ipAddress O endereço IP
     * @return Lista de listeners para o IP
     */
    public static List<UdpListener> getListenersForIp(String ipAddress) {
        List<UdpListener> result = new ArrayList<>();

        synchronized (activeListeners) {
            for (UdpListener listener : activeListeners) {
                if (listener.isActive() && listener.getRemoteIpAddress().equals(ipAddress)) {
                    result.add(listener);
                }
            }
        }

        return result;
    }

    /**
     * @return Número atual de listeners ativos
     */
    public static int getActiveListenerCount() {
        synchronized (activeListeners) {
            return activeListeners.size();
        }
    }

    /**
     * Libera todos os listeners e recursos
     */
    public static void disposeAll() {
        synchronized (activeListeners) {
            UdpListener[] listeners = getActiveListeners();
            for (UdpListener listener : listeners) {
                listener.stop();
            }
            activeListeners.clear();
        }
        System.out.println("Todos os listeners UDP foram encerrados");
    }
}