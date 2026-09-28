package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final ChatbotService chatbotService;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirMensaje(
            @RequestParam(value = "Body", required = false) String mensaje,
            @RequestParam("From") String from,
            @RequestParam(value = "MediaUrl0", required = false) String mediaUrl,
            @RequestParam(value = "MediaContentType0", required = false) String mediaType) {

        Long chatId = (long) from.hashCode();
        String respuesta;

        // Si el mensaje es un archivo (PDF)
        if (mediaUrl != null && mediaType != null && mediaType.contains("pdf")) {
            try {
                URL url = new URL(mediaUrl);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("Authorization", "Basic " + getTwilioAuth());
                InputStream inputStream = connection.getInputStream();

                MultipartFile archivo = new MultipartFile() {
                    @Override public String getName() { return "archivo"; }
                    @Override public String getOriginalFilename() { return "documento.pdf"; }
                    @Override public String getContentType() { return "application/pdf"; }
                    @Override public boolean isEmpty() { return false; }
                    @Override public long getSize() { return 0; }
                    @Override public byte[] getBytes() throws java.io.IOException {
                        return inputStream.readAllBytes();
                    }
                    @Override public InputStream getInputStream() { return inputStream; }
                    @Override public void transferTo(java.io.File dest) { }
                };

                respuesta = chatbotService.procesarDocumento(archivo, chatId);

            } catch (Exception e) {
                respuesta = "Error al procesar el archivo: " + e.getMessage();
            }
        } else {
            respuesta = chatbotService.procesarMensaje(mensaje, chatId);
        }

        String twiml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response><Message>" + respuesta + "</Message></Response>";

        return ResponseEntity.ok()
                .header("Content-Type", "application/xml")
                .body(twiml);
    }

    // Obtener credenciales de Twilio desde application.properties
    private String getTwilioAuth() {
        return java.util.Base64.getEncoder()
                .encodeToString((twilioAccountSid + ":" + twilioAuthToken).getBytes());
    }
}