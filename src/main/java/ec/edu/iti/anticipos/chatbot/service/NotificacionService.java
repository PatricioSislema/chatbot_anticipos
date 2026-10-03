package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.NotificacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Notificacion;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.NotificacionRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la entidad Notificacion.
 * Contiene la lógica de negocio relacionada con las notificaciones enviadas a los actores.
 * El envío de correo se realiza de forma asíncrona para no bloquear la respuesta a Twilio.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final TramiteRepository tramiteRepository;
    private final JavaMailSender mailSender;

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

    // Guardar una nueva notificación (guarda en BD y envía correo asíncrono)
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

        // ✅ Enviar correo de forma ASÍNCRONA (no bloquea la respuesta)
        enviarCorreoAsync(dto.getDestinatario(), dto.getTipo(),
                "Tienes una nueva notificación del sistema de anticipos del ITI. " +
                        "Trámite ID: " + dto.getTramiteId() + ". Tipo: " + dto.getTipo());

        return convertirAResponse(guardada);
    }

    // ✅ Método asíncrono para enviar correo
    @Async
    public void enviarCorreoAsync(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject(asunto);
            message.setText(cuerpo);
            mailSender.send(message);
            System.out.println("Correo enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
        }
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