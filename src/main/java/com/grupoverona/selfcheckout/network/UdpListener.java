package com.grupoverona.selfcheckout.network;

import com.grupoverona.selfcheckout.util.MessageProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Listener UDP para receber mensagens do PDV.
 * Implementação otimizada para reduzir consumo de CPU e energia
 * através de um socket compartilhado.
 */
public class UdpListener {
    // Porta padrão para comunicação PDV
    private static final int DEFAULT_PORT = 38800;

    // Tamanho máximo do buffer para receber pacotes UDP
    private static final int MAX_PACKET_SIZE = 4096;

    // Configuração do PDV
    private final String remoteIpAddress;
    private final int port;

    // Socket UDP compartilhado
    private static DatagramSocket sharedSocket;
    private static Thread listenerThread;
    private static final AtomicBoolean threadRunning = new AtomicBoolean(false);
    private static byte[] sharedBuffer;

    // Estado deste listener
    private boolean isActive = false;

    // Callback para processar mensagens recebidas
    private Consumer<String> messageCallback;

    /**
     * Cria um listener para um PDV específico.
     * @param ipAddress Endereço no formato "IP:PORTA" ou apenas "IP" (usa porta padrão)
     */
    public UdpListener(String ipAddress) {
        // Separa o IP e a porta (formato esperado: IP:PORTA)
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            this.remoteIpAddress = parts[0];
            this.port = Integer.parseInt(parts[1]);
        } else {
            this.remoteIpAddress = ipAddress;
            this.port = DEFAULT_PORT;
        }

        // Registra no registro central de listeners
        UdpListenerRegistry.addListener(this);
    }

    /**
     * Define o callback para receber as mensagens
     * @param callback Função que será chamada quando mensagens forem recebidas
     */
    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    /**
     * Inicia o listener UDP
     */
    public synchronized void start() {
        if (isActive) {
            return; // Evita iniciar múltiplas vezes
        }

        isActive = true;

        try {
            // Inicia o socket compartilhado se necessário
            if (sharedSocket == null) {
                initializeSharedSocket();
            }

            // Notifica que está ativo
            notifyClient("Ouvindo PDV: " + remoteIpAddress + " na porta " + port);
        } catch (Exception e) {
            notifyClient("Erro ao iniciar listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envia mensagem para o cliente através do callback
     */
    private void notifyClient(String message) {
        if (messageCallback != null) {
            messageCallback.accept(message);
        }
    }

    /**
     * Inicializa o socket compartilhado usado por todos os listeners
     */
    private synchronized void initializeSharedSocket() throws IOException {
        if (sharedSocket != null) {
            return; // Já inicializado
        }

        try {
            // Cria um novo socket com configurações otimizadas
            sharedSocket = new DatagramSocket(DEFAULT_PORT);
            sharedSocket.setSoTimeout(5000); // 5 segundos de timeout

            // Inicializa buffer compartilhado
            sharedBuffer = new byte[MAX_PACKET_SIZE];

            // Inicia a thread de escuta
            if (threadRunning.compareAndSet(false, true)) {
                startListenerThread();
            }
        } catch (IOException e) {
            notifyClient("Erro ao abrir socket: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Inicia a thread de escuta compartilhada
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                System.out.println("Iniciando thread de escuta UDP na porta " + DEFAULT_PORT);
                DatagramPacket packet = new DatagramPacket(sharedBuffer, sharedBuffer.length);

                while (threadRunning.get()) {
                    try {
                        // Aguarda pacote (com timeout configurado)
                        sharedSocket.receive(packet);

                        // Obtém informações do remetente
                        String senderIp = packet.getAddress().getHostAddress();
                        int senderPort = packet.getPort();

                        // Converte dados para string
                        String message = new String(
                                packet.getData(),
                                packet.getOffset(),
                                packet.getLength(),
                                StandardCharsets.UTF_8
                        ).trim();

                        // Processa apenas se não estiver vazia
                        if (!message.isEmpty()) {
                            deliverMessage(senderIp, senderPort, message);
                        }

                        // Reinicia o packet para próximo recebimento
                        packet.setLength(sharedBuffer.length);
                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout normal, continua
                        continue;
                    } catch (IOException e) {
                        if (threadRunning.get()) {
                            System.err.println("Erro ao receber pacote: " + e.getMessage());
                            try {
                                Thread.sleep(1000); // Evita loop de erro intensivo
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            } finally {
                closeSocket();
            }
        }, "UDP-Listener-Thread");

        // Configurações para otimização
        listenerThread.setDaemon(true);
        listenerThread.setPriority(Thread.MIN_PRIORITY);
        listenerThread.start();
    }

    /**
     * Fecha o socket compartilhado
     */
    private static synchronized void closeSocket() {
        if (sharedSocket != null && !sharedSocket.isClosed()) {
            sharedSocket.close();
            sharedSocket = null;
            System.out.println("Socket UDP encerrado");
        }
    }

    /**
     * Distribui mensagens recebidas para os listeners interessados
     */
    private static void deliverMessage(String senderIp, int senderPort, String message) {
        UdpListener[] listeners = UdpListenerRegistry.getActiveListeners();
        boolean messageDelivered = false;
        String processedMessage = null;

        for (UdpListener listener : listeners) {
            if (listener.isActive && listener.remoteIpAddress.equals(senderIp)) {
                if (listener.messageCallback != null) {
                    // Processa a mensagem apenas uma vez para economizar processamento
                    if (!messageDelivered) {
                        String senderInfo = senderIp + ":" + senderPort;
                        processedMessage = MessageProcessor.processUdpMessage(senderInfo, message);
                        listener.messageCallback.accept(processedMessage);
                        messageDelivered = true;
                    } else {
                        // Reutiliza a mensagem já processada
                        listener.messageCallback.accept(processedMessage);
                    }
                }
            }
        }
    }

    /**
     * Para este listener UDP específico
     */
    public synchronized void stop() {
        if (!isActive) {
            return;
        }

        isActive = false;
        notifyClient("Listener para " + remoteIpAddress + " encerrado");
        UdpListenerRegistry.removeListener(this);

        // Verifica se ainda há listeners ativos
        if (UdpListenerRegistry.getActiveListeners().length == 0) {
            threadRunning.set(false);
        }
    }

    /**
     * @return O endereço IP remoto deste listener
     */
    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    /**
     * @return A porta deste listener
     */
    public int getPort() {
        return port;
    }

    /**
     * @return Se este listener está ativo
     */
    public boolean isActive() {
        return isActive;
    }
}