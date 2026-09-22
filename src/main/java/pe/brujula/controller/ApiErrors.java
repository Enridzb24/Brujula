package pe.brujula.controller;

import java.util.Map;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"No se pudo completar la solicitud.":e.getReason())); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    ResponseEntity<?> validation(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Revisa los campos: usa montos positivos con hasta dos decimales y una fecha válida, no futura.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e) { return ResponseEntity.status(409).body(Map.of("message","El registro ya existe o está relacionado con otros datos. Revisa el correo o la categoría.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) { log.error("No se pudo completar la operación",e); return ResponseEntity.internalServerError().body(Map.of("message","No se pudo completar la operación. Inténtalo de nuevo.")); }
}
