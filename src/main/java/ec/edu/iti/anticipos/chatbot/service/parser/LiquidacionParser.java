package ec.edu.iti.anticipos.chatbot.service.parser;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser específico para el formulario de LIQUIDACIÓN de fondos del ITI.
 *
 * ⚠️ LIMITACIÓN CONOCIDA:
 * Azure DI extrae los encabezados juntos y los valores después.
 * Este parser asume ese formato y extrae los valores por orden.
 */
@Component
public class LiquidacionParser {

    public FormularioLiquidacionDTO parsear(String texto) {
        FormularioLiquidacionDTO dto = new FormularioLiquidacionDTO();

        System.out.println("=== PARSEANDO FORMULARIO DE LIQUIDACIÓN ===");

        if (texto == null || texto.isEmpty()) {
            dto.setExtraccionExitosa(false);
            return dto;
        }

        System.out.println("--- TEXTO COMPLETO ---");
        System.out.println(texto);
        System.out.println("--- FIN DEL TEXTO ---");

        try {
            String normalizado = normalizarTexto(texto);

            // Extraer valores después del título
            dto.setSolicitante(extraerValorDespuesDelTitulo(normalizado, 0));
            dto.setUnidad(extraerValorDespuesDelTitulo(normalizado, 1));
            dto.setCampus(extraerValorDespuesDelTitulo(normalizado, 2));
            dto.setFecha(parsearFecha(extraerValorDespuesDelTitulo(normalizado, 3)));

            dto.setProposito(extraerValor(normalizado,
                    "PROPOSITO\\s+DEL\\s+ANTICIPO\\s*:?\\s*\\n?\\s*([^\\n]+)"));

            dto.setLugar(extraerValor(normalizado,
                    "LUGAR\\s+Y\\s+FECHA\\s+A\\s+REALIZARSE\\s*:?\\s*\\n?\\s*([^\\n]+)"));

            dto.setAnticipo(extraerMonto(normalizado, "\\$\\s*([\\d,\\.]+)"));

            dto.setTotalGastos(extraerMonto(normalizado,
                    "TOTAL\\s+GASTOS\\s*:?\\s*\\$?\\s*([\\d,\\.]+)"));

            dto.setDiferencia(extraerMonto(normalizado,
                    "DIFERENCIA\\s*:?\\s*\\$?\\s*([\\d,\\.]+)"));

            dto.setCi(extraerValor(normalizado, "CI\\s*[.:]\\s*(\\d+)"));

            dto.setFirma("firma_registrada");
            dto.setExtraccionExitosa(true);

            System.out.println("=== EXTRACCIÓN EXITOSA ===");
            System.out.println("Solicitante: " + dto.getSolicitante());
            System.out.println("Unidad: " + dto.getUnidad());
            System.out.println("Campus: " + dto.getCampus());
            System.out.println("Fecha: " + dto.getFecha());
            System.out.println("Propósito: " + dto.getProposito());
            System.out.println("Lugar: " + dto.getLugar());
            System.out.println("Anticipo: " + dto.getAnticipo());
            System.out.println("Total gastos: " + dto.getTotalGastos());
            System.out.println("Diferencia: " + dto.getDiferencia());
            System.out.println("CI: " + dto.getCi());

        } catch (Exception e) {
            System.err.println("Error al parsear liquidación: " + e.getMessage());
            e.printStackTrace();
            dto.setExtraccionExitosa(false);
        }

        return dto;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return null;
        return texto
                .replaceAll("[ÁÀÂÄ]", "A")
                .replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I")
                .replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U")
                .replaceAll("[áàâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòôö]", "o")
                .replaceAll("[úùûü]", "u");
    }

    private String extraerValorDespuesDelTitulo(String texto, int indice) {
        int idxTitulo = texto.toUpperCase().indexOf("FORMATO DE LIQUIDACION DE FONDOS");
        if (idxTitulo < 0) return null;

        String textoDespues = texto.substring(idxTitulo + "FORMATO DE LIQUIDACION DE FONDOS".length());
        String[] lineas = textoDespues.split("\\n");

        int contador = 0;
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (limpia.isEmpty()) continue;
            if (limpia.length() < 3) continue;

            String upper = limpia.toUpperCase();
            if (upper.equals("VALOR") || upper.equals("VALOR:")) continue;
            if (upper.equals("NOMBRE DEL SOLICITANTE") || upper.startsWith("NOMBRE DEL SOLICITANTE:")) continue;
            if (upper.startsWith("UNIDAD/COORDINACI")) continue;
            if (upper.equals("CAMPUS") || upper.startsWith("CAMPUS:")) continue;
            if (upper.equals("FECHA") || upper.equals("FECHA :") || upper.startsWith("FECHA:")) continue;
            if (upper.startsWith("PROP")) continue;
            if (upper.startsWith("LUGAR")) continue;
            if (upper.startsWith("NOTA:")) continue;
            if (upper.startsWith("FECHA ") || upper.startsWith("Nª")) continue;
            if (upper.equals("PROVEEDOR") || upper.equals("CONCEPTO") || upper.equals("REV.")) continue;
            if (upper.startsWith("TOTAL") || upper.startsWith("DIFERENCIA")) continue;
            if (upper.startsWith("FIRMA") || upper.startsWith("CI") || upper.startsWith("NOMBRE:")) continue;
            if (upper.startsWith("ITI") || upper.startsWith("İTİ") || upper.startsWith("TECNOL")) continue;
            if (upper.startsWith("FORMATO DE")) continue;

            if (limpia.matches("^[\\$\\d,\\.\\s]+$")) continue;

            if (limpia.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                if (contador == indice) return limpia;
                contador++;
                continue;
            }

            if (limpia.matches("^\\d+$")) continue;
            if (limpia.length() > 100) continue;

            if (contador == indice) return limpia;
            contador++;
        }
        return null;
    }

    private String extraerValor(String texto, String regex) {
        try {
            Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher m = p.matcher(texto);
            if (m.find()) {
                String valor = m.group(1).trim();
                return valor.isEmpty() ? null : valor;
            }
        } catch (Exception e) {
            System.err.println("Error regex [" + regex + "]: " + e.getMessage());
        }
        return null;
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null) return null;
        String limpia = valor.trim().replaceAll("[^\\d/\\-]", "");
        if (limpia.isEmpty()) return null;
        String[] formatos = {"dd/MM/yyyy", "yyyy/MM/dd", "dd-MM-yyyy", "yyyy-MM-dd"};
        for (String fmt : formatos) {
            try {
                return LocalDate.parse(limpia, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal extraerMonto(String texto, String regex) {
        String valor = extraerValor(texto, regex);
        if (valor == null) return null;
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }
}