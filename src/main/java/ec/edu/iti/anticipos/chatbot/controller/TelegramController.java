package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final ChatbotService chatbotService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @PostMapping("/webhook")
    public ResponseEntity<String> recibirMensaje(@RequestBody Update update) {
        try {
            if (!update.hasMessage()) return ResponseEntity.ok("OK");

            Long chatId = update.getMessage().getChatId();
            String respuesta;

            if (update.getMessage().hasDocument()) {
                String fileId = update.getMessage().getDocument().getFileId();
                String fileName = update.getMessage().getDocument().getFileName();

                if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                    enviarMensaje(chatId, "⚠️ Solo acepto archivos PDF.");
                    return ResponseEntity.ok("OK");
                }

                // 1. Obtener ruta del archivo
                String getFileUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
                Map<String, Object> fileResponse = restTemplate.getForObject(getFileUrl, Map.class);
                Map<String, Object> result = (Map<String, Object>) fileResponse.get("result");
                String filePath = (String) result.get("file_path");

                // 2. Descargar archivo
                String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
                InputStream inputStream = new URL(downloadUrl).openStream();

                // 3. Envolver en MultipartFile
                org.springframework.web.multipart.MultipartFile archivo = new org.springframework.web.multipart.MultipartFile() {
                    @Override public String getName() { return "archivo"; }
                    @Override public String getOriginalFilename() { return fileName; }
                    @Override public String getContentType() { return "application/pdf"; }
                    @Override public boolean isEmpty() { return false; }
                    @Override public long getSize() { return 0; }
                    @Override public byte[] getBytes() throws java.io.IOException { return inputStream.readAllBytes(); }
                    @Override public InputStream getInputStream() { return inputStream; }
                    @Override public void transferTo(java.io.File dest) { }
                };

                respuesta = chatbotService.procesarDocumento(archivo, chatId);

            } else if (update.getMessage().hasText()) {
                respuesta = chatbotService.procesarMensaje(update.getMessage().getText(), chatId);
            } else {
                respuesta = "Envía un mensaje de texto o un PDF.";
            }

            enviarMensaje(chatId, respuesta);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("Error");
        }
    }

    private void enviarMensaje(Long chatId, String texto) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", texto);
        restTemplate.postForObject(url, body, String.class);
    }
}