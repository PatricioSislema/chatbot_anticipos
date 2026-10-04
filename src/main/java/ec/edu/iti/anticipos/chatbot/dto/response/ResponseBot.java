package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que agrupa la respuesta del bot: el texto y los botones (opcional).
 * Si botones está vacío, se envía solo texto.
 * Si botones tiene filas, se envían botones inline.
 */
@Data
public class ResponseBot {

    private String texto;
    private List<List<Boton>> botones;

    public ResponseBot(String texto) {
        this.texto = texto;
        this.botones = new ArrayList<>();
    }

    public ResponseBot(String texto, List<List<Boton>> botones) {
        this.texto = texto;
        this.botones = botones != null ? botones : new ArrayList<>();
    }

    public boolean tieneBotones() {
        return botones != null && !botones.isEmpty();
    }

    /**
     * Representa un botón inline de Telegram.
     */
    @Data
    public static class Boton {
        private String texto;
        private String callbackData;

        public Boton(String texto, String callbackData) {
            this.texto = texto;
            this.callbackData = callbackData;
        }
    }

    // ==================== HELPERS PARA CREAR BOTONES ====================

    public static List<Boton> fila(Boton... botones) {
        List<Boton> fila = new ArrayList<>();
        for (Boton b : botones) fila.add(b);
        return fila;
    }

    public static Boton boton(String texto, String callbackData) {
        return new Boton(texto, callbackData);
    }
}