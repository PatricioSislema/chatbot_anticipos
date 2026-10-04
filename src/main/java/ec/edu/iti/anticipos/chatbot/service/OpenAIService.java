package ec.edu.iti.anticipos.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Servicio de inteligencia artificial.
 * Se conecta a Ollama (phi3:mini) para:
 *  1. Clasificar la intención del usuario (devuelve una palabra clave).
 *  2. Responder consultas generales (modo IAITI).
 */
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final ChatClient chatClient;

    // ==========================================
    // 1. CLASIFICADOR DE INTENCIONES
    // ==========================================
    /**
     * Recibe el mensaje del usuario y devuelve una de las intenciones válidas:
     *   SOLICITAR, LIQUIDAR, CONSULTAR, AYUDA, SALIR, MENU, CANCELAR, CONSULTA_GENERAL, DESCONOCIDA
     */
    public String clasificarIntencion(String mensaje) {
        // Si es solo un número, no llamar a la IA (es probable que sea un ID)
        if (mensaje != null && mensaje.trim().matches("\\d+")) {
            return "DESCONOCIDA";
        }
        String prompt = """
            Eres un clasificador de intenciones para un chatbot del ITI.
            Analiza el mensaje del usuario y responde SOLO con UNA de estas palabras clave:

            SOLICITAR  - si quiere pedir un anticipo, adelanto, plata, dinero, fondos, viáticos
            LIQUIDAR   - si quiere liquidar, rendir, justificar, cuadrar cuentas, presentar facturas
            CONSULTAR  - si pregunta por el estado de su trámite, solicitud, seguimiento
            AYUDA      - si pide ayuda, opciones, qué puede hacer
            SALIR      - si quiere cerrar sesión, salir, terminar
            MENU       - si quiere volver al menú principal, inicio
            CANCELAR   - si quiere cancelar la operación actual
            CONSULTA_GENERAL - cualquier otra pregunta (capital de Ecuador, clima, chistes)
            DESCONOCIDA - si no entiendes nada

            Responde SOLO con la palabra clave en MAYÚSCULAS. Sin explicaciones, sin puntos, sin comillas.

            Mensaje: "%s"
            """.formatted(mensaje);

        try {
            String respuesta = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim()
                    .toUpperCase()
                    .replaceAll("[^A-Z_]", "");

            System.out.println("[IA] Intención detectada: " + respuesta);
            return respuesta;
        } catch (Exception e) {
            System.err.println("[IA] Error al clasificar: " + e.getMessage());
            return "DESCONOCIDA";
        }
    }

    // ==========================================
    // 2. RESPUESTA A CONSULTAS GENERALES (modo IAITI)
    // ==========================================
    public String responderConsultaGeneral(String mensaje) {
        return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                "Puedo ayudarte con:\n" +
                "📝 Solicitar un anticipo\n" +
                "💰 Liquidar un anticipo\n" +
                "📋 Consultar el estado de tus trámites\n\n" +
                "👉 Usa los botones para continuar.";
    }

    // ==========================================
    // 3. FALLBACK: RESPUESTA GENERAL (por compatibilidad)
    // ==========================================
    public String interpretarMensaje(String mensaje) {
        return responderConsultaGeneral(mensaje);
    }
}