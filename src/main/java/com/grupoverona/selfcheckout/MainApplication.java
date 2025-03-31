package com.grupoverona.selfcheckout;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Classe principal da aplicação JavaFX
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // Tenta carregar o arquivo FXML de diferentes formas para garantir que seja encontrado
            FXMLLoader fxmlLoader = null;

            // Tentar o caminho padrão
            String fxmlPath = "/com/grupoverona/selfcheckout/Main.fxml";
            if (getClass().getResource(fxmlPath) != null) {
                fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            }
            // Tentar o caminho relativo
            else if (getClass().getResource("/Main.fxml") != null) {
                fxmlLoader = new FXMLLoader(getClass().getResource("/Main.fxml"));
            }
            // Tentar buscar pela classe Launcher
            else if (Launcher.class.getResource("/com/grupoverona/selfcheckout/Main.fxml") != null) {
                fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/grupoverona/selfcheckout/Main.fxml"));
            }
            // Tentar com ClassLoader
            else if (getClass().getClassLoader().getResource("com/grupoverona/selfcheckout/Main.fxml") != null) {
                fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("com/grupoverona/selfcheckout/Main.fxml"));
            }
            // Última tentativa
            else if (getClass().getClassLoader().getResource("Main.fxml") != null) {
                fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("Main.fxml"));
            }
            else {
                throw new IOException("Não foi possível encontrar o arquivo FXML. Verifique se ele está no diretório correto.");
            }

            // Cria um controlador e o associa ao FXML
            MainController controller = new MainController();
            fxmlLoader.setController(controller);

            // Carrega o FXML
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);

            // Armazena o controlador para acessá-lo no método stop()
            scene.setUserData(controller);

            // Configura o estágio
            stage.setTitle("Monitor de Câmeras e PDVs - Grupo Verona");
            stage.setScene(scene);

            // Passa o estágio para o controlador
            controller.setMainStage(stage);

            // Mostra a janela
            stage.show();

            // Exibe o caminho do FXML carregado
            System.out.println("FXML carregado com sucesso: " + fxmlLoader.getLocation());

        } catch (Exception e) {
            System.err.println("Erro ao iniciar a aplicação: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop() {
        // Libera os recursos ao fechar a aplicação
        try {
            // Obtém o controlador para liberar os recursos
            Stage stage = (Stage) Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (stage != null) {
                MainController controller = (MainController) stage.getScene().getUserData();
                if (controller != null) {
                    controller.disposeResources();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao liberar recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}