package ec.edu.iti.anticipos.chatbot.controller;

import ec.edu.iti.anticipos.chatbot.dto.response.ResponseBot;
import ec.edu.iti.anticipos.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST que actúa como webhook de Telegram.
 * Recibe mensajes, documentos y callbacks de botones inline,
 * y delega el procesamiento al ChatbotService.
 * Envía respuestas con o sin botones según corresponda.
 */
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final ChatbotService chatbotService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    // ==================== WEBHOOK PRINCIPAL ====================
    @PostMapping("/webhook")
    public ResponseEntity<String> recibirMensaje(@RequestBody Update update) {
        try {
            // ============ CALLBACK QUERY (clic en botón) ============
            if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                String callbackId = update.getCallbackQuery().getId();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

                // Responder al callback (quitar "cargando" de Telegram)
                responderCallback(callbackId);

                // Eliminar los botones del mensaje original (evita doble clic)
                eliminarBotones(chatId, messageId);

                // ⏳ Mensaje "procesando" mientras se ejecuta la acción
                enviarMensaje(chatId, "⏳ Procesando tu solicitud...\n\n" +
                        "📌 Esto puede tardar unos segundos.");

                ResponseBot respuesta = chatbotService.procesarMensajeConBotones(callbackData, chatId);
                enviarRespuesta(chatId, respuesta);
                return ResponseEntity.ok("OK");
            }

            // ============ MENSAJE DE TEXTO O DOCUMENTO ============
            if (!update.hasMessage()) return ResponseEntity.ok("OK");

            Long chatId = update.getMessage().getChatId();
            ResponseBot respuesta;

            if (update.getMessage().hasDocument()) {
                String fileId = update.getMessage().getDocument().getFileId();
                String fileName = update.getMessage().getDocument().getFileName();

                if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                    enviarMensaje(chatId, "⚠️ El archivo enviado no es un PDF.\n\n" +
                            "📌 Solo acepto documentos en formato PDF.\n\n" +
                            "👉 Adjunta el archivo nuevamente con extensión .pdf");
                    return ResponseEntity.ok("OK");
                }

                // ⏳ Mensaje "procesando" mientras se descarga y procesa
                enviarMensaje(chatId, "⏳ Procesando tu documento...\n\n" +
                        "📌 Esto puede tardar unos segundos.");

                String getFileUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
                Map<String, Object> fileResponse = restTemplate.getForObject(getFileUrl, Map.class);
                Map<String, Object> result = (Map<String, Object>) fileResponse.get("result");
                String filePath = (String) result.get("file_path");

                String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
                InputStream inputStream = new URL(downloadUrl).openStream();

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

                respuesta = chatbotService.procesarDocumentoConBotones(archivo, chatId);

            } else if (update.getMessage().hasText()) {
                respuesta = chatbotService.procesarMensajeConBotones(
                        update.getMessage().getText(), chatId);
            } else {
                respuesta = new ResponseBot("Envía un mensaje de texto o un PDF.");
            }

            enviarRespuesta(chatId, respuesta);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("Error");
        }
    }

    // ==================== ENVIAR MENSAJE CON O SIN BOTONES ====================
    private void enviarRespuesta(Long chatId, ResponseBot respuesta) {
        if (respuesta.tieneBotones()) {
            enviarMensajeConBotones(chatId, respuesta.getTexto(), respuesta.getBotones());
        } else {
            enviarMensaje(chatId, respuesta.getTexto());
        }
    }

    private void enviarMensaje(Long chatId, String texto) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", texto);
        body.put("parse_mode", "Markdown");
        restTemplate.postForObject(url, body, String.class);
    }

    private void enviarMensajeConBotones(Long chatId, String texto,
                                         List<List<ResponseBot.Boton>> botones) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        List<List<Map<String, String>>> inlineKeyboard = new ArrayList<>();
        for (List<ResponseBot.Boton> fila : botones) {
            List<Map<String, String>> filaJson = new ArrayList<>();
            for (ResponseBot.Boton b : fila) {
                Map<String, String> botonJson = new HashMap<>();
                botonJson.put("text", b.getTexto());
                botonJson.put("callback_data", b.getCallbackData());
                filaJson.add(botonJson);
            }
            inlineKeyboard.add(filaJson);
        }

        Map<String, Object> replyMarkup = new HashMap<>();
        replyMarkup.put("inline_keyboard", inlineKeyboard);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", texto);
        body.put("parse_mode", "Markdown");
        body.put("reply_markup", replyMarkup);

        restTemplate.postForObject(url, body, String.class);
    }

    private void responderCallback(String callbackId) {
        String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackId);
        restTemplate.postForObject(url, body, String.class);
    }

    private void eliminarBotones(Long chatId, Integer messageId) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/editMessageReplyMarkup";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            body.put("reply_markup", new HashMap<>());
            restTemplate.postForObject(url, body, String.class);
        } catch (Exception e) {
            System.err.println("Error al eliminar botones: " + e.getMessage());
        }
    }
}