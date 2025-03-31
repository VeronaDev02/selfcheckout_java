package com.grupoverona.selfcheckout;

import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import javafx.scene.image.ImageView;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Manipulador de mídia usando VLCj para suporte a RTSP
 */
public class VlcjMediaHandler {

    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;
    private ImageView imageView;
    private Consumer<String> logCallback;

    /**
     * Construtor
     */
    public VlcjMediaHandler() {
        // Inicializa o VLC com parâmetros padrão e opções de rede
        String[] standardOptions = {
                "--no-video-title-show",
                "--quiet",
                "--quiet-synchro",
                "--network-caching=1000", // 1000ms de buffer para melhorar a estabilidade da rede
                "--rtsp-caching=300",     // 300ms de buffer específico para RTSP
                "--rtsp-tcp",             // Força uso de TCP para RTSP (mais estável que UDP)
                "--no-drop-late-frames",  // Não descarta frames atrasados
        };

        try {
            mediaPlayerFactory = new MediaPlayerFactory(standardOptions);
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
            log("Inicializado VLCj Media Handler");
        } catch (Exception e) {
            log("Erro ao inicializar VLCj: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Define o callback para log
     */
    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    /**
     * Conecta a um stream e o exibe no pane especificado
     */
    public void connectToStream(String url, AnchorPane videoPane) {
        // Para qualquer reprodução anterior
        stop();

        try {
            log("Iniciando conexão com: " + url);

            // Limpa o pane
            Platform.runLater(() -> {
                videoPane.getChildren().clear();

                // Cria o ImageView para exibir o vídeo
                imageView = new ImageView();

                // Configura para não preservar a proporção, permitindo preencher completamente
                imageView.setPreserveRatio(false);

                // Configura para esticar a imagem para preencher o espaço disponível
                imageView.setFitWidth(videoPane.getWidth());
                imageView.setFitHeight(videoPane.getHeight());

                // Vincula as dimensões do ImageView ao tamanho do painel
                imageView.fitWidthProperty().bind(videoPane.widthProperty());
                imageView.fitHeightProperty().bind(videoPane.heightProperty());

                // Adiciona listeners para ajustar quando o painel for redimensionado
                videoPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                    Platform.runLater(() -> {
                        if (imageView != null) {
                            // Recalcula para ocupar todo o espaço
                            imageView.setFitWidth(newVal.doubleValue());
                        }
                    });
                });

                videoPane.heightProperty().addListener((obs, oldVal, newVal) -> {
                    Platform.runLater(() -> {
                        if (imageView != null) {
                            // Recalcula para ocupar todo o espaço
                            imageView.setFitHeight(newVal.doubleValue());
                        }
                    });
                });

                // Adiciona o ImageView ao pane com configurações de ancoragem absoluta
                videoPane.getChildren().add(imageView);
                AnchorPane.setTopAnchor(imageView, 0.0);
                AnchorPane.setBottomAnchor(imageView, 0.0);
                AnchorPane.setLeftAnchor(imageView, 0.0);
                AnchorPane.setRightAnchor(imageView, 0.0);

                // Configura o VLC para renderizar no ImageView
                mediaPlayer.videoSurface().set(new ImageViewVideoSurface(imageView));

                // Configura eventos
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

                // Inicia a reprodução
                mediaPlayer.media().play(url);
            });

        } catch (Exception e) {
            log("Erro ao conectar ao stream: " + e.getMessage());
            e.printStackTrace();

            // Mostra o erro na UI
            Platform.runLater(() -> {
                videoPane.getChildren().clear();
                javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                        "Erro ao conectar ao stream\n" + url + "\n" + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10px;");
                videoPane.getChildren().add(errorLabel);
                AnchorPane.setTopAnchor(errorLabel, 10.0);
                AnchorPane.setLeftAnchor(errorLabel, 10.0);
            });
        }
    }

    /**
     * Para a reprodução e libera recursos
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            log("Stream parado");
        }
    }

    /**
     * Atualiza o layout após redimensionamento da janela
     * Este método pode ser chamado quando a janela é redimensionada ou
     * quando o modo de tela cheia é alternado
     */
    public void refreshLayout(AnchorPane videoPane) {
        // Se não tivermos um imageView válido, não há nada a fazer
        if (imageView == null) {
            return;
        }

        Platform.runLater(() -> {
            // Forçar nova vinculação das dimensões
            imageView.fitWidthProperty().unbind();
            imageView.fitHeightProperty().unbind();

            // Definir dimensões explícitas
            imageView.setFitWidth(videoPane.getWidth());
            imageView.setFitHeight(videoPane.getHeight());

            // Vincular novamente
            imageView.fitWidthProperty().bind(videoPane.widthProperty());
            imageView.fitHeightProperty().bind(videoPane.heightProperty());

            log("Layout atualizado - dimensões: " + videoPane.getWidth() + "x" + videoPane.getHeight());
        });
    }

    /**
     * Método auxiliar para log
     */
    private void log(String message) {
        System.out.println("VlcjMediaHandler: " + message);
        if (logCallback != null) {
            Platform.runLater(() -> logCallback.accept(message));
        }
    }

    /**
     * Libera recursos quando não for mais necessário
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