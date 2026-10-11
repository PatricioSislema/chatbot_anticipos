package ec.edu.iti.anticipos.chatbot.service.parser;

import ec.edu.iti.anticipos.chatbot.dto.ComprobanteExtraidoDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ComprobanteParser {

    private static final String RUC_ITI = "1792146836001";
    private static final String[] ALIAS_ITI = {
            "INSTITUTO TECNOLOGICO",
            "INSTITUTO TECNOLÓGICO",
            "ITI"
    };

    private static final Pattern PATRON_MONTO =
            Pattern.compile("\\$\\s?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)");
    private static final Pattern PATRON_FECHA_DDMMYYYY =
            Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})");
    private static final Pattern PATRON_FECHA_YYYYMMDD =
            Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})");
    private static final Pattern PATRON_CUENTA =
            Pattern.compile("(?:cuenta|cta|ahorro|ahorros|corriente)[^0-9]{0,20}(\\d{6,20})",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_BANCO =
            Pattern.compile("(BANCO\\s+[A-ZÁÉÍÓÚÑ]+(?:\\s+[A-ZÁÉÍÓÚÑ]+){0,2})");

    public ComprobanteExtraidoDTO parsear(String texto, BigDecimal saldoPendiente) {
        ComprobanteExtraidoDTO dto = new ComprobanteExtraidoDTO();
        dto.setTextoExtraido(texto);
        dto.setObservacion("");

        if (texto == null || texto.isBlank()) {
            dto.setObservacion("No se pudo extraer texto del PDF.");
            return dto;
        }

        String textoUpper = texto.toUpperCase();

        dto.setEsDelIti(contieneIti(textoUpper));

        BigDecimal monto = extraerPrimerMonto(texto);
        dto.setMontoDepositado(monto);
        dto.setMontoEncontrado(monto != null);

        if (monto != null && saldoPendiente != null) {
            BigDecimal diff = monto.subtract(saldoPendiente).abs();
            dto.setMontoCoincide(diff.compareTo(new BigDecimal("0.50")) <= 0);
        }

        LocalDate fecha = extraerFecha(texto);
        dto.setFechaDeposito(fecha);
        if (fecha != null) {
            dto.setFechaReciente(!fecha.isBefore(LocalDate.now().minusDays(5)));
        }

        dto.setBancoOrigen(extraerBanco(textoUpper));
        dto.setCuentaOrigen(extraerCuenta(texto));

        StringBuilder obs = new StringBuilder();
        if (!dto.isEsDelIti()) {
            obs.append("No se detectó el RUC/nombre del ITI en el comprobante. ");
        }
        if (!dto.isMontoEncontrado()) {
            obs.append("No se detectó monto; requiere validación manual de la Contadora. ");
        } else if (saldoPendiente != null && !dto.isMontoCoincide()) {
            obs.append(String.format(
                    "⚠️ El monto del comprobante ($%.2f) no coincide con el saldo pendiente ($%.2f). ",
                    monto, saldoPendiente));
        }
        if (fecha == null) {
            obs.append("No se detectó fecha; se recomienda revisar. ");
        } else if (!dto.isFechaReciente()) {
            obs.append("La fecha del comprobante tiene más de 5 días. ");
        }
        dto.setObservacion(obs.toString().trim());

        return dto;
    }

    private boolean contieneIti(String textoUpper) {
        if (textoUpper.contains(RUC_ITI)) return true;
        for (String alias : ALIAS_ITI) {
            if (textoUpper.contains(alias)) return true;
        }
        return false;
    }

    private BigDecimal extraerPrimerMonto(String texto) {
        Matcher m = PATRON_MONTO.matcher(texto);
        if (m.find()) {
            return normalizarMonto(m.group(1));
        }
        return null;
    }

    private BigDecimal normalizarMonto(String raw) {
        String limpio = raw.trim();
        int lastComa = limpio.lastIndexOf(',');
        int lastPunto = limpio.lastIndexOf('.');

        if (lastComa > lastPunto) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else {
            limpio = limpio.replace(",", "");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate extraerFecha(String texto) {
        Matcher m1 = PATRON_FECHA_DDMMYYYY.matcher(texto);
        if (m1.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(m1.group(3)),
                        Integer.parseInt(m1.group(2)),
                        Integer.parseInt(m1.group(1)));
            } catch (Exception ignored) { }
        }
        Matcher m2 = PATRON_FECHA_YYYYMMDD.matcher(texto);
        if (m2.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(m2.group(1)),
                        Integer.parseInt(m2.group(2)),
                        Integer.parseInt(m2.group(3)));
            } catch (Exception ignored) { }
        }
        return null;
    }

    private String extraerBanco(String textoUpper) {
        Matcher m = PATRON_BANCO.matcher(textoUpper);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extraerCuenta(String texto) {
        Matcher m = PATRON_CUENTA.matcher(texto);
        return m.find() ? m.group(1) : null;
    }
}