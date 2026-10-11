package ec.edu.iti.anticipos.chatbot.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ComprobanteExtraidoDTO {

    private String bancoOrigen;
    private String cuentaOrigen;
    private BigDecimal montoDepositado;
    private LocalDate fechaDeposito;

    private boolean esDelIti;
    private boolean montoEncontrado;
    private boolean montoCoincide;
    private boolean fechaReciente;

    private String textoExtraido;
    private String observacion;
}