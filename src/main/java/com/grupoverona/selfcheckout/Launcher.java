package com.grupoverona.selfcheckout;

import com.grupoverona.selfcheckout.app.MainApplication;

/**
 * Classe auxiliar para contornar limitações de empacotamento com módulos JavaFX
 * quando usando maven-shade-plugin.
 *
 * Esta classe serve apenas como um ponto de entrada alternativo que delega
 * para a classe principal MainApplication.
 *
 * Nota: Esta classe só é necessária para compatibilidade com certas
 * configurações de empacotamento JAR. Em um ambiente de desenvolvimento
 * normal, use MainApplication diretamente.
 */
public class Launcher {
    /**
     * Método principal que simplesmente delega para MainApplication
     */
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}