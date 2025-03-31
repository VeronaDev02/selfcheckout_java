package com.grupoverona.selfcheckout;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

/**
 * Classe que representa um quadrante da tela com uma câmera e um PDV
 */
public class CameraQuadrant {

    // Identificador do quadrante (0-3)
    private final int id;

    // Componentes da UI
    private final AnchorPane videoPane;
    private final AnchorPane logPane;
    private TextArea logTextArea;

    // Handler para streaming de vídeo
    // Mudança de DirectMediaHandler para VlcjMediaHandler para suporte a RTSP
    private VlcjMediaHandler mediaHandler;

    // Componente para ouvir UDP
    private UdpListener udpListener;

    /**
     * Construtor
     */
    public CameraQuadrant(int id, AnchorPane videoPane, AnchorPane logPane) {
        this.id = id;
        this.videoPane = videoPane;
        this.logPane = logPane;

        // Inicializa o componente de log
        initializeLogArea();
    }

    /**
     * Inicializa a área de texto para exibir os logs do PDV
     */
    private void initializeLogArea() {
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);

        // Aplica estilo ao TextArea
        logTextArea.setStyle("-fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");

        // Adiciona o TextArea ao pane de log com dimensões preenchendo todo o espaço
        AnchorPane.setTopAnchor(logTextArea, 0.0);
        AnchorPane.setBottomAnchor(logTextArea, 0.0);
        AnchorPane.setLeftAnchor(logTextArea, 0.0);
        AnchorPane.setRightAnchor(logTextArea, 0.0);

        logPane.getChildren().add(logTextArea);
    }

    /**
     * Conecta ao stream RTSP da câmera
     */
    public void connectToRtspStream(String rtspUrl) {
        if (mediaHandler == null) {
            // Usa VlcjMediaHandler em vez de DirectMediaHandler para suporte a RTSP
            mediaHandler = new VlcjMediaHandler();

            // Configuramos um callback separado que vai apenas para o console
            // e não para o TextArea de log do PDV
            mediaHandler.setLogCallback(message -> {
                // Log apenas no console, não no TextArea
                System.out.println("Stream Q" + id + ": " + message);
            });
        }

        try {
            // Apenas registramos a conexão no log do PDV
            appendToLog("Câmera conectada: " + rtspUrl);

            // Inicia o stream no painel de vídeo
            mediaHandler.connectToStream(rtspUrl, videoPane);
        } catch (Exception e) {
            // Erros críticos aparecem no log do PDV
            appendToLog("Erro ao conectar a câmera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Conecta ao fluxo UDP do PDV
     */
    public void connectToUdpStream(String ipAddress) {
        // Se já existir um listener, encerra-o primeiro
        if (udpListener != null) {
            udpListener.stop();
            udpListener = null;
        }

        try {
            // Cria um novo listener UDP com o IP e porta fornecidos
            udpListener = new UdpListener(ipAddress);

            // Configura o callback para receber os dados
            udpListener.setMessageCallback(message -> {
                // Como o UDP é assíncrono, precisamos atualizar a UI na thread JavaFX
                Platform.runLater(() -> appendToLog(message));
            });

            // Inicia o listener
            udpListener.start();

            // Atualiza o log
            appendToLog("PDV configurado: " + ipAddress);
        } catch (Exception e) {
            appendToLog("Erro ao conectar ao PDV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adiciona uma nova linha ao log
     */
    private void appendToLog(String message) {
        // Limita o tamanho do log para evitar problemas de memória
        if (logTextArea.getText().length() > 10000) {
            logTextArea.clear();
        }

        // Adiciona a mensagem com timestamp
        logTextArea.appendText(String.format("[%tT] %s%n", System.currentTimeMillis(), message));

        // Rola para o final
        logTextArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Notifica o quadrante sobre mudanças de layout (tela cheia, redimensionamento, etc.)
     * para que possa atualizar seus componentes conforme necessário
     */
    public void notifyLayoutChange() {
        // Forçar atualização do layout do videoPane
        videoPane.layout();

        // Chamamos o método de atualização de layout do mediaHandler
        if (mediaHandler != null) {
            mediaHandler.refreshLayout(videoPane);
            appendToLog("Layout atualizado - ajustando stream de vídeo");
        }
    }

    /**
     * Libera recursos ao fechar a aplicação
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

}