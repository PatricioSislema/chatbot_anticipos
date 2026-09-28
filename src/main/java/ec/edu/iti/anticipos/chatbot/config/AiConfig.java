package ec.edu.iti.anticipos.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente de IA.
 * Se conecta a Ollama local (desarrollo) y es compatible con el sidecar SLM de Azure.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
