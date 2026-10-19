package com.erp;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Currency;

public class Formato {

    private static final DateTimeFormatter ARMAZENAMENTO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter ANTIGO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static String agoraParaArquivo() {
        return LocalDateTime.now().format(ARMAZENAMENTO);
    }
    public static String dataParaTela(String dataGravada) {
        LocalDateTime data = interpretar(dataGravada);
        if (data == null) return dataGravada; // não entendeu: mostra como está
        return data.format(DateTimeFormatter.ofPattern(
                Idioma.config("formato.data", "dd/MM/yyyy HH:mm"), Idioma.locale()));
    }

    private static LocalDateTime interpretar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        for (DateTimeFormatter formato : new DateTimeFormatter[] { ARMAZENAMENTO, ANTIGO }) {
            try { return LocalDateTime.parse(texto.trim(), formato); }
            catch (DateTimeParseException ignorado) { }
        }
        return null;
    }
    public static String moeda(BigDecimal valor) {
        BigDecimal convertido = valor.multiply(taxa()).setScale(2, RoundingMode.HALF_UP);
        NumberFormat formatador = NumberFormat.getCurrencyInstance(Idioma.locale());
        formatador.setCurrency(Currency.getInstance(Idioma.config("formato.moeda.codigo", "BRL")));
        return formatador.format(convertido);
    }

    public static BigDecimal paraArmazenar(BigDecimal valorDigitado) {
        BigDecimal taxa = taxa();
        if (taxa.compareTo(BigDecimal.ONE) == 0) return valorDigitado;
        return valorDigitado.divide(taxa, 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal taxa() {
        return new BigDecimal(Idioma.config("formato.moeda.taxa", "1.00"));
    }
    public static String numero(double valor, int casas) {
        return String.format(Idioma.locale(), "%,." + casas + "f", valor);
    }
}