package tn.esprit.agri.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Specific handler for business rules (e.g. min farmers, invalid data, etc.)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Violation de règle métier");
        error.put("errorCode", getErrorCodeFromMessage(ex.getMessage()));  // optional helper
        error.put("message", ex.getMessage());

        // Add helpful suggestion based on common messages
        if (ex.getMessage().contains("2 agriculteurs")) {
            error.put("suggestion", "Ajoutez d'autres agriculteurs éligibles ou modifiez le type de culture / score minimum.");
        }

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Fallback for other runtime errors
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGenericRuntime(RuntimeException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Erreur interne");
        error.put("message", "Une erreur inattendue s'est produite. Contactez l'administrateur.");
        error.put("details", ex.getMessage());  // only for dev – remove in production

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Optional helper to make errorCode dynamic
    private String getErrorCodeFromMessage(String message) {
        if (message.contains("2 agriculteurs")) return "MIN_ELIGIBLE_FARMERS_REQUIRED";
        return "UNKNOWN_BUSINESS_RULE";
    }
}