package com.grupoverona.selfcheckout.media;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.util.function.Consumer;

/**
 * Gerenciador de mídia que utiliza a biblioteca VLCj para
 * reprodução de streams RTSP em componentes JavaFX.
 */
public class VlcjMediaHandler {

    // Opções padrão para otimização de streaming RTSP
    private static final String[] VLC_OPTIONS = {
            "--no-video-title-show",   // Não mostra título do vídeo
            "--quiet",                 // Reduz logs do VLC
            "--quiet-synchro",         // Reduz logs de sincronização
            "--network-caching=1000",  // 1000ms de buffer para estabilidade da rede
            "--rtsp-caching=300",      // 300ms de buffer específico para RTSP
            "--rtsp-tcp",              // Força uso de TCP para RTSP (mais estável)
            "--no-drop-late-frames"    // Não descarta frames atrasados
    };

    // Componentes VLC
    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;

    // Componente JavaFX para exibição
    private ImageView imageView;

    // Callback para logs
    private Consumer<String> logCallback;

    /**
     * Inicializa o manipulador de mídia
     */
    public VlcjMediaHandler() {
        try {
            mediaPlayerFactory = new MediaPlayerFactory(VLC_OPTIONS);
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
            log("Inicializado VLCj Media Handler");
        } catch (Exception e) {
            log("Erro ao inicializar VLCj: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Define o callback para logs
     */
    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    /**
     * Conecta a um stream e exibe no painel
     * @param url URL do stream (ex: rtsp://...)
     * @param videoPane Painel onde o vídeo será exibido
     */
    public void connectToStream(String url, AnchorPane videoPane) {
        stop();

        try {
            log("Iniciando conexão com: " + url);

            Platform.runLater(() -> {
                setupVideoPane(videoPane);
                setupMediaPlayerEvents();
                mediaPlayer.media().play(url);
            });
        } catch (Exception e) {
            log("Erro ao conectar ao stream: " + e.getMessage());
            e.printStackTrace();
            showErrorInUI(videoPane, url, e.getMessage());
        }
    }

    /**
     * Configura o painel de vídeo com ImageView
     */
    private void setupVideoPane(AnchorPane videoPane) {
        videoPane.getChildren().clear();

        // Cria o ImageView para exibir o vídeo
        imageView = new ImageView();
        imageView.setPreserveRatio(false);  // Preenche todo o espaço

        // Dimensiona para preencher o espaço disponível
        imageView.setFitWidth(videoPane.getWidth());
        imageView.setFitHeight(videoPane.getHeight());

        // Vincula dimensões ao painel
        imageView.fitWidthProperty().bind(videoPane.widthProperty());
        imageView.fitHeightProperty().bind(videoPane.heightProperty());

        // Adiciona o ImageView ao painel
        videoPane.getChildren().add(imageView);
        AnchorPane.setTopAnchor(imageView, 0.0);
        AnchorPane.setBottomAnchor(imageView, 0.0);
        AnchorPane.setLeftAnchor(imageView, 0.0);
        AnchorPane.setRightAnchor(imageView, 0.0);

        // Configura o VLC para renderizar no ImageView
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(imageView));
    }

    /**
     * Configura eventos do media player
     */
    private void setupMediaPlayerEvents() {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                log("Reprodução iniciada");
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                log("Erro durante a reprodução");
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                if (newCache == 100f) {
                    log("Buffer completo");
                }
            }
        });
    }

    /**
     * Exibe mensagem de erro na UI
     */
    private void showErrorInUI(AnchorPane videoPane, String url, String errorMessage) {
        Platform.runLater(() -> {
            videoPane.getChildren().clear();

            Label errorLabel = new Label("Erro ao conectar ao stream\n" + url + "\n" + errorMessage);
            errorLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10px;");

            videoPane.getChildren().add(errorLabel);
            AnchorPane.setTopAnchor(errorLabel, 10.0);
            AnchorPane.setLeftAnchor(errorLabel, 10.0);
        });
    }

    /**
     * Para a reprodução do stream
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            log("Stream parado");
        }
    }

    /**
     * Atualiza o layout após redimensionamento
     */
    public void refreshLayout(AnchorPane videoPane) {
        if (imageView == null) {
            return;
        }

        Platform.runLater(() -> {
            // Renova vinculações para forçar atualização
            imageView.fitWidthProperty().unbind();
            imageView.fitHeightProperty().unbind();

            imageView.setFitWidth(videoPane.getWidth());
            imageView.setFitHeight(videoPane.getHeight());

            imageView.fitWidthProperty().bind(videoPane.widthProperty());
            imageView.fitHeightProperty().bind(videoPane.heightProperty());

            log("Layout atualizado - dimensões: " + videoPane.getWidth() + "x" + videoPane.getHeight());
        });
    }

    /**
     * Log de mensagens
     */
    private void log(String message) {
        System.out.println("VlcjMediaHandler: " + message);
        if (logCallback != null) {
            Platform.runLater(() -> logCallback.accept(message));
        }
    }

    /**
     * Libera todos os recursos
     */
    public void dispose() {
        stop();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mediaPlayerFactory != null) {
            mediaPlayerFactory.release();
            mediaPlayerFactory = null;
        }

        log("Recursos liberados");
    }
}