package ec.edu.iti.anticipos.chatbot.util;

/**
 * Helper para construir mensajes enriquecidos para notificaciones (correo + Telegram).
 */
public class MensajeHelper {

    public static String nuevaSolicitud(Long tramiteId, String solicitante, String proposito, String presupuesto) {
        return "📬 *Nueva solicitud pendiente*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "👤 Solicitante: " + solicitante + "\n" +
                "🎯 Propósito: " + proposito + "\n" +
                "💰 Presupuesto: $" + presupuesto + "\n\n" +
                "👉 Ingresa al bot para revisarla.";
    }

    public static String tramiteAprobado(Long tramiteId, String solicitante, String estado) {
        return "✅ *Trámite aprobado*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "👤 Solicitante: " + solicitante + "\n" +
                "📊 Nuevo estado: " + estado + "\n\n" +
                "👉 Ingresa al bot para revisarlo.";
    }

    public static String tramiteRechazado(Long tramiteId, String rolRechaza, String motivo) {
        return "❌ *Trámite rechazado*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "👤 Rechazado por: " + rolRechaza + "\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "👉 Ingresa al bot para más detalles.";
    }

    public static String liquidacionAprobada(Long tramiteId, String tipoCaso, String saldo, String mensajeExtra) {
        return "✅ *Liquidación aprobada*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "📊 Tipo: " + tipoCaso + "\n" +
                "💰 Saldo: $" + saldo + "\n\n" +
                mensajeExtra + "\n\n" +
                "👉 Ingresa al bot para más detalles.";
    }

    public static String ordenGenerada(Long ordenId, Long tramiteId, String valor, String tipo) {
        return "📄 *Nueva orden de transferencia*\n\n" +
                "🆔 Orden: #" + ordenId + "\n" +
                "📋 Trámite: #" + tramiteId + "\n" +
                "🏷️ Tipo: " + tipo + "\n" +
                "💰 Valor: $" + valor + "\n\n" +
                "👉 Ingresa al bot para aprobarla.";
    }

    public static String ordenAprobada(Long ordenId, Long tramiteId, String valor) {
        return "✅ *Orden de transferencia aprobada*\n\n" +
                "🆔 Orden: #" + ordenId + "\n" +
                "📋 Trámite: #" + tramiteId + "\n" +
                "💰 Valor: $" + valor + "\n\n" +
                "📌 El dinero se ha efectuado.\n\n" +
                "👉 La Contadora debe registrar el comprobante.";
    }

    public static String comprobanteRegistrado(Long tramiteId, String valor) {
        return "✅ *Anticipo transferido*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "💰 Valor: $" + valor + "\n\n" +
                "🎉 El anticipo ha sido transferido correctamente.";
    }

    public static String devolucionValidada(Long tramiteId, String monto) {
        return "✅ *Devolución validada*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "💰 Monto: $" + monto + "\n\n" +
                "🎉 La liquidación ha sido cerrada.";
    }

    public static String reembolsoAutorizado(Long reembolsoId, Long tramiteId, String monto) {
        return "✅ *Reembolso autorizado*\n\n" +
                "🆔 Reembolso: #" + reembolsoId + "\n" +
                "📋 Trámite: #" + tramiteId + "\n" +
                "💰 Monto: $" + monto + "\n\n" +
                "📌 La Contadora debe informar a la Colectora.";
    }

    public static String reembolsoCerrado(Long tramiteId, String monto) {
        return "✅ *Liquidación cerrada*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "💰 Monto reembolsado: $" + monto + "\n\n" +
                "🎉 El reembolso se ha procesado correctamente.";
    }

    public static String informarColectora(Long tramiteId, Long reembolsoId, String monto) {
        return "💰 *Reembolso autorizado - Generar orden*\n\n" +
                "🆔 Reembolso: #" + reembolsoId + "\n" +
                "📋 Trámite: #" + tramiteId + "\n" +
                "💵 Monto: $" + monto + "\n\n" +
                "👉 Ingresa al bot para generar la orden de transferencia.";
    }

    public static String liquidacionCerrada(Long tramiteId, String tipoCaso, String monto) {
        return "🎉 *Liquidación cerrada*\n\n" +
                "🆔 Trámite: #" + tramiteId + "\n" +
                "📊 Caso: " + tipoCaso + "\n" +
                "💰 Monto: $" + monto + "\n\n" +
                "La liquidación se ha cerrado correctamente.";
    }
}