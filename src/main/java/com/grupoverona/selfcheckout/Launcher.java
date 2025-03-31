package com.grupoverona.selfcheckout;

/**
 * Classe auxiliar para contornar limitações de empacotamento com módulos JavaFX
 * quando usando maven-shade-plugin.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}