package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.NotificacionResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.Notificacion;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.NotificacionRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para la entidad Notificacion.
 * Contiene la lógica de negocio relacionada con las notificaciones enviadas a los actores.
 * Envía por correo (JavaMailSender) y/o Telegram (Bot API) según el canal.
 * El envío se realiza de forma asíncrona para no bloquear la respuesta al cliente.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final TramiteRepository tramiteRepository;
    private final JavaMailSender mailSender;
    private final UsuarioService usuarioService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

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

    // Guardar una nueva notificación y enviarla
    public NotificacionResponseDTO guardar(NotificacionRequestDTO dto) {
        Tramite tramite = tramiteRepository.findById(dto.getTramiteId())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        // Determinar el canal por defecto
        String canal = dto.getEstado() != null ? "Correo" : "Ambos";

        Notificacion notificacion = new Notificacion();
        notificacion.setTramite(tramite);
        notificacion.setDestinatario(dto.getDestinatario());
        notificacion.setTipo(dto.getTipo());
        notificacion.setFechaEnvio(LocalDate.now());
        notificacion.setEstado(dto.getEstado() != null ? dto.getEstado() : "Enviado");
        notificacion.setCanal(canal);

        Notificacion guardada = notificacionRepository.save(notificacion);

        // ✅ Enviar correo de forma ASÍNCRONA
        enviarCorreoAsync(dto.getDestinatario(), dto.getTipo(),
                "Tienes una nueva notificación del sistema de anticipos del ITI. " +
                        "Trámite ID: " + dto.getTramiteId() + ". Tipo: " + dto.getTipo());

        // ✅ Intentar enviar por Telegram si el usuario tiene chatId
        enviarTelegramAsync(dto.getDestinatario(), dto.getTipo(),
                "Trámite ID: " + dto.getTramiteId() + "\nTipo: " + dto.getTipo());

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
            message.setFrom("chatbot.iti@gmail.com");
            mailSender.send(message);
            System.out.println("Correo enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
        }
    }

    // ✅ Método asíncrono para enviar por Telegram
    @Async
    public void enviarTelegramAsync(String correoDestinatario, String asunto, String cuerpo) {
        try {
            // Buscar el chatId del usuario por su correo
            String chatId = usuarioService.obtenerTelegramChatIdPorCorreo(correoDestinatario);

            if (chatId == null || chatId.isEmpty()) {
                System.out.println("Usuario sin chatId de Telegram: " + correoDestinatario);
                return;
            }

            enviarMensajeTelegram(chatId, "📬 *" + asunto + "*\n\n" + cuerpo);
        } catch (Exception e) {
            System.err.println("Error al enviar Telegram: " + e.getMessage());
        }
    }

    // Enviar mensaje por Telegram (método sincrónico interno)
    private void enviarMensajeTelegram(String chatId, String texto) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", texto);
        body.put("parse_mode", "Markdown");

        restTemplate.postForObject(url, body, String.class);
        System.out.println("Mensaje Telegram enviado a chatId: " + chatId);
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
        dto.setCanal(notificacion.getCanal());
        return dto;
    }
}