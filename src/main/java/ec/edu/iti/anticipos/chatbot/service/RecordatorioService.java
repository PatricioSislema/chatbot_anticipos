package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio que corre periódicamente (cada 6 horas) y revisa si hay
 * liquidaciones próximas a vencer (5 días desde la acreditación del anticipo).
 * Si las hay, notifica a la Contadora.
 */
@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    // Constante: días de plazo desde la acreditación
    private static final int DIAS_PLAZO = 5;

    // Constante: días de aviso antes del vencimiento
    private static final int DIAS_AVISO = 1;

    /**
     * Corre cada 6 horas.
     * Cron: segundo minuto hora día-del-mes mes día-de-semana
     * 0 0 0/6 * * * → Cada 6 horas (00:00, 06:00, 12:00, 18:00)
     */
    @Scheduled(cron = "0 0 0/6 * * *")
    public void revisarLiquidacionesProximasAVencer() {
        System.out.println("=== RECORDATORIO: Revisando liquidaciones próximas a vencer ===");

        // Buscar trámites en estado "Transferido" (anticipo acreditado al funcionario)
        List<Tramite> transferidos = tramiteRepository.findByEstado("Transferido");

        if (transferidos.isEmpty()) {
            System.out.println("No hay trámites transferidos pendientes de liquidar.");
            return;
        }

        LocalDate hoy = LocalDate.now();
        int avisosEnviados = 0;

        for (Tramite tramite : transferidos) {
            long diasTranscurridos = ChronoUnit.DAYS.between(tramite.getFechaCreacion(), hoy);
            long diasRestantes = DIAS_PLAZO - diasTranscurridos;

            // Si faltan 1 día o menos para el vencimiento y aún no venció
            if (diasRestantes > 0 && diasRestantes <= DIAS_AVISO) {
                notificarContadora(tramite, diasRestantes);
                avisosEnviados++;
            }
        }

        System.out.println("Recordatorios enviados: " + avisosEnviados);
    }

    // Notificar a la Contadora que un trámite está próximo a vencer
    private void notificarContadora(Tramite tramite, long diasRestantes) {
        // Buscar a la Contadora
        List<Usuario> contadoras = usuarioRepository.findByRolNombre("Contadora");

        if (contadoras.isEmpty()) {
            System.err.println("No se encontró ninguna Contadora para notificar.");
            return;
        }

        for (Usuario contadora : contadoras) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramite.getTramiteId());
            notifDTO.setDestinatario(contadora.getCorreo());
            notifDTO.setTipo("Recordatorio: liquidación próxima a vencer");
            notifDTO.setEstado("Enviado");

            notificacionService.guardar(notifDTO);

            System.out.println("Recordatorio enviado a " + contadora.getCorreo() +
                    " para trámite " + tramite.getTramiteId() +
                    " (vence en " + diasRestantes + " días)");
        }
    }
}