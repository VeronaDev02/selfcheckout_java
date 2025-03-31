package com.grupoverona.selfcheckout.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Classe principal da aplicação JavaFX.
 * Ponto de entrada único do sistema.
 */
public class MainApplication extends Application {

    /**
     * Método de inicialização da aplicação JavaFX.
     */
    @Override
    public void start(Stage stage) throws IOException {
        try {
            // Tenta carregar o FXML usando o caminho padrão
            String fxmlPath = "/com/grupoverona/selfcheckout/Main.fxml";
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (fxmlLoader.getLocation() == null) {
                // Se falhar, tenta caminhos alternativos
                System.out.println("FXML não encontrado no caminho padrão, tentando alternativas...");
                fxmlLoader = findFxmlAlternative();

                if (fxmlLoader == null) {
                    throw new IOException("Não foi possível encontrar o arquivo FXML. Verifique a instalação.");
                }
            }

            // Cria e associa o controlador
            MainController controller = new MainController();
            fxmlLoader.setController(controller);

            // Carrega o FXML e configura a cena
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            scene.setUserData(controller);

            // Configura o estágio
            stage.setTitle("Monitor de Câmeras e PDVs - Grupo Verona");
            stage.setScene(scene);

            // Passa o estágio para o controlador
            controller.setMainStage(stage);

            // Exibe a janela
            stage.show();

            System.out.println("FXML carregado com sucesso: " + fxmlLoader.getLocation());
        } catch (Exception e) {
            System.err.println("Erro ao iniciar a aplicação: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Tenta encontrar o arquivo FXML em caminhos alternativos.
     * Esta abordagem é necessária para funcionar em diferentes ambientes:
     * - Desenvolvimento (IDE)
     * - JAR empacotado pelo Maven
     */
    private FXMLLoader findFxmlAlternative() {
        // Lista de possíveis caminhos alternativos
        String[] alternatives = {
                "/Main.fxml",
                "/app/Main.fxml",
                "/fxml/Main.fxml",
                "/resources/Main.fxml"
        };

        for (String path : alternatives) {
            if (getClass().getResource(path) != null) {
                System.out.println("FXML encontrado em: " + path);
                return new FXMLLoader(getClass().getResource(path));
            }
        }

        // Como último recurso, tenta usar o ClassLoader
        for (String path : alternatives) {
            String resourcePath = path.startsWith("/") ? path.substring(1) : path;
            if (getClass().getClassLoader().getResource(resourcePath) != null) {
                System.out.println("FXML encontrado via ClassLoader em: " + resourcePath);
                return new FXMLLoader(getClass().getClassLoader().getResource(resourcePath));
            }
        }

        // Se não encontrar, retorna null
        return null;
    }

    /**
     * Método chamado quando a aplicação está sendo encerrada.
     * Libera os recursos utilizados pela aplicação.
     */
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

    /**
     * Configura propriedades do sistema para otimização
     */
    private static void configureSystemProperties() {
        // Melhoria de desempenho do JavaFX
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("prism.dirtyopts", "false");

        // Configurações específicas para o VLC
        System.setProperty("jna.nosys", "true");  // Evita problemas com JNA

        // Define caminho de cache para evitar problemas de permissão
        String userHome = System.getProperty("user.home");
        System.setProperty("java.io.tmpdir", userHome + "/.grupoverona/cache");
    }

    /**
     * Método principal que inicia a aplicação
     */
    public static void main(String[] args) {
        // Configurações do sistema para melhorar desempenho
        configureSystemProperties();

        // Inicia a aplicação JavaFX
        launch(args);
    }
}