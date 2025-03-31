package com.grupoverona.selfcheckout.app;

import com.grupoverona.selfcheckout.ui.CameraQuadrant;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador principal da aplicação, responsável por gerenciar:
 * - Quadrantes de câmeras/PDVs
 * - Interações da UI (botões, campos de texto)
 * - Modo tela cheia
 */
public class MainController {

    // GridPane principal contendo os quadrantes (fx:id no FXML)
    @FXML
    private GridPane grid_quadrante;

    // Referência ao estágio (janela) principal
    private Stage mainStage;

    // Controles da UI que serão buscados via lookup
    private Button btn_tela_cheia;

    // Campos para RTSP
    private TextField txtField_rtsp1, txtField_rtsp2, txtField_rtsp3, txtField_rtsp4;
    private Button btn_rtsp1, btn_rtsp2, btn_rtsp3, btn_rtsp4;

    // Campos para IP
    private TextField txtField_ip1, txtField_ip2, txtField_ip3, txtField_ip4;
    private Button btn_ip1, btn_ip2, btn_ip3, btn_ip4;

    // Lista dos quadrantes de câmeras (máximo 4)
    private final List<CameraQuadrant> quadrants = new ArrayList<>(4);

    /**
     * Define o estágio principal
     */
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    /**
     * Inicialização chamada pelo JavaFX após carregamento do FXML
     */
    @FXML
    public void initialize() {
        System.out.println("MainController inicializado");
        System.out.println("grid_quadrante: " + (grid_quadrante != null ? "encontrado" : "nulo"));

        // Aguarda até que a UI esteja totalmente carregada
        Platform.runLater(this::setupAfterUIReady);
    }

