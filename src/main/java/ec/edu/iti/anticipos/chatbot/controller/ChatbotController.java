package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.request.MensajeRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.MensajeResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para el chatbot.
 * Expone los endpoints para recibir mensajes y documentos.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // Recibir un mensaje de texto
    @PostMapping("/mensaje")
    public ResponseEntity<MensajeResponseDTO> recibirMensaje(@RequestBody MensajeRequestDTO dto) {
        String respuesta = chatbotService.procesarMensaje(dto.getTexto(), dto.getUsuarioId());

        MensajeResponseDTO response = new MensajeResponseDTO();
        response.setRespuesta(respuesta);
        response.setExitoso(true);

        return ResponseEntity.ok(response);
    }

    // Recibir un documento PDF (formulario o factura)
    @PostMapping("/documento")
    public ResponseEntity<MensajeResponseDTO> recibirDocumento(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("usuarioId") Long usuarioId) {

        String respuesta = chatbotService.procesarDocumento(archivo, usuarioId);

        MensajeResponseDTO response = new MensajeResponseDTO();
        response.setRespuesta(respuesta);
        response.setExitoso(true);

        return ResponseEntity.ok(response);
    }
}