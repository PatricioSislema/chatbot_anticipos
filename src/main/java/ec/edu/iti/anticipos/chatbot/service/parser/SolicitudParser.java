package ec.edu.iti.anticipos.chatbot.service.parser;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioSolicitudDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser específico para el formulario de SOLICITUD de anticipo de fondos del ITI.
 *
 * Estrategia:
 *  1. Normalizar el texto (quitar tildes, unificar encabezados partidos).
 *  2. Usar regex específicos para cada campo.
 *  3. Tolerar variaciones de tildes, mayúsculas y espacios.
 *
 * ⚠️ LIMITACIÓN CONOCIDA:
 * Si Azure DI cambia cómo extrae el texto o el formato del PDF cambia,
 * hay que ajustar los regex.
 */
@Component
public class SolicitudParser {

    public FormularioSolicitudDTO parsear(String texto) {
        FormularioSolicitudDTO dto = new FormularioSolicitudDTO();

        System.out.println("=== PARSEANDO FORMULARIO DE SOLICITUD ===");

        if (texto == null || texto.isEmpty()) {
            dto.setExtraccionExitosa(false);
            return dto;
        }

        System.out.println("--- TEXTO COMPLETO ---");
        System.out.println(texto);
        System.out.println("--- FIN DEL TEXTO ---");

        try {
            // 1. Normalizar texto (quitar tildes, unificar encabezados)
            String normalizado = normalizarTexto(texto);
            System.out.println("--- TEXTO NORMALIZADO ---");
            System.out.println(normalizado);
            System.out.println("--- FIN TEXTO NORMALIZADO ---");

            // 2. Extraer campos con regex (ya sin tildes)
            dto.setSolicitante(extraerValor(normalizado,
                    "NOMBRE\\s+DEL\\s+SOLICITANTE\\s*:?\\s*([^\\n:]+)"));

            dto.setUnidad(extraerValor(normalizado,
                    "UNIDAD/COORDINACION\\s*:?\\s*([^\\n:]+)"));

            // CAMPUS: puede tener "FECHA" en la misma línea, hay que cortar
            String campus = extraerValor(normalizado,
                    "CAMPUS\\s*:?\\s*([^\\n:]+?)(?:\\s+FECHA|$)");
            if (campus != null) campus = campus.trim();
            dto.setCampus(campus);

            dto.setFecha(extraerFecha(normalizado,
                    "FECHA\\s*:?\\s*(\\d{4}/\\d{2}/\\d{2})"));

            dto.setProposito(extraerValor(normalizado,
                    "PROPOSITO\\s+DEL\\s+ANTICIPO\\s*:?\\s*\\n?\\s*([^\\n]+)"));

            dto.setLugar(extraerLugar(normalizado));

            dto.setPresupuesto(extraerMonto(normalizado,
                    "PRESUPUESTO\\s+PARA\\s+LA\\s+ACTIVIDAD\\s*:?[^$]*\\$\\s*([\\d,\\.]+)"));

            dto.setTotal(extraerMonto(normalizado,
                    "TOTAL\\s*\\$?\\s*([\\d,\\.]+)"));

            dto.setCi(extraerValor(normalizado, "CI\\s*[:.]\\s*(\\d+)"));

            if (dto.getLugar() != null) {
                dto.setFechaRealizacion(extraerFechaDeLugar(dto.getLugar()));
            }

            dto.setRubros(new HashMap<>());
            dto.setExtraccionExitosa(true);

            System.out.println("=== EXTRACCIÓN EXITOSA ===");
            System.out.println("Solicitante: " + dto.getSolicitante());
            System.out.println("Unidad: " + dto.getUnidad());
            System.out.println("Campus: " + dto.getCampus());
            System.out.println("Fecha: " + dto.getFecha());
            System.out.println("Propósito: " + dto.getProposito());
            System.out.println("Lugar: " + dto.getLugar());
            System.out.println("Presupuesto: " + dto.getPresupuesto());
            System.out.println("Total: " + dto.getTotal());
            System.out.println("CI: " + dto.getCi());

        } catch (Exception e) {
            System.err.println("Error al parsear solicitud: " + e.getMessage());
            e.printStackTrace();
            dto.setExtraccionExitosa(false);
        }

        return dto;
    }

    /**
     * Normaliza el texto:
     *  - Quita tildes de todas las vocales.
     *  - Une encabezados partidos en 2 líneas.
     */
    private String normalizarTexto(String texto) {
        if (texto == null) return null;
        String normalizado = texto;

        // Quitar tildes
        normalizado = normalizado
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

        // Unir "PROPOSITO DEL\nANTICIPO:" → "PROPOSITO DEL ANTICIPO:"
        normalizado = normalizado.replaceAll(
                "(?i)(PROPOSITO\\s+DEL)\\s*\\n\\s*(ANTICIPO\\s*:)",
                "$1 $2"
        );

        return normalizado;
    }

    /**
     * Extrae el lugar manejando 2 casos:
     *  - "LUGAR Y FECHA A REALIZARSE: X"
     *  - "LUGAR Y FECHA A\nX\nREALIZARSE:"
     */
    private String extraerLugar(String texto) {
        if (texto == null) return null;

        // Caso 1: encabezado completo en una línea
        Pattern p1 = Pattern.compile(
                "LUGAR\\s+Y\\s+FECHA\\s+A\\s+REALIZARSE\\s*:?\\s*\\n?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m1 = p1.matcher(texto);
        if (m1.find()) {
            String valor = m1.group(1).trim();
            if (!valor.isEmpty() && !valor.toUpperCase().contains("REALIZARSE")) {
                System.out.println(">>> Lugar (caso 1): [" + valor + "]");
                return valor;
            }
        }

        // Caso 2: encabezado partido con valor intercalado
        Pattern p2 = Pattern.compile(
                "LUGAR\\s+Y\\s+FECHA\\s+A\\s*\\n\\s*([^\\n]+)\\s*\\n\\s*REALIZARSE\\s*:",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m2 = p2.matcher(texto);
        if (m2.find()) {
            String valor = m2.group(1).trim();
            if (!valor.isEmpty()) {
                System.out.println(">>> Lugar (caso 2): [" + valor + "]");
                return valor;
            }
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

    private LocalDate extraerFecha(String texto, String regex) {
        String valor = extraerValor(texto, regex);
        return parsearFecha(valor);
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null) return null;
        String limpia = valor.trim().replaceAll("[^\\d/\\-]", "");
        if (limpia.isEmpty()) return null;
        String[] formatos = {"yyyy/MM/dd", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"};
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

    private LocalDate extraerFechaDeLugar(String lugar) {
        if (lugar == null) return null;
        Pattern p = Pattern.compile("(\\d{4}/\\d{2}/\\d{2})");
        Matcher m = p.matcher(lugar);
        if (m.find()) return parsearFecha(m.group(1));
        return null;
    }
}