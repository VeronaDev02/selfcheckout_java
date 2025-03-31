package com.grupoverona.selfcheckout.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Processador de mensagens recebidas dos PDVs.
 * Esta classe é responsável por formatar e filtrar mensagens antes da exibição na UI.
 */
public class MessageProcessor {

    // Lista de filtros registrados
    private static final List<MessageFilter> filters = new ArrayList<>();

    /**
     * Interface para filtros de mensagens.
     * Implementações desta interface podem modificar ou formatar mensagens recebidas.
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
     * Inicialização estática da classe - configura filtros padrão
     */
    static {
        registerDefaultFilters();
    }

    /**
     * Registra os filtros padrão para formatação de mensagens
     */
    private static void registerDefaultFilters() {
        // TODO: Aqui você pode configurar os filtros de formatação padrão
        // de acordo com o formato esperado das mensagens dos PDVs

        // Filtro para remover caracteres de controle (exceto nova linha)
        addFilter(message -> message.replaceAll("[\\p{Cntrl}&&[^\r\n]]", ""));

        // Filtro para substituir o caractere ^ por quebra de linha
        // TODO: Personalizar este filtro conforme a sintaxe específica dos PDVs
        addFilter(message -> message.replace("^", "\n"));

        // Filtro para alinhar mensagens com valores monetários
        // TODO: Ajustar o padrão regex para capturar corretamente os valores monetários
        // no formato específico utilizado pelos PDVs
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

        // TODO: Adicionar filtros adicionais para formatação específica:
        // - Destacar valores negativos em vermelho
        // - Formatar códigos de produtos
        // - Alinhar colunas de tabelas
        // - Converter códigos de operação para texto legível
    }

    /**
     * Adiciona um filtro personalizado
     * @param filter O filtro a ser adicionado
     */
    public static void addFilter(MessageFilter filter) {
        filters.add(filter);
    }

    /**
     * Remove todos os filtros existentes
     */
    public static void clearFilters() {
        filters.clear();
    }

    /**
     * Processa uma mensagem aplicando todos os filtros registrados na ordem
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
     * Formata uma mensagem UDP com informações do remetente
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

    /**
     * Cria um filtro que substitui um padrão regex por uma string formatada
     * @param regex O padrão regex para buscar
     * @param format O formato para substituição (usando String.format)
     * @param groupIndices Os índices dos grupos capturados a serem usados no formato
     * @return Um filtro configurado
     */
    public static MessageFilter createRegexFilter(String regex, String format, int... groupIndices) {
        return message -> {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(message);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                // Prepara os argumentos para o formato
                Object[] args = new Object[groupIndices.length];
                for (int i = 0; i < groupIndices.length; i++) {
                    args[i] = matcher.group(groupIndices[i]);
                }

                // Substitui pelo formato
                matcher.appendReplacement(sb, String.format(format, args));
            }

            matcher.appendTail(sb);
            return sb.toString();
        };
    }
}