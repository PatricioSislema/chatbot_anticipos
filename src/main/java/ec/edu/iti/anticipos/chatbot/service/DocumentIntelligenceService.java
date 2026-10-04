package ec.edu.iti.anticipos.chatbot.service;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import ec.edu.iti.anticipos.chatbot.dto.response.FacturaExtraidaDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioSolicitudDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de extracción de datos desde PDFs usando Azure Document Intelligence.
 * Extrae campos de formularios de solicitud, liquidación y facturas.
 * Usa extracción por posición para el caso en que Azure agrupa encabezados
 * y valores por separado.
 */
@Service
public class DocumentIntelligenceService {

    @Value("${azure.document.endpoint}")
    private String endpoint;

    @Value("${azure.document.key}")
    private String key;

    // ==================== FACTURAS ====================
    public FacturaExtraidaDTO extraerDatos(byte[] pdfBytes) {
        FacturaExtraidaDTO dto = new FacturaExtraidaDTO();

        try {
            DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(key))
                    .buildClient();

            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(pdfBytes));

            AnalyzeResult result = client.beginAnalyzeDocument("prebuilt-invoice", options)
                    .getFinalResult();

            System.out.println("=== RESULTADO DE AZURE ===");
            System.out.println("Documentos: " + (result.getDocuments() != null ? result.getDocuments().size() : "null"));

            if (result.getDocuments() != null && !result.getDocuments().isEmpty()) {
                AnalyzedDocument documento = result.getDocuments().get(0);
                Map<String, DocumentField> campos = documento.getFields();
                System.out.println("Campos encontrados: " + campos.keySet());

                DocumentField campoInvoiceId = campos.get("InvoiceId");
                if (campoInvoiceId != null && campoInvoiceId.getValueString() != null) {
                    dto.setNumeroFactura(campoInvoiceId.getValueString());
                }

                DocumentField campoVendorName = campos.get("VendorName");
                if (campoVendorName != null && campoVendorName.getValueString() != null) {
                    dto.setProveedor(campoVendorName.getValueString());
                }

                DocumentField campoVendorTaxId = campos.get("VendorTaxId");
                if (campoVendorTaxId != null && campoVendorTaxId.getValueString() != null) {
                    dto.setRuc(campoVendorTaxId.getValueString());
                }

                DocumentField campoInvoiceDate = campos.get("InvoiceDate");
                if (campoInvoiceDate != null && campoInvoiceDate.getValueDate() != null) {
                    dto.setFecha(campoInvoiceDate.getValueDate());
                }

                DocumentField campoInvoiceTotal = campos.get("InvoiceTotal");
                if (campoInvoiceTotal != null) {
                    System.out.println("=== DIAGNÓSTICO INVOICETOTAL ===");
                    System.out.println("Tipo: " + campoInvoiceTotal.getType());
                    System.out.println("ValueNumber: " + campoInvoiceTotal.getValueNumber());
                    System.out.println("ValueCurrency: " + campoInvoiceTotal.getValueCurrency());
                    System.out.println("ValueString: " + campoInvoiceTotal.getValueString());

                    if (campoInvoiceTotal.getValueNumber() != null) {
                        dto.setMonto(BigDecimal.valueOf(campoInvoiceTotal.getValueNumber()));
                        System.out.println("Monto extraído como número: " + campoInvoiceTotal.getValueNumber());
                    } else if (campoInvoiceTotal.getValueCurrency() != null) {
                        dto.setMonto(BigDecimal.valueOf(campoInvoiceTotal.getValueCurrency().getAmount()));
                        System.out.println("Monto extraído como moneda: " + campoInvoiceTotal.getValueCurrency().getAmount());
                    }
                }

                dto.setExtraccionExitosa(true);
            } else {
                dto.setExtraccionExitosa(false);
            }

        } catch (Exception e) {
            dto.setExtraccionExitosa(false);
            System.err.println("Error al extraer factura: " + e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }

    // ==================== FORMULARIO DE SOLICITUD ====================
    public FormularioSolicitudDTO extraerFormularioSolicitud(byte[] pdfBytes) {
        FormularioSolicitudDTO dto = new FormularioSolicitudDTO();

        try {
            DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(key))
                    .buildClient();

            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(pdfBytes));
            AnalyzeResult result = client.beginAnalyzeDocument("prebuilt-read", options).getFinalResult();
            String texto = result.getContent();

            System.out.println("=== TEXTO DEL FORMULARIO DE SOLICITUD ===");
            System.out.println(texto);

            // Si tiene encabezados múltiples, usar extracción por orden
            if (tieneEncabezadosMultiples(texto)) {
                String[] valores = extraerValoresEnOrden(texto);
                dto.setSolicitante(valores[0]);
                dto.setUnidad(valores[1]);
                dto.setCampus(valores[2]);
                System.out.println(">>> Nombre (por orden): [" + valores[0] + "]");
                System.out.println(">>> Unidad (por orden): [" + valores[1] + "]");
                System.out.println(">>> Campus (por orden): [" + valores[2] + "]");
            } else {
                dto.setSolicitante(extraerNombreSolicitante(texto));
                dto.setUnidad(extraerUnidad(texto));
                dto.setCampus(extraerCampus(texto));
            }

            dto.setProposito(extraerProposito(texto));
            dto.setLugar(extraerLugar(texto));
            dto.setCi(extraerCampo(texto, "CI[:.]?\\s*([^\\n]+)"));

            dto.setFecha(extraerFecha(texto, "FECHA\\s*:?\\s*([\\d/]+)"));
            dto.setFechaRealizacion(extraerFecha(texto, "REALIZARSE[:\\s]*\\n?(\\d{4}/\\d{2}/\\d{2})"));

            dto.setPresupuesto(extraerMonto(texto, "\\$\\s*([\\d,\\.]+)\\s*\\nPRESUPUESTO"));
            dto.setTotal(extraerMonto(texto, "TOTAL[:\\s]*\\$?\\s*([\\d,\\.]+)"));

            dto.setRubros(new HashMap<>());

            dto.setExtraccionExitosa(true);

        } catch (Exception e) {
            dto.setExtraccionExitosa(false);
            System.err.println("Error al extraer formulario de solicitud: " + e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }

    // ==================== FORMULARIO DE LIQUIDACIÓN ====================
    public FormularioLiquidacionDTO extraerFormularioLiquidacion(byte[] pdfBytes) {
        FormularioLiquidacionDTO dto = new FormularioLiquidacionDTO();

        try {
            DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(key))
                    .buildClient();

            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(pdfBytes));
            AnalyzeResult result = client.beginAnalyzeDocument("prebuilt-read", options).getFinalResult();
            String texto = result.getContent();

            System.out.println("=== TEXTO DEL FORMULARIO DE LIQUIDACIÓN ===");
            System.out.println(texto);

            // Si tiene encabezados múltiples, usar extracción por orden
            if (tieneEncabezadosMultiples(texto)) {
                String[] valores = extraerValoresEnOrden(texto);
                dto.setSolicitante(valores[0]);
                dto.setUnidad(valores[1]);
                dto.setCampus(valores[2]);
                System.out.println(">>> Nombre (por orden): [" + valores[0] + "]");
                System.out.println(">>> Unidad (por orden): [" + valores[1] + "]");
                System.out.println(">>> Campus (por orden): [" + valores[2] + "]");
            } else {
                dto.setSolicitante(extraerNombreSolicitante(texto));
                dto.setUnidad(extraerUnidad(texto));
                dto.setCampus(extraerCampus(texto));
            }

            dto.setCi(extraerCampo(texto, "CI[:.]?\\s*([^\\n]+)"));
            // Extraer el anticipo: buscar "$XXX" después de VALOR o en cualquier parte del texto
            BigDecimal anticipo = extraerMonto(texto, "(?i)VALOR\\s*[:\\s]*\\$?\\s*([\\d,\\.]+)");
            if (anticipo == null) {
                // Fallback: primer monto con $ en el texto
                anticipo = extraerMonto(texto, "\\$\\s*([\\d,\\.]+)");
            }
            dto.setAnticipo(anticipo);
            dto.setProposito(extraerProposito(texto));
            dto.setLugar(extraerLugar(texto));
            dto.setFirma(extraerCampo(texto, "FIRMA DEL SOLICITANTE[:\\s]*([^\\n]+)"));

            dto.setFecha(extraerFecha(texto, "FECHA[:\\s]*([\\d/]+)"));
            dto.setTotalGastos(extraerMonto(texto, "TOTAL GASTOS[:\\s]*\\$?\\s*([\\d,\\.]+)"));
            dto.setDiferencia(extraerMonto(texto, "DIFERENCIA[:\\s]*\\$?\\s*([\\d,\\.]+)"));

            dto.setExtraccionExitosa(true);

        } catch (Exception e) {
            dto.setExtraccionExitosa(false);
            System.err.println("Error al extraer formulario de liquidación: " + e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }

    // ==================== DETECCIÓN DE FORMATO ====================

    /**
     * Detecta si el texto tiene el formato "encabezados múltiples en una línea".
     * Ej. "NOMBRE DEL SOLICITANTE: UNIDAD/COORDINACIÓN: CAMPUS: FECHA :"
     */
    private boolean tieneEncabezadosMultiples(String texto) {
        if (texto == null) return false;
        String upper = texto.toUpperCase();
        return upper.contains("NOMBRE DEL SOLICITANTE:")
                && upper.contains("UNIDAD/COORDINACIÓN:")
                && upper.contains("CAMPUS:")
                && upper.contains("FECHA");
    }

    /**
     * Extrae los 4 valores (nombre, unidad, campus, fecha) en orden
     * a partir del formato "encabezados múltiples".
     * Salta las líneas que son encabezados exactos, no las que contienen la palabra.
     */
    private String[] extraerValoresEnOrden(String texto) {
        String[] resultado = new String[4];

        String[] lineas = texto.split("\\n");
        int valoresEncontrados = 0;

        for (String linea : lineas) {
            String limpia = linea.trim();
            if (limpia.isEmpty()) continue;

            String upper = limpia.toUpperCase();

            // Saltar SOLO si la línea ES exactamente un encabezado (no si lo CONTIENE)
            if (upper.equals("NOMBRE DEL SOLICITANTE") || upper.equals("NOMBRE DEL SOLICITANTE:")) continue;
            if (upper.equals("UNIDAD/COORDINACIÓN") || upper.equals("UNIDAD/COORDINACION")) continue;
            if (upper.equals("UNIDAD/COORDINACIÓN:") || upper.equals("UNIDAD/COORDINACION:")) continue;
            if (upper.equals("CAMPUS") || upper.equals("CAMPUS:")) continue;
            if (upper.equals("FECHA") || upper.equals("FECHA:") || upper.equals("FECHA :")) continue;
            if (upper.equals("VALOR") || upper.equals("VALOR:")) continue;
            if (upper.equals("PROPÓSITO DEL ANTICIPO") || upper.equals("PROPOSITO DEL ANTICIPO")) continue;
            if (upper.equals("LUGAR Y FECHA A REALIZARSE")) continue;
            if (upper.equals("FIRMA DEL SOLICITANTE")) continue;
            if (upper.equals("TOTAL") || upper.equals("DIFERENCIA")) continue;

            // Saltar encabezado múltiple
            if (upper.contains("NOMBRE DEL SOLICITANTE") && upper.contains("UNIDAD/COORDINACIÓN")) continue;

            // Saltar líneas con prefijos específicos
            if (upper.startsWith("FORMATO DE")) continue;
            if (upper.startsWith("NOTA:")) continue;
            if (upper.startsWith("ITI") || upper.startsWith("İTİ")) continue;
            if (upper.startsWith("TECNOLÓGICO") || upper.startsWith("TECNOLOGICO")) continue;
            if (upper.startsWith("PROPÓSITO") || upper.startsWith("PROPOSITO")) continue;
            if (upper.startsWith("LUGAR")) continue;

            // Saltar montos y números puros
            if (limpia.matches("^\\$?\\s*[\\d,\\.]+\\s*$")) continue;

            // Fecha
            if (limpia.matches("^\\d{4}/\\d{2}/\\d{2}$")) {
                resultado[3] = limpia;
                valoresEncontrados++;
                continue;
            }

            // Saltar líneas sin letras
            if (!limpia.matches(".*[a-zA-ZÁÉÍÓÚÑáéíóúñ].*")) continue;

            // Saltar líneas muy largas (notas)
            if (limpia.length() > 100) continue;

            // Asignar por orden
            if (valoresEncontrados == 0) {
                resultado[0] = limpia;
                valoresEncontrados++;
            } else if (valoresEncontrados == 1) {
                resultado[1] = limpia;
                valoresEncontrados++;
            } else if (valoresEncontrados == 2) {
                resultado[2] = limpia;
                valoresEncontrados++;
            } else if (valoresEncontrados == 3) {
                resultado[3] = limpia;
                valoresEncontrados++;
            }

            if (valoresEncontrados >= 4) break;
        }

        return resultado;
    }

    // ==================== EXTRACTORES INDIVIDUALES ====================

    private String extraerNombreSolicitante(String texto) {
        String valor = extraerCampoPorEncabezado(texto, "NOMBRE DEL SOLICITANTE", "NOMBRE");
        if (valor != null) {
            System.out.println(">>> Nombre extraído: [" + valor + "]");
        }
        return valor;
    }

    private String extraerUnidad(String texto) {
        String valor = extraerCampoPorEncabezado(texto, "UNIDAD/COORDINACIÓN", "TEXTO");
        if (valor == null) valor = extraerCampoPorEncabezado(texto, "UNIDAD/COORDINACION", "TEXTO");
        if (valor != null) System.out.println(">>> Unidad extraída: [" + valor + "]");
        return valor;
    }

    private String extraerCampus(String texto) {
        String valor = extraerCampoPorEncabezado(texto, "CAMPUS", "TEXTO");
        if (valor != null) {
            int idxFecha = valor.toUpperCase().indexOf("FECHA");
            if (idxFecha > 0) valor = valor.substring(0, idxFecha).trim();
            System.out.println(">>> Campus extraído: [" + valor + "]");
        }
        return valor;
    }

    private String extraerProposito(String texto) {
        String valor = extraerCampoPorEncabezado(texto, "PROPOSIT", "TEXTO");
        if (valor != null) {
            valor = valor.replaceAll("(?i)anticipo", "").trim();
            if (valor.isEmpty()) valor = null;
        }
        if (valor != null) System.out.println(">>> Proposito extraído: [" + valor + "]");
        return valor;
    }

    private String extraerLugar(String texto) {
        String valor = extraerCampoPorEncabezado(texto, "LUGAR Y FECHA A", "TEXTO");
        if (valor == null) valor = extraerCampoPorEncabezado(texto, "LUGAR Y FECHA", "TEXTO");
        if (valor != null) {
            valor = valor.replaceAll("(?i)realizarse", "").trim();
            if (valor.isEmpty()) valor = null;
        }
        if (valor != null) System.out.println(">>> Lugar extraído: [" + valor + "]");
        return valor;
    }

    // ==================== MÉTODO GENÉRICO ====================

    private String extraerCampoPorEncabezado(String texto, String encabezado, String tipo) {
        if (texto == null) return null;

        int idx = texto.toUpperCase().indexOf(encabezado.toUpperCase());
        if (idx < 0) return null;

        int fin = Math.min(idx + 250, texto.length());
        String fragmento = texto.substring(idx, fin);

        int idxDos = fragmento.indexOf(":");
        if (idxDos >= 0 && idxDos < fragmento.length() - 1) {
            fragmento = fragmento.substring(idxDos + 1).trim();
        }

        if ("NOMBRE".equals(tipo)) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+){1,4})");
            java.util.regex.Matcher m = p.matcher(fragmento);

            String[] prohibidas = {
                    "Unidad", "Coordinacion", "Coordinación", "Campus",
                    "Fecha", "Valor", "Formato", "Solicitante", "Anticipo",
                    "Lugar", "Nombre", "Presentacion", "Presentación",
                    "Firma", "Aprobación", "Aprobacion", "Persona", "Autoriza"
            };

            while (m.find()) {
                String candidato = m.group(1).trim();
                boolean prohibido = false;
                for (String proh : prohibidas) {
                    if (candidato.toLowerCase().contains(proh.toLowerCase())) {
                        prohibido = true;
                        break;
                    }
                }
                if (prohibido) continue;
                return candidato;
            }
            return null;
        } else {
            String[] lineas = fragmento.split("\n");
            for (String linea : lineas) {
                String limpia = linea.trim();
                if (limpia.isEmpty()) continue;

                String upper = limpia.toUpperCase();
                if (upper.equals(encabezado.toUpperCase())) continue;
                if (upper.endsWith(":")) continue;

                int idxCorte = limpia.indexOf("  ");
                if (idxCorte > 0) limpia = limpia.substring(0, idxCorte).trim();

                if (limpia.length() > 100) limpia = limpia.substring(0, 100);

                if (limpia.matches("^[\\d\\$\\.,/\\s-]+$")) continue;

                return limpia;
            }
            return null;
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String extraerCampo(String texto, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(texto);
        return m.find() ? m.group(1).trim() : null;
    }

    private java.time.LocalDate extraerFecha(String texto, String regex) {
        String valor = extraerCampo(texto, regex);
        if (valor == null) return null;
        try {
            return java.time.LocalDate.parse(valor.trim(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal extraerMonto(String texto, String regex) {
        String valor = extraerCampo(texto, regex);
        if (valor == null) return null;
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }
}