    /**
     * Configura a aplicação após a UI estar completamente carregada
     */
    private void setupAfterUIReady() {
        if (grid_quadrante == null) {
            System.err.println("ERRO: grid_quadrante não encontrado no FXML.");
            return;
        }

        try {
            Scene scene = grid_quadrante.getScene();
            if (scene != null) {
                // Busca todos os controles da UI pelo ID
                lookupControls(scene);
                logControlsFound();

                // Inicializa os quadrantes
                setupQuadrants();

                // Configura os botões de conexão
                setupConnections();

                // Configura o botão de tela cheia
                setupFullScreenButton();
            } else {
                System.err.println("ERRO: Scene é nula, não é possível buscar controles");
            }
        } catch (Exception e) {
            System.err.println("ERRO ao configurar controles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra quais controles foram encontrados
     */
    private void logControlsFound() {
        System.out.println("btn_tela_cheia: " + (btn_tela_cheia != null ? "encontrado" : "nulo"));
        System.out.println("btn_rtsp1: " + (btn_rtsp1 != null ? "encontrado" : "nulo"));
        System.out.println("txtField_rtsp1: " + (txtField_rtsp1 != null ? "encontrado" : "nulo"));
    }

    /**
     * Busca todos os controles da UI pelo ID
     */
    private void lookupControls(Scene scene) {
        // Botão de tela cheia
        btn_tela_cheia = (Button) scene.lookup("#btn_tela_cheia");

        // TextFields para RTSP
        txtField_rtsp1 = (TextField) scene.lookup("#txtField_rtsp1");
        txtField_rtsp2 = (TextField) scene.lookup("#txtField_rtsp2");
        txtField_rtsp3 = (TextField) scene.lookup("#txtField_rtsp3");
        txtField_rtsp4 = (TextField) scene.lookup("#txtField_rtsp4");

        // TextFields para IP
        txtField_ip1 = (TextField) scene.lookup("#txtField_ip1");
        txtField_ip2 = (TextField) scene.lookup("#txtField_ip2");
        txtField_ip3 = (TextField) scene.lookup("#txtField_ip3");
        txtField_ip4 = (TextField) scene.lookup("#txtField_ip4");

        // Botões RTSP
        btn_rtsp1 = (Button) scene.lookup("#btn_rtsp1");
        btn_rtsp2 = (Button) scene.lookup("#btn_rtsp2");
        btn_rtsp3 = (Button) scene.lookup("#btn_rtsp3");
        btn_rtsp4 = (Button) scene.lookup("#btn_rtsp4");

        // Botões IP
        btn_ip1 = (Button) scene.lookup("#btn_ip1");
        btn_ip2 = (Button) scene.lookup("#btn_ip2");
        btn_ip3 = (Button) scene.lookup("#btn_ip3");
        btn_ip4 = (Button) scene.lookup("#btn_ip4");
    }

    /**
     * Inicializa os quadrantes de câmera/PDV
     */
    private void setupQuadrants() {
        try {
            // Cria quadrantes em uma grade 2x2
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    GridPane quadrantGrid = findQuadrantGridPane(row, col);

                    if (quadrantGrid != null) {
                        // Obtém panes para vídeo e log
                        AnchorPane logPane = (AnchorPane) quadrantGrid.getChildren().get(0);
                        AnchorPane videoPane = (AnchorPane) quadrantGrid.getChildren().get(1);

                        // Cria o quadrante
                        int index = row * 2 + col;
                        quadrants.add(new CameraQuadrant(index, videoPane, logPane));
                        System.out.println("Quadrante " + index + " criado com sucesso!");
                    } else {
                        System.err.println("ERRO: GridPane não encontrado para quadrante [" + row + "," + col + "]");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ERRO ao criar quadrantes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encontra o GridPane de um quadrante específico
     */
    private GridPane findQuadrantGridPane(int row, int col) {
        for (javafx.scene.Node node : grid_quadrante.getChildren()) {
            if (node instanceof GridPane) {
                Integer rowIndex = GridPane.getRowIndex(node);
                Integer colIndex = GridPane.getColumnIndex(node);

                // GridPane.get*Index pode retornar null para índice 0
                int nodeRow = (rowIndex == null) ? 0 : rowIndex;
                int nodeCol = (colIndex == null) ? 0 : colIndex;

                if (nodeRow == row && nodeCol == col) {
                    return (GridPane) node;
                }
            }
        }
        return null;
    }

    /**
     * Configura todas as conexões (RTSP e UDP)
     */
    private void setupConnections() {
        setupRtspConnections();
        setupUdpConnections();
    }

    /**
     * Configura os botões de conexão RTSP
     */
    private void setupRtspConnections() {
        configureRtspButton(btn_rtsp1, txtField_rtsp1, 0);
        configureRtspButton(btn_rtsp2, txtField_rtsp2, 1);
        configureRtspButton(btn_rtsp3, txtField_rtsp3, 2);
        configureRtspButton(btn_rtsp4, txtField_rtsp4, 3);
    }

    /**
     * Configura um botão RTSP específico
     */
    private void configureRtspButton(Button button, TextField textField, int quadrantIndex) {
        if (button != null && quadrants.size() > quadrantIndex) {
            System.out.println("Configurando botão RTSP para quadrante " + quadrantIndex);

            button.setOnAction(event -> {
                System.out.println("Botão RTSP clicado para quadrante " + quadrantIndex);

                if (textField != null) {
                    String rtspUrl = textField.getText().trim();

                    if (!rtspUrl.isEmpty()) {
                        System.out.println("URL RTSP: " + rtspUrl);
                        quadrants.get(quadrantIndex).connectToRtspStream(rtspUrl);
                    } else {
                        System.out.println("URL RTSP está vazia");
                    }
                } else {
                    System.out.println("Campo de texto é nulo para quadrante " + quadrantIndex);
                }
            });
        } else {
            System.out.println("Botão é nulo ou quadrante não existe: button=" + button +
                    ", quadrantIndex=" + quadrantIndex +
                    ", quadrants.size()=" + quadrants.size());
        }
    }

    /**
     * Configura os botões de conexão UDP
     */
    private void setupUdpConnections() {
        configureUdpButton(btn_ip1, txtField_ip1, 0);
        configureUdpButton(btn_ip2, txtField_ip2, 1);
        configureUdpButton(btn_ip3, txtField_ip3, 2);
        configureUdpButton(btn_ip4, txtField_ip4, 3);
    }

    /**
     * Configura um botão UDP específico
     */
    private void configureUdpButton(Button button, TextField textField, int quadrantIndex) {
        if (button != null && quadrants.size() > quadrantIndex) {
            System.out.println("Configurando botão UDP para quadrante " + quadrantIndex);

            button.setOnAction(event -> {
                System.out.println("Botão UDP clicado para quadrante " + quadrantIndex);

                if (textField != null) {
                    String ipAddress = textField.getText().trim();

                    if (!ipAddress.isEmpty()) {
                        System.out.println("IP: " + ipAddress);
                        quadrants.get(quadrantIndex).connectToUdpStream(ipAddress);
                    } else {
                        System.out.println("Endereço IP está vazio");
                    }
                } else {
                    System.out.println("Campo de texto é nulo para quadrante " + quadrantIndex);
                }
            });
        } else {
            System.out.println("Botão é nulo ou quadrante não existe: button=" + button +
                    ", quadrantIndex=" + quadrantIndex +
                    ", quadrants.size()=" + quadrants.size());
        }
    }

    /**
     * Configura o botão de tela cheia
     */
    private void setupFullScreenButton() {
        if (btn_tela_cheia == null || mainStage == null) {
            System.err.println("ERRO: Botão de tela cheia ou estágio principal não encontrado");
            System.err.println("btn_tela_cheia: " + (btn_tela_cheia != null ? "encontrado" : "nulo"));
            System.err.println("mainStage: " + (mainStage != null ? "encontrado" : "nulo"));
            return;
        }

        // Listener para estado de tela cheia
        mainStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            // Atualiza com pequeno atraso para garantir que redimensionamento terminou
            Platform.runLater(() -> {
                // Pequena pausa adicional para layout estabilizar
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("Estado de tela cheia alterado: " + newVal);
                System.out.println("Atualizando layout dos streams de vídeo...");

                // Força atualização do layout
                grid_quadrante.layout();

                // Notifica quadrantes da mudança
                for (CameraQuadrant quadrant : quadrants) {
                    if (quadrant != null) {
                        quadrant.notifyLayoutChange();
                    }
                }

                // Atualiza texto do botão
                updateFullScreenButtonText(newVal);
            });
        });

        // Ação do botão
        btn_tela_cheia.setOnAction(event -> {
            System.out.println("Botão tela cheia clicado!");
            boolean fullScreen = !mainStage.isFullScreen();
            mainStage.setFullScreen(fullScreen);
        });

        // Texto inicial do botão
        updateFullScreenButtonText(mainStage.isFullScreen());
    }

    /**
     * Atualiza o texto do botão de tela cheia
     */
    private void updateFullScreenButtonText(boolean isFullScreen) {
        if (btn_tela_cheia != null) {
            btn_tela_cheia.setText(isFullScreen ? "[ ] Sair da Tela Cheia" : "[ ] Tela Cheia");
        }
    }

    /**
     * Libera todos os recursos utilizados pela aplicação
     */
    public void disposeResources() {
        // Libera os recursos de cada quadrante
        for (CameraQuadrant quadrant : quadrants) {
            if (quadrant != null) {
                quadrant.dispose();
            }
        }

        System.out.println("Todos os recursos foram liberados com sucesso!");
    }
}