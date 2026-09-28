package ec.edu.iti.anticipos.chatbot.service.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO que agrupa la configuración de cada rol aprobador.
 * Evita métodos con demasiados parámetros y facilita la extensión del sistema.
 */
@Data
@AllArgsConstructor
public class ConfiguracionAprobador {

    // Estado que el aprobador debe buscar (ej: "Pendiente")
    private String estadoBuscado;

    // Nuevo estado que se asigna al aprobar (ej: "Aprobado por Jefe")
    private String nuevoEstado;

    // Correo del siguiente actor a notificar al aprobar
    private String destinatarioAprobacion;

    // Nombre del rol para los mensajes (ej: "Jefe")
    private String rolNombre;

    // Correo del destinatario en caso de rechazo (siempre el docente)
    private String destinatarioRechazo;
}
