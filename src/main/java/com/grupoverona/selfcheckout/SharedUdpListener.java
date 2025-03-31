package com.grupoverona.selfcheckout;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Classe singleton para gerenciar um único socket UDP que escuta em uma porta específica
 * e encaminha mensagens para os listeners registrados com base no IP de origem.
 */
public class SharedUdpListener {

    // Singleton instance
    private static SharedUdpListener instance;

    // Tamanho máximo do buffer para receber pacotes UDP
    private static final int MAX_PACKET_SIZE = 4096;

    // Mapa de sockets por porta
    private final Map<Integer, PortHandler> portHandlers = new ConcurrentHashMap<>();

    // Classe para gerenciar um socket em uma porta específica
    private class PortHandler {
        private final int port;
        private DatagramSocket socket;
        private Thread listenerThread;
        private volatile boolean running = false;
        private final Map<String, Consumer<String>> ipListeners = new ConcurrentHashMap<>();

        public PortHandler(int port) {
            this.port = port;
        }

        public synchronized void start() throws IOException {
            if (running) {
                return; // Já está rodando
            }

            try {
                // Tenta criar o socket na porta especificada
                socket = new DatagramSocket(port);
                socket.setSoTimeout(3000); // Timeout para evitar bloqueio indefinido

                running = true;

                // Inicia a thread de escuta
                listenerThread = new Thread(this::run, "UDP-Listener-" + port);
                listenerThread.setDaemon(true);
                listenerThread.start();

                System.out.println("Socket UDP compartilhado iniciado na porta " + port);
            } catch (BindException e) {
                System.err.println("Erro ao abrir socket na porta " + port + ": " + e.getMessage());
                throw e;
            }
        }

        private void run() {
            try {
                // Buffer para receber os dados
                byte[] buffer = new byte[MAX_PACKET_SIZE];

                // Packet para receber os dados
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Loop de recebimento
                while (running) {
                    try {
                        // Limpa o buffer
                        for (int i = 0; i < buffer.length; i++) {
                            buffer[i] = 0;
                        }

                        // Recebe um pacote (este método bloqueia até receber ou timeout)
                        socket.receive(packet);

                        // Obtém o IP de origem
                        String senderIp = packet.getAddress().getHostAddress();

                        // Converte os dados para string
                        String message = new String(
                                packet.getData(),
                                packet.getOffset(),
                                packet.getLength(),
                                StandardCharsets.UTF_8
                        );

                        // Processa a mensagem somente se não for vazia
                        if (!message.trim().isEmpty()) {
                            // Encaminha a mensagem para os listeners interessados neste IP
                            deliverMessageToListeners(senderIp, message);
                        }

                        // Reinicia o packet para o próximo recebimento
                        packet.setLength(buffer.length);
                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout de recepção - apenas continua o loop
                        continue;
                    } catch (IOException e) {
                        if (running) { // Só loga se ainda estiver rodando
                            System.err.println("Erro ao receber pacote na porta " + port + ": " + e.getMessage());
                            // Espera um pouco antes de tentar novamente
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            } finally {
                // Fecha o socket se a thread terminar
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Socket UDP compartilhado encerrado na porta " + port);
            }
        }

        private void deliverMessageToListeners(String senderIp, String message) {
            // Verifica se há algum listener registrado para este IP
            Consumer<String> listener = ipListeners.get(senderIp);
            if (listener != null) {
                listener.accept("Recebido de " + senderIp + ": " + message);
            }

            // Verifica se há algum listener "curinga" (0.0.0.0) que recebe mensagens de qualquer IP
            listener = ipListeners.get("0.0.0.0");
            if (listener != null) {
                listener.accept("Recebido de " + senderIp + ": " + message);
            }
        }

        public synchronized void stop() {
            if (!running) {
                return;
            }

            running = false;

            if (socket != null) {
                socket.close();
            }

            if (listenerThread != null) {
                listenerThread.interrupt();
                try {
                    listenerThread.join(1000); // Espera um segundo para a thread terminar
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void registerListener(String ip, Consumer<String> callback) {
            ipListeners.put(ip, callback);
            System.out.println("Registrado listener para IP " + ip + " na porta " + port);
        }

        public void unregisterListener(String ip) {
            ipListeners.remove(ip);
            System.out.println("Removido listener para IP " + ip + " da porta " + port);

            // Se não há mais listeners registrados, podemos parar este handler
            if (ipListeners.isEmpty()) {
                stop();
                portHandlers.remove(port);
            }
        }
    }

    // Construtor privado para o singleton
    private SharedUdpListener() {
        // Inicializa mapa
    }

    /**
     * Obtém a instância singleton
     */
    public static synchronized SharedUdpListener getInstance() {
        if (instance == null) {
            instance = new SharedUdpListener();
        }
        return instance;
    }

    /**
     * Registra um listener para receber mensagens de um IP específico em uma porta específica
     * @param ip O IP do PDV
     * @param port A porta do PDV
     * @param callback O callback para receber as mensagens
     * @return true se o registro foi bem sucedido, false caso contrário
     */
    public boolean registerListener(String ip, int port, Consumer<String> callback) {
        try {
            // Obtém ou cria o handler para esta porta
            PortHandler handler = portHandlers.computeIfAbsent(port, k -> new PortHandler(port));

            // Inicia o handler se ainda não foi iniciado
            if (!handler.running) {
                handler.start();
            }

            // Registra o callback para este IP
            handler.registerListener(ip, callback);

            return true;
        } catch (IOException e) {
            System.err.println("Erro ao registrar listener para " + ip + ":" + port + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove um listener registrado
     * @param ip O IP do PDV
     * @param port A porta do PDV
     */
    public void unregisterListener(String ip, int port) {
        PortHandler handler = portHandlers.get(port);
        if (handler != null) {
            handler.unregisterListener(ip);
        }
    }
}