package ec.edu.iti.anticipos.chatbot.controller;


import ec.edu.iti.anticipos.chatbot.dto.request.RolRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.RolResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.RolService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la entidad Rol.
 * Expone los endpoints para gestionar los roles del sistema.
 */

@RestController
@RequestMapping("/apí/roles")
@RequiredArgsConstructor
public class RolController {
    private final RolService rolService;

    // Listar todos los roles
    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listar(){
        return ResponseEntity.ok(rolService.listarTodos());
    }


    // Buscar un rol por ID
    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> buscar(@PathVariable Long id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Crear un nuevo rol
    public ResponseEntity<RolResponseDTO> crear(@RequestBody RolRequestDTO dto){
        return ResponseEntity.ok(rolService.guardar(dto));
    }

    //Eliminar un rol por id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id){
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
