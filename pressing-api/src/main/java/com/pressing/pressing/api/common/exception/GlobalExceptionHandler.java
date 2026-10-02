package com.pressing.pressing.api.common.exception;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;




@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Repris dans le message d'erreur pour ne plus Mentir (la config dit 10MB, pas 5 Mo)
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String tailleMaxFichier;

    @ExceptionHandler(RessourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> ressourceIntrouvable(RessourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(erreur(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(DonneeDejaExistanteException.class)
    public ResponseEntity<Map<String, Object>> donneeDejaExistante(DonneeDejaExistanteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(erreur(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(ConflitDonneeException.class)
    public ResponseEntity<Map<String, Object>> conflitDonnee(ConflitDonneeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(erreur(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentInvalide(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(erreur(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("erreurs", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpsInvalide(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(erreur("Corps de la requête invalide : " + ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametreInvalide(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(erreur("Paramètre invalide : " + ex.getName(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeInvalide(TypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(erreur("Paramètre invalide", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesRefuse(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(erreur("Accès refusé : vos droits ne permettent pas cette action", HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> uploadTropGros(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(erreur("Fichier trop volumineux (maximum " + tailleMaxFichier + ")", HttpStatus.PAYLOAD_TOO_LARGE.value()));
    }

    /**
     * Un tri invalide (GET /api/tarifs?sort=bogus) est une requete client erronee,
     * pas une panne serveur : repondre 400 plutot que 500 evite de noyer les logs
     * de stack traces et de faire croire a un incident.
     *
     * Deux cas distincts : propriete inconnue sur l'entite (PropertyReferenceException)
     * et expression refusee par la couche JPA (InvalidDataAccessApiUsageException).
     */
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class, InvalidDataAccessResourceUsageException.class})
    public ResponseEntity<Map<String, Object>> requeteTriInvalide(RuntimeException ex) {
        log.warn("Requete de tri invalide rejetee : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(erreur("Critère de tri invalide", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erreurGenerique(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erreur("Une erreur interne est survenue", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private Map<String, Object> erreur(String message, int status) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("message", message);
        return body;
    }
}
