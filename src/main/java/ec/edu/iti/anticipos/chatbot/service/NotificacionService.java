package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.NotificacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Notificacion;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.NotificacionRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Notificacion.
 * Contiene la lógica de negocio relacionada con las notificaciones enviadas a los actores.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final TramiteRepository tramiteRepository;
    private final EmailService emailService;

    // Listar todas las notificaciones
    public List<NotificacionResponseDTO> listarTodos() {
        return notificacionRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // Buscar una notificación por ID
    public Optional<NotificacionResponseDTO> buscarPorId(Long id) {
        return notificacionRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Guardar una nueva notificación y enviar el correo
    public NotificacionResponseDTO guardar(NotificacionRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        Notificacion notificacion = new Notificacion();
        notificacion.setTramite(tramite);
        notificacion.setDestinatario(dto.getDestinatario());
        notificacion.setTipo(dto.getTipo());
        notificacion.setFechaEnvio(LocalDate.now());
        notificacion.setEstado(dto.getEstado() != null ? dto.getEstado() : "Enviado");

        Notificacion guardada = notificacionRepository.save(notificacion);

        // Enviar el correo
        String asunto = "Notificación del sistema de anticipos ITI";
        String cuerpo = "Estimado/a usuario/a,\n\n" +
                "Se le informa que su trámite ha cambiado de estado.\n" +
                "Tipo: " + dto.getTipo() + "\n" +
                "Fecha: " + LocalDate.now() + "\n\n" +
                "Atentamente,\n" +
                "Sistema de Gestión de Anticipos ITI";

        emailService.enviarCorreo(dto.getDestinatario(), asunto, cuerpo);

        return convertirAResponse(guardada);
    }

    // Eliminar una notificación por ID
    public void eliminar(Long id) {
        notificacionRepository.deleteById(id);
    }

    // Convertir entidad a DTO de respuesta
    private NotificacionResponseDTO convertirAResponse(Notificacion notificacion) {
        NotificacionResponseDTO dto = new NotificacionResponseDTO();
        dto.setNotificacionId(notificacion.getNotificacionId());
        dto.setTramiteId(notificacion.getTramite().getTramiteId());
        dto.setDestinatario(notificacion.getDestinatario());
        dto.setTipo(notificacion.getTipo());
        dto.setFechaEnvio(notificacion.getFechaEnvio());
        dto.setEstado(notificacion.getEstado());
        return dto;
    }
}