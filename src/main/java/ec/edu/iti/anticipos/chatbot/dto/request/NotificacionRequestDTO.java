package ec.edu.iti.anticipos.chatbot.dto.request;

import lombok.Data;

@Data
public class NotificacionRequestDTO {
    private Long tramiteId;
    private String destinatario;
    private String tipo;
    private String estado;
    private String mensajePersonalizado;
}