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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controlador REST que actúa como webhook de Telegram.
 *
 * Feedback visual inteligente:
 *  - Si el procesamiento termina en menos de 3 seg → NO se muestra "⏳ Procesando...".
 *  - Si el procesamiento tarda más de 3 seg → se muestra "⏳ Procesando..." y se borra al terminar.
 *  - Esto funciona en TODOS los clientes de Telegram (Desktop, móvil, Web).
 */
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final ChatbotService chatbotService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    private static final int SEGUNDOS_ANTES_DE_MOSTRAR = 3;

    // Scheduler para programar el envío del mensaje "procesando"
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

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

                responderCallback(callbackId);
                eliminarBotones(chatId, messageId);

                ResponseBot respuesta = procesarConFeedback(chatId,
                        "⏳ Procesando tu solicitud...",
                        () -> chatbotService.procesarMensajeConBotones(callbackData, chatId));

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

                respuesta = procesarConFeedback(chatId,
                        "⏳ Procesando tu documento...\n📌 Esto puede tardar 1-2 minutos.",
                        () -> {
                            try {
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

                                return chatbotService.procesarDocumentoConBotones(archivo, chatId);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

            } else if (update.getMessage().hasText()) {
                respuesta = procesarConFeedback(chatId,
                        "⏳ Procesando tu mensaje...",
                        () -> chatbotService.procesarMensajeConBotones(
                                update.getMessage().getText(), chatId));
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

    // ==================== PROCESAR CON FEEDBACK CONDICIONAL ====================
    /**
     * Ejecuta una tarea con feedback visual condicional.
     *
     * Si la tarea tarda más de SEGUNDOS_ANTES_DE_MOSTRAR segundos:
     *  - Se envía un mensaje "⏳ Procesando..." que se borra al terminar.
     *
     * Si la tarea termina antes:
     *  - No se envía nada.
     */
    private ResponseBot procesarConFeedback(Long chatId, String textoProcesando,
                                            java.util.function.Supplier<ResponseBot> tarea) {
        final AtomicBoolean mensajeEnviado = new AtomicBoolean(false);
        final Integer[] mensajeId = {null};

        // Programar el envío del mensaje después de N segundos
        ScheduledFuture<?> timerFuturo = scheduler.schedule(() -> {
            Integer id = enviarMensajeConId(chatId, textoProcesando);
            if (id != null) {
                mensajeId[0] = id;
                mensajeEnviado.set(true);
                System.out.println(">>> ⏳ Mensaje 'procesando' enviado con ID: " + id);
            }
        }, SEGUNDOS_ANTES_DE_MOSTRAR, TimeUnit.SECONDS);

        try {
            // Ejecutar la tarea
            ResponseBot resultado = tarea.get();

            // Cancelar el timer (si aún no se ejecutó)
            boolean cancelado = timerFuturo.cancel(false);
            System.out.println(">>> Timer cancelado: " + cancelado);

            // Si el mensaje fue enviado, borrarlo
            if (mensajeEnviado.get() && mensajeId[0] != null) {
                borrarMensaje(chatId, mensajeId[0]);
                System.out.println(">>> 🗑️ Mensaje 'procesando' borrado: " + mensajeId[0]);
            }

            return resultado;

        } catch (Exception e) {
            timerFuturo.cancel(false);

            if (mensajeEnviado.get() && mensajeId[0] != null) {
                borrarMensaje(chatId, mensajeId[0]);
            }

            throw new RuntimeException(e);
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

    // ✅ Enviar mensaje y devolver el message_id (para poder borrarlo después)
    private Integer enviarMensajeConId(Long chatId, String texto) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", texto);
            body.put("parse_mode", "Markdown");

            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response != null && response.get("result") != null) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                return (Integer) result.get("message_id");
            }
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje con ID: " + e.getMessage());
        }
        return null;
    }

    // ✅ Borrar un mensaje por su ID
    private void borrarMensaje(Long chatId, Integer messageId) {
        if (messageId == null) return;
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/deleteMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            restTemplate.postForObject(url, body, String.class);
        } catch (Exception e) {
            System.err.println("Error al borrar mensaje: " + e.getMessage());
        }
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