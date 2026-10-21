package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;

/**
 * DTO para devolver datos de un usuario al cliente.
 * No se incluye la contraseña por motivo de seguridad.
 * Incluye el estado de bloqueo, el contador de intentos fallidos
 * y el chat_id de Telegram.
 */
@Data
public class UsuarioResponseDTO {
    private Long usuarioId;
    private String cedula;
    private String nombre;
    private String correo;
    private String rolNombre;
    private Boolean bloqueado;
    private Integer intentosFallidos;
    private String telegramChatId;
    private String telefono;
    private String cargo;
    private Long jefeInmediatoId;
    private String jefeNombre;
}