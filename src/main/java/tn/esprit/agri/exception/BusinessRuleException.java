package tn.esprit.agri.exception;

/**
 * Exception personnalisée pour les violations de règles métier.
 * Permet de renvoyer des codes d'erreur spécifiques et des messages clairs.
 */
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructeur principal
     * @param message   Message d'erreur clair pour l'utilisateur
     * @param errorCode Code unique pour identifier la règle violée (utile pour frontend/logs)
     */
    public BusinessRuleException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructeur pour compatibilité avec le code de l'équipe
     */
    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "DEFAULT_ERROR";
    }

    /**
     * Constructeur avec cause (si besoin)
     */
    public BusinessRuleException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
