package com.grupoverona.selfcheckout;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Classe responsável por processar mensagens recebidas dos PDVs
 * antes de serem exibidas na interface do usuário.
 */
public class MessageProcessor {

    // Lista de processadores registrados
    private static final List<MessageFilter> filters = new ArrayList<>();

    // Inicialização estática da classe
    static {
        // Registra os filtros padrão
        registerDefaultFilters();
    }

    /**
     * Interface para filtros de mensagens
     */
    public interface MessageFilter {
        /**
         * Processa uma mensagem
         * @param message A mensagem original
         * @return A mensagem processada
         */
        String process(String message);
    }

    /**
     * Registra os filtros padrão
     */
    private static void registerDefaultFilters() {
        // Filtro para remover caracteres de controle (exceto nova linha)
        addFilter(message -> message.replaceAll("[\\p{Cntrl}&&[^\r\n]]", ""));

        // Filtro para substituir o caractere ^ por quebra de linha
        addFilter(message -> message.replace("^", "\n"));

        // Filtro para alinhar mensagens com valores monetários
        addFilter(message -> {
            Pattern pattern = Pattern.compile("(\\d+,\\d{2}) ?= ?(\\d+,\\d{2})");
            Matcher matcher = pattern.matcher(message);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                // Formata para alinhar à direita
                String replacement = String.format("%-10s = %10s", matcher.group(1), matcher.group(2));
                matcher.appendReplacement(sb, replacement);
            }

            matcher.appendTail(sb);
            return sb.toString();
        });
    }

    /**
     * Adiciona um filtro personalizado
     * @param filter O filtro a ser adicionado
     */
    public static void addFilter(MessageFilter filter) {
        filters.add(filter);
    }

    /**
     * Remove todos os filtros
     */
    public static void clearFilters() {
        filters.clear();
    }

    /**
     * Processa uma mensagem aplicando todos os filtros registrados
     * @param originalMessage A mensagem original recebida do PDV
     * @return A mensagem processada pronta para exibição
     */
    public static String processMessage(String originalMessage) {
        String processedMessage = originalMessage;

        // Aplicar cada filtro em sequência
        for (MessageFilter filter : filters) {
            processedMessage = filter.process(processedMessage);
        }

        return processedMessage;
    }

    /**
     * Processa uma mensagem recebida via UDP
     * @param senderInfo Informação do remetente (IP:porta)
     * @param rawMessage Mensagem bruta recebida
     * @return Mensagem formatada pronta para exibição
     */
    public static String processUdpMessage(String senderInfo, String rawMessage) {
        // Primeiro processa a mensagem bruta
        String processedContent = processMessage(rawMessage);

        // Formata a mensagem completa com informações do remetente
        if (processedContent.contains("\n")) {
            // Se a mensagem já contém quebras de linha, adiciona a informação do remetente na primeira linha
            return "Recebido de " + senderInfo + ":\n" + processedContent;
        } else {
            // Caso contrário, mantém tudo em uma única linha
            return "Recebido de " + senderInfo + ": " + processedContent;
        }
    }
}