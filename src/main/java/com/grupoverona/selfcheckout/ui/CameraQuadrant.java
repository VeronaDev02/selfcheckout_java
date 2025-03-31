package com.grupoverona.selfcheckout.ui;

import com.grupoverona.selfcheckout.media.VlcjMediaHandler;
import com.grupoverona.selfcheckout.network.UdpListener;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

/**
 * Componente UI que representa um quadrante da tela contendo:
 * - Uma visualização de câmera (stream RTSP)
 * - Um painel de log para mensagens do PDV (via UDP)
 */
public class CameraQuadrant {
    // Tamanho máximo do log para evitar problemas de memória
    private static final int MAX_LOG_SIZE = 10000;

    // Identificador do quadrante (0-3)
    private final int id;

    // Componentes da UI
    private final AnchorPane videoPane;
    private final AnchorPane logPane;
    private TextArea logTextArea;

    // Componentes para streaming e comunicação
    private VlcjMediaHandler mediaHandler;
    private UdpListener udpListener;

    /**
     * Cria um novo quadrante de câmera
     * @param id Identificador do quadrante (0-3)
     * @param videoPane Painel para exibição do vídeo
     * @param logPane Painel para exibição dos logs
     */
    public CameraQuadrant(int id, AnchorPane videoPane, AnchorPane logPane) {
        this.id = id;
        this.videoPane = videoPane;
        this.logPane = logPane;

        initializeLogArea();
    }

    /**
     * Inicializa a área de log com estilo adequado
     */
    private void initializeLogArea() {
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");

        // Preenche todo o espaço do pane
        AnchorPane.setTopAnchor(logTextArea, 0.0);
        AnchorPane.setBottomAnchor(logTextArea, 0.0);
        AnchorPane.setLeftAnchor(logTextArea, 0.0);
        AnchorPane.setRightAnchor(logTextArea, 0.0);

        logPane.getChildren().add(logTextArea);
    }

    /**
     * Conecta à câmera via RTSP
     * @param rtspUrl URL do stream RTSP da câmera
     */
    public void connectToRtspStream(String rtspUrl) {
        if (mediaHandler == null) {
            mediaHandler = new VlcjMediaHandler();

            // Configura log separado para o console
            mediaHandler.setLogCallback(message ->
                    System.out.println("Stream Q" + id + ": " + message)
            );
        }

        try {
            appendToLog("Câmera conectada: " + rtspUrl);
            mediaHandler.connectToStream(rtspUrl, videoPane);
        } catch (Exception e) {
            appendToLog("Erro ao conectar a câmera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Conecta ao PDV via UDP
     * @param ipAddress Endereço IP:PORTA do PDV (ou apenas IP para porta padrão)
     */
    public void connectToUdpStream(String ipAddress) {
        // Encerra o listener anterior se existir
        if (udpListener != null) {
            udpListener.stop();
            udpListener = null;
        }

        try {
            udpListener = new UdpListener(ipAddress);

            // Configura callback para receber mensagens na UI
            udpListener.setMessageCallback(message ->
                    Platform.runLater(() -> appendToLog(message))
            );

            udpListener.start();
            appendToLog("PDV configurado: " + ipAddress);
        } catch (Exception e) {
            appendToLog("Erro ao conectar ao PDV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adiciona texto ao log com controle de tamanho
     * @param message Mensagem a ser adicionada
     */
    private void appendToLog(String message) {
        // Limita o tamanho do log para evitar problemas de memória
        if (logTextArea.getText().length() > MAX_LOG_SIZE) {
            logTextArea.clear();
        }

        // Adiciona a mensagem com timestamp
        logTextArea.appendText(String.format("[%tT] %s%n", System.currentTimeMillis(), message));

        // Rola para o final
        logTextArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Notifica o quadrante sobre mudanças de layout
     * (redimensionamento, tela cheia, etc.)
     */
    public void notifyLayoutChange() {
        // Forçamos o recálculo do layout para garantir dimensões atualizadas
        videoPane.layout();

        // Registramos as dimensões atuais para debug
        double width = videoPane.getWidth();
        double height = videoPane.getHeight();

        appendToLog("Atualizando layout - dimensões: " + width + "x" + height);

        // Se as dimensões forem muito pequenas ou zero, tentamos forçar o cálculo
        if (width < 10 || height < 10) {
            // Aguarda um pouco para permitir que o JavaFX calcule o layout
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Forçamos novamente o layout
            videoPane.layout();
            width = videoPane.getWidth();
            height = videoPane.getHeight();

            appendToLog("Dimensões após forçar layout: " + width + "x" + height);
        }

        if (mediaHandler != null) {
            // Atualizamos o layout do vídeo
            mediaHandler.refreshLayout(videoPane);
            appendToLog("Stream de vídeo ajustado para o novo tamanho");

            // Segunda chamada após um breve atraso para garantir que funcione
            Platform.runLater(() -> {
                try {
                    Thread.sleep(100);
                    mediaHandler.refreshLayout(videoPane);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * Libera todos os recursos utilizados pelo quadrante
     */
    public void dispose() {
        if (mediaHandler != null) {
            mediaHandler.dispose();
            mediaHandler = null;
        }

        if (udpListener != null) {
            udpListener.stop();
            udpListener = null;
        }
    }

    /**
     * @return O ID deste quadrante
     */
    public int getId() {
        return id;
    }
}