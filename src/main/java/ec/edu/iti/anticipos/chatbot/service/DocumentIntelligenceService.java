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

            dto.setSolicitante(extraerCampo(texto, "NOMBRE DEL SOLICITANTE[:\\s]*([^\\n]+)"));
            dto.setUnidad(extraerCampo(texto, "UNIDAD/COORDINACIÓN[:\\s]*([^\\n]+)"));
            dto.setCampus(extraerCampo(texto, "CAMPUS[:\\s]*([^\\n]+)"));
            dto.setProposito(extraerCampo(texto, "PROPOSITÓ DEL[\\s\\S]*?ANTICIPO[:\\s]*([^\\n]+)"));
            dto.setLugar(extraerCampo(texto, "LUGAR Y FECHA A[\\s\\S]*?REALIZARSE[:\\s]*([^\\n]+)"));
            dto.setCi(extraerCampo(texto, "CI[:.]?\\s*([^\\n]+)"));

            dto.setFecha(extraerFecha(texto, "FECHA\\s*:\\s*([\\d/]+)"));
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

            dto.setSolicitante(extraerCampo(texto, "NOMBRE DEL SOLICITANTE[:\\s]*([^\\n]+)"));
            dto.setCi(extraerCampo(texto, "CI[:.]?\\s*([^\\n]+)"));
            dto.setAnticipo(extraerMonto(texto, "\\$\\s*([\\d,\\.]+)\\s*\\nCAMPUS"));
            dto.setUnidad(extraerCampo(texto, "UNIDAD/COORDINACIÓN[:\\s]*([^\\n]+)"));
            dto.setCampus(extraerCampo(texto, "CAMPUS[:\\s]*([^\\n]+)"));
            dto.setProposito(extraerCampo(texto, "PROPOSITÓ DEL ANTICIPO[:\\s]*([^\\n]+)"));
            dto.setLugar(extraerCampo(texto, "LUGAR Y FECHA A REALIZARSE[:\\s]*([^\\n]+)"));
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

    // ==================== MÉTODOS AUXILIARES ====================

    // Extrae un campo del texto con regex
    private String extraerCampo(String texto, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(texto);
        return m.find() ? m.group(1).trim() : null;
    }

    // Extrae una fecha con regex
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

    // Extrae un monto con regex
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