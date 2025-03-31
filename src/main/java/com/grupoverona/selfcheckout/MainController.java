package com.grupoverona.selfcheckout;

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
 * Controlador da tela principal da aplicação
 */
public class MainController {

    // Grid principal de quadrantes - este é o único que tem fx:id no FXML
    @FXML
    private GridPane grid_quadrante;

    // Para os outros controles que usam id em vez de fx:id no FXML
    private Button btn_tela_cheia;
    private TextField txtField_rtsp1, txtField_rtsp2, txtField_rtsp3, txtField_rtsp4;
    private TextField txtField_ip1, txtField_ip2, txtField_ip3, txtField_ip4;
    private Button btn_rtsp1, btn_rtsp2, btn_rtsp3, btn_rtsp4;
    private Button btn_ip1, btn_ip2, btn_ip3, btn_ip4;

    // Lista dos quadrantes de câmeras
    private List<CameraQuadrant> quadrants = new ArrayList<>(4);

    // Referência ao estágio (janela) principal
    private Stage mainStage;

    /**
     * Define o estágio principal
     */
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    /**
     * Inicializa o controlador
     */
    @FXML
    public void initialize() {
        System.out.println("MainController inicializado");
        System.out.println("grid_quadrante: " + (grid_quadrante != null ? "encontrado" : "nulo"));

        // Como precisamos ter certeza que os elementos FXML foram carregados,
        // vamos esperar até que a UI esteja pronta
        Platform.runLater(this::setupAfterUIReady);
    }

    /**
     * Configura todos os componentes após a UI estar pronta
     */
    private void setupAfterUIReady() {
        if (grid_quadrante == null) {
            System.err.println("ERRO: grid_quadrante não encontrado no FXML.");
            return;
        }

        // Busca todos os controles pelo ID
        try {
            Scene scene = grid_quadrante.getScene();
            if (scene != null) {
                // Busca botões e campos de texto
                lookupControls(scene);

                // Debug: imprimir quais elementos foram encontrados
                System.out.println("btn_tela_cheia: " + (btn_tela_cheia != null ? "encontrado" : "nulo"));
                System.out.println("btn_rtsp1: " + (btn_rtsp1 != null ? "encontrado" : "nulo"));
                System.out.println("txtField_rtsp1: " + (txtField_rtsp1 != null ? "encontrado" : "nulo"));

                // Inicializa os quadrantes
                setupQuadrants();

                // Configura os botões de conexão
                setupConnections();

                // Configura o botão de tela cheia
                if (mainStage != null && btn_tela_cheia != null) {
                    configureFullScreenButton(mainStage);
                    System.out.println("Botão de tela cheia configurado com sucesso!");
                } else {
                    System.err.println("AVISO: Não foi possível configurar botão de tela cheia.");
                    System.err.println("mainStage: " + (mainStage != null ? "encontrado" : "nulo"));
                    System.err.println("btn_tela_cheia: " + (btn_tela_cheia != null ? "encontrado" : "nulo"));
                }
            } else {
                System.err.println("ERRO: Scene é nula, não é possível buscar controles");
            }
        } catch (Exception e) {
            System.err.println("ERRO ao configurar controles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Busca todos os controles da interface pelo ID
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
     * Inicializa os quadrantes com os componentes corretos
     */
    private void setupQuadrants() {
        try {
            // Cria quadrantes individuais
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    // Obtém o GridPane do quadrante específico
                    GridPane quadrantGrid = findQuadrantGridPane(row, col);

                    if (quadrantGrid != null) {
                        // Obtém os panes para vídeo e log dentro deste quadrante
                        AnchorPane logPane = (AnchorPane) quadrantGrid.getChildren().get(0);
                        AnchorPane videoPane = (AnchorPane) quadrantGrid.getChildren().get(1);

                        // Cria o quadrante
                        int index = row * 2 + col;
                        quadrants.add(new CameraQuadrant(index, videoPane, logPane));
                        System.out.println("Quadrante " + index + " criado com sucesso!");
                    } else {
                        System.err.println("ERRO: Não foi possível encontrar o GridPane para o quadrante [" + row + "," + col + "]");
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

                // Verifica se este é o GridPane que estamos procurando
                if ((rowIndex == null ? 0 : rowIndex) == row &&
                        (colIndex == null ? 0 : colIndex) == col) {
                    return (GridPane) node;
                }
            }
        }

        return null;
    }

    /**
     * Configura todos os botões de conexão
     */
    private void setupConnections() {
        setupRtspConnections();
        setupUdpConnections();
    }

    /**
     * Configura os botões de conexão RTSP
     */
    private void setupRtspConnections() {
        // Verificamos cada botão antes de configurá-lo
        if (btn_rtsp1 != null && quadrants.size() > 0) {
            System.out.println("Configurando btn_rtsp1");
            btn_rtsp1.setOnAction(event -> {
                System.out.println("btn_rtsp1 clicado");
                if (txtField_rtsp1 != null) {
                    String rtspUrl = txtField_rtsp1.getText();
                    System.out.println("URL RTSP 1: " + rtspUrl);
                    if (!rtspUrl.isEmpty()) {
                        quadrants.get(0).connectToRtspStream(rtspUrl);
                    }
                } else {
                    System.out.println("txtField_rtsp1 é nulo");
                }
            });
        } else {
            System.out.println("btn_rtsp1 é nulo ou quadrants está vazio: btn_rtsp1=" + btn_rtsp1 + ", quadrants.size()=" + quadrants.size());
        }

        if (btn_rtsp2 != null && quadrants.size() > 1) {
            System.out.println("Configurando btn_rtsp2");
            btn_rtsp2.setOnAction(event -> {
                System.out.println("btn_rtsp2 clicado");
                if (txtField_rtsp2 != null) {
                    String rtspUrl = txtField_rtsp2.getText();
                    System.out.println("URL RTSP 2: " + rtspUrl);
                    if (!rtspUrl.isEmpty()) {
                        quadrants.get(1).connectToRtspStream(rtspUrl);
                    }
                } else {
                    System.out.println("txtField_rtsp2 é nulo");
                }
            });
        } else {
            System.out.println("btn_rtsp2 é nulo ou quadrants está vazio: btn_rtsp2=" + btn_rtsp2 + ", quadrants.size()=" + quadrants.size());
        }

        // Repete para os outros botões...
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
                    String rtspUrl = textField.getText();
                    System.out.println("URL RTSP: " + rtspUrl);
                    if (!rtspUrl.isEmpty()) {
                        quadrants.get(quadrantIndex).connectToRtspStream(rtspUrl);
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
                    String ipAddress = textField.getText();
                    System.out.println("IP: " + ipAddress);
                    if (!ipAddress.isEmpty()) {
                        quadrants.get(quadrantIndex).connectToUdpStream(ipAddress);
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
    public void configureFullScreenButton(Stage stage) {
        if (btn_tela_cheia == null) {
            System.err.println("ERRO: Botão de tela cheia não encontrado no FXML");
            return;
        }

        // Adiciona um listener ao estado de tela cheia para forçar atualização dos streams
        stage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            // Aguarda um momento para garantir que o redimensionamento terminou
            Platform.runLater(() -> {
                // Aguarda mais 100ms para garantir que o layout foi atualizado
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Log para debug
                System.out.println("Estado de tela cheia alterado: " + newVal);
                System.out.println("Forçando atualização dos streams de vídeo...");

                // Força atualização do layout
                grid_quadrante.layout();

                // Se necessário, notifica os quadrantes sobre a mudança
                for (CameraQuadrant quadrant : quadrants) {
                    if (quadrant != null) {
                        // Método auxiliar para notificar mudança de tela
                        notifyQuadrantLayoutChange(quadrant);
                    }
                }
            });
        });

        btn_tela_cheia.setOnAction(event -> {
            System.out.println("Botão tela cheia clicado!");
            boolean fullScreen = !stage.isFullScreen();
            stage.setFullScreen(fullScreen);

            // Atualiza o texto do botão
            if (fullScreen) {
                btn_tela_cheia.setText("[ ] Sair da Tela Cheia");
            } else {
                btn_tela_cheia.setText("[ ] Tela Cheia");
            }
        });
    }

    /**
     * Notifica um quadrante sobre mudanças de layout
     */
    private void notifyQuadrantLayoutChange(CameraQuadrant quadrant) {
        // Chama o método específico do quadrante para notificar mudança de layout
        quadrant.notifyLayoutChange();
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