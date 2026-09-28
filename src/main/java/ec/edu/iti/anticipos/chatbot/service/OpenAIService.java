package ec.edu.iti.anticipos.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Servicio de inteligencia artificial.
 * Se conecta a Ollama para interpretar lenguaje natural y generar respuestas.
 * Compatible con futuras migraciones al sidecar SLM de Azure.
 */
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final ChatClient chatClient;

    // Envía el mensaje a la IA y devuelve la respuesta generada
    public String interpretarMensaje(String mensaje) {
        return chatClient.prompt()
                .system("Eres el asistente virtual del Instituto Superior Tecnológico Internacional ITI. " +
                        "Tu función es ayudar a docentes y personal administrativo con la gestión de anticipos de fondos. " +
                        "Responde de forma clara, breve y profesional, enfocándote en el contexto institucional.")
                .user(mensaje)
                .call()
                .content();
    }
}
