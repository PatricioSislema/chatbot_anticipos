package ec.edu.iti.anticipos.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Esta entidad representa la tabla "usuario".
 * Almacena los usuarios del sistema con sus credenciales y rol asignado.
 * Incluye el estado de bloqueo, el contador de intentos fallidos,
 * el chat_id de Telegram y la relación con el jefe inmediato.
 */
@Data
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "cedula", nullable = false, unique = true, length = 10)
    private String cedula;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    // Relación: muchos usuarios pertenecen a un solo rol
    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // ===== CAMPOS DE BLOQUEO =====
    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = false;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /**
     * ID de chat de Telegram del usuario.
     * Se llena automáticamente cuando el usuario hace login por el bot.
     * Puede ser null si el usuario nunca ha usado el bot.
     */
    @Column(name = "telegram_chat_id", length = 50)
    private String telegramChatId;

    /**
     * Jefe inmediato del usuario.
     * Solo aplica para Funcionarios.
     * Apunta a otro Usuario que ocupa el rol de "Jefe Inmediato".
     * Puede ser null para los demás roles.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_inmediato_id")
    @JsonIgnore
    private Usuario jefeInmediato;
}