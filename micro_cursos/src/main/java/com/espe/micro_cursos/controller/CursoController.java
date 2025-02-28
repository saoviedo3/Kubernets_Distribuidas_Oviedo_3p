package com.espe.micro_cursos.controller;

import com.espe.micro_cursos.models.Usuario;
import com.espe.micro_cursos.models.entities.Curso;
import com.espe.micro_cursos.services.CursoService;
import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    @Autowired
    private CursoService service;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Curso curso, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        Curso cursoDb = service.save(curso);
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoDb);
    }

    @GetMapping
    public ResponseEntity<List<Curso>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Curso> cursoOptional = service.findById(id);
        if (cursoOptional.isPresent()) {
            return ResponseEntity.ok(cursoOptional.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Curso no encontrado"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Valid @RequestBody Curso curso, BindingResult result, @PathVariable Long id) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        Optional<Curso> cursoOptional = service.findById(id);
        if (cursoOptional.isPresent()) {
            Curso cursoDb = cursoOptional.get();
            cursoDb.setNombre(curso.getNombre());
            cursoDb.setDescripcion(curso.getDescripcion());
            cursoDb.setCreditos(curso.getCreditos());
            return ResponseEntity.status(HttpStatus.CREATED).body(service.save(cursoDb));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Curso no encontrado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<Curso> curso = service.findById(id);
        if (curso.isPresent()) {
            service.delete(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Curso eliminado correctamente"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Curso no encontrado"));
    }

    @PostMapping("/{id}/usuarios")
    public ResponseEntity<?> assignUser(@PathVariable Long id, @Valid @RequestBody Usuario usuario) {
        try {
            Optional<Usuario> usuarioFeign = service.findUsuarioById(usuario.getId());
            if (usuarioFeign.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Usuario no encontrado"));
            }

            Optional<Curso> cursoOptional = service.findById(id);
            if (cursoOptional.isPresent()) {
                Curso curso = cursoOptional.get();

                // Validar si el usuario ya está matriculado
                boolean isAlreadyEnrolled = curso.getCursoUsuarios().stream()
                        .anyMatch(cursoUsuario -> cursoUsuario.getUsuarioId().equals(usuario.getId()));
                if (isAlreadyEnrolled) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "El usuario ya está matriculado en este curso"));
                }

                // Matricular usuario si no está ya matriculado
                curso.addUsuario(usuario);
                service.save(curso);
                return ResponseEntity.status(HttpStatus.CREATED).body(curso);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Curso no encontrado"));
        } catch (FeignException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Error en la comunicación con el servicio de usuarios"));
        }
    }

    @DeleteMapping("/{id}/usuarios/{usuarioId}")
    public ResponseEntity<?> unassignUser(@PathVariable Long id, @PathVariable Long usuarioId) {
        Optional<Curso> cursoOptional = service.findById(id);
        if (cursoOptional.isPresent()) {
            Curso curso = cursoOptional.get();
            curso.removeUsuario(usuarioId);
            service.save(curso);
            return ResponseEntity.ok(Collections.singletonMap("message", "Matricula eliminada correctamente"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Matricula no encontrada"));
    }

    @GetMapping("/{id}/usuarios")
    public ResponseEntity<?> listUsersByCourse(@PathVariable Long id) {
        Optional<Curso> cursoOptional = service.findById(id);
        if (cursoOptional.isPresent()) {
            return ResponseEntity.ok(cursoOptional.get().getUsuarios());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "Curso no encontrado"));
    }
    @PostMapping("/usuarios")
    public ResponseEntity<?> createUsuario(@Valid @RequestBody Usuario usuario) {
        try {
            Usuario usuarioCreado = service.addUsuarioToSystem(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Error al crear el usuario"));
        }
    }

}
