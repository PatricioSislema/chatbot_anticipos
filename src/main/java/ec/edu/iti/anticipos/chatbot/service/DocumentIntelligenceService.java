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
import ec.edu.iti.anticipos.chatbot.service.parser.LiquidacionParser;
import ec.edu.iti.anticipos.chatbot.service.parser.SolicitudParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentIntelligenceService {

    @Value("${azure.document.endpoint}")
    private String endpoint;

    @Value("${azure.document.key}")
    private String key;

    private final SolicitudParser solicitudParser;
    private final LiquidacionParser liquidacionParser;

    // ==================== FACTURAS ====================
    public FacturaExtraidaDTO extraerDatos(byte[] pdfBytes) {
        FacturaExtraidaDTO dto = new FacturaExtraidaDTO();

        try {
            DocumentIntelligenceClient client = crearCliente();

            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(pdfBytes));
            AnalyzeResult result = client.beginAnalyzeDocument("prebuilt-invoice", options).getFinalResult();

            System.out.println("=== RESULTADO DE AZURE (FACTURA) ===");
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
                    if (campoInvoiceTotal.getValueNumber() != null) {
                        dto.setMonto(BigDecimal.valueOf(campoInvoiceTotal.getValueNumber()));
                    } else if (campoInvoiceTotal.getValueCurrency() != null) {
                        dto.setMonto(BigDecimal.valueOf(campoInvoiceTotal.getValueCurrency().getAmount()));
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
        try {
            String texto = extraerTextoPlano(pdfBytes);
            return solicitudParser.parsear(texto);
        } catch (Exception e) {
            System.err.println("Error al extraer formulario de solicitud: " + e.getMessage());
            e.printStackTrace();
            FormularioSolicitudDTO dto = new FormularioSolicitudDTO();
            dto.setExtraccionExitosa(false);
            return dto;
        }
    }

    // ==================== FORMULARIO DE LIQUIDACIÓN ====================
    public FormularioLiquidacionDTO extraerFormularioLiquidacion(byte[] pdfBytes) {
        try {
            String texto = extraerTextoPlano(pdfBytes);
            return liquidacionParser.parsear(texto);
        } catch (Exception e) {
            System.err.println("Error al extraer formulario de liquidación: " + e.getMessage());
            e.printStackTrace();
            FormularioLiquidacionDTO dto = new FormularioLiquidacionDTO();
            dto.setExtraccionExitosa(false);
            return dto;
        }
    }

    // ==================== DETECCIÓN DE TIPO ====================
    public String detectarTipoFormulario(String texto) {
        if (texto == null) return "DESCONOCIDO";

        String upper = texto.toUpperCase();

        if (upper.contains("FORMULARIO DE ANTICIPO DE FONDOS")) {
            return "SOLICITUD";
        }
        if (upper.contains("FORMATO DE LIQUIDACIÓN DE FONDOS") ||
                upper.contains("FORMATO DE LIQUIDACION DE FONDOS")) {
            return "LIQUIDACION";
        }

        if (upper.contains("ANTICIPO DE FONDOS") && !upper.contains("LIQUIDACI")) {
            return "SOLICITUD";
        }
        if (upper.contains("LIQUIDACI")) {
            return "LIQUIDACION";
        }

        return "DESCONOCIDO";
    }

    // ==================== HELPERS ====================

    /**
     * Extrae el texto plano del PDF usando Azure DI (prebuilt-read).
     * ✅ AHORA ES PÚBLICO para que FuncionarioChatService pueda validar comprobantes.
     */
    public String extraerTextoPlano(byte[] pdfBytes) {
        DocumentIntelligenceClient client = crearCliente();

        AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(pdfBytes));
        AnalyzeResult result = client.beginAnalyzeDocument("prebuilt-read", options).getFinalResult();

        return result.getContent();
    }

    private DocumentIntelligenceClient crearCliente() {
        return new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
    }
}