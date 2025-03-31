package com.grupoverona.selfcheckout;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Classe que implementa um listener UDP para receber mensagens do PDV
 * Otimizada para reduzir consumo de CPU e energia
 */
public class UdpListener {

    // Tamanho máximo do buffer para receber pacotes UDP
    private static final int MAX_PACKET_SIZE = 4096;

    // Endereço IP e porta do PDV remoto
    private final String remoteIpAddress;
    private final int port;

    // Socket UDP compartilhado para a porta 38800
    private static DatagramSocket sharedSocket;

    // Thread de escuta compartilhada
    private static Thread listenerThread;

    // Flag atômica para controlar se a thread está rodando
    private static final AtomicBoolean threadRunning = new AtomicBoolean(false);

    // Buffer reutilizável para recepção de dados
    private static byte[] sharedBuffer;

    // Flag para indicar se esta instância está ativa
    private boolean isActive = false;

    // Callback para processar as mensagens recebidas
    private Consumer<String> messageCallback;

    /**
     * Construtor
     * @param ipAddress Endereço IP e porta no formato IP:PORTA
     */
    public UdpListener(String ipAddress) {
        // Separa o IP e a porta (formato esperado: IP:PORTA)
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            this.remoteIpAddress = parts[0];
            this.port = Integer.parseInt(parts[1]);
        } else {
            this.remoteIpAddress = ipAddress;
            this.port = 38800; // Porta padrão do PDV
        }

        // Registra esta instância no registro central
        UdpListenerRegistry.addListener(this);
    }

    /**
     * Define o callback para receber as mensagens
     */
    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    /**
     * Inicia o listener UDP
     */
    public synchronized void start() {
        if (isActive) {
            return; // Já está ativo
        }

        isActive = true;

        try {
            // Inicia o socket compartilhado se ainda não estiver rodando
            if (sharedSocket == null) {
                initializeSharedSocket();
            }

            // Notifica que está ativo
            if (messageCallback != null) {
                messageCallback.accept("Ouvindo PDV: " + remoteIpAddress + " na porta " + port);
            }

        } catch (Exception e) {
            if (messageCallback != null) {
                messageCallback.accept("Erro ao iniciar listener: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * Inicializa o socket compartilhado
     */
    private synchronized void initializeSharedSocket() throws IOException {
        // Se já existe um socket, não faz nada
        if (sharedSocket != null) {
            return;
        }

        try {
            // Cria um novo socket na porta específica
            sharedSocket = new DatagramSocket(38800);
            // Aumenta o timeout para reduzir a frequência de verificações e economizar energia
            sharedSocket.setSoTimeout(5000); // 5 segundos de timeout

            // Inicializa o buffer compartilhado
            sharedBuffer = new byte[MAX_PACKET_SIZE];

            // Inicia a thread de escuta se não estiver rodando
            if (threadRunning.compareAndSet(false, true)) {
                startListenerThread();
            }
        } catch (IOException e) {
            // Se não conseguir abrir o socket, propaga a exceção
            if (messageCallback != null) {
                messageCallback.accept("Erro ao abrir socket: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Inicia a thread de escuta compartilhada
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                System.out.println("Iniciando thread de escuta UDP na porta 38800");

                // Packet para receber os dados (reutilizando o buffer compartilhado)
                DatagramPacket packet = new DatagramPacket(sharedBuffer, sharedBuffer.length);

                // Loop de recebimento
                while (threadRunning.get()) {
                    try {
                        // Aguarda pacote (com timeout de 5s configurado no socket)
                        sharedSocket.receive(packet);

                        // Obtém o IP de origem
                        String senderIp = packet.getAddress().getHostAddress();
                        int senderPort = packet.getPort();

                        // Converte os dados para string
                        String message = new String(
                                packet.getData(),
                                packet.getOffset(),
                                packet.getLength(),
                                StandardCharsets.UTF_8
                        ).trim();

                        // Só processa se não estiver vazia (otimizado para reduzir processamento)
                        if (!message.isEmpty()) {
                            // Distribui a mensagem para todos os listeners interessados neste IP
                            deliverMessage(senderIp, senderPort, message);
                        }

                        // Reinicia o packet para o próximo recebimento
                        packet.setLength(sharedBuffer.length);
                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout de recepção - apenas continua o loop
                        // Não faz nada aqui para reduzir processamento durante timeouts
                        continue;
                    } catch (IOException e) {
                        if (threadRunning.get()) { // Só loga se ainda estiver rodando
                            System.err.println("Erro ao receber pacote: " + e.getMessage());
                            // Pequena pausa para não sobrecarregar em caso de erros em sequência
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
                // Fecha o socket quando a thread terminar
                if (sharedSocket != null && !sharedSocket.isClosed()) {
                    sharedSocket.close();
                    sharedSocket = null;
                }
                System.out.println("Thread de escuta UDP encerrada");
            }
        }, "UDP-Listener-Thread");

        // Configura a thread como daemon para que não impeça o JVM de terminar
        listenerThread.setDaemon(true);

        // Prioridade mais baixa para reduzir consumo de CPU/energia
        listenerThread.setPriority(Thread.MIN_PRIORITY);

        listenerThread.start();
    }

    /**
     * Entrega a mensagem para todos os listeners interessados
     */
    private static void deliverMessage(String senderIp, int senderPort, String message) {
        UdpListener[] listeners = UdpListenerRegistry.getActiveListeners();

        boolean messageDelivered = false;

        for (UdpListener listener : listeners) {
            if (listener.isActive && listener.remoteIpAddress.equals(senderIp)) {
                // Apenas entrega se o IP corresponder
                if (listener.messageCallback != null) {
                    // Processa a mensagem apenas uma vez e reutiliza
                    if (!messageDelivered) {
                        String senderInfo = senderIp + ":" + senderPort;
                        String processedMessage = MessageProcessor.processUdpMessage(senderInfo, message);
                        listener.messageCallback.accept(processedMessage);
                        messageDelivered = true;
                    } else {
                        // Se já tiver processado a mensagem para outro listener do mesmo IP
                        // só reutiliza a mensagem processada
                        listener.messageCallback.accept("Recebido de " + senderIp + ":" + senderPort + ": " + message);
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

        if (messageCallback != null) {
            messageCallback.accept("Listener para " + remoteIpAddress + " encerrado");
        }

        // Remove do registro
        UdpListenerRegistry.removeListener(this);

        // Verifica se ainda há listeners ativos antes de encerrar a thread
        if (UdpListenerRegistry.getActiveListeners().length == 0) {
            threadRunning.set(false);
        }

        System.out.println("Listener para " + remoteIpAddress + " encerrado");
    }
}