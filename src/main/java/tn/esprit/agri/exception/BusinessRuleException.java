package tn.esprit.agri.exception;

public class BusinessRuleException extends RuntimeException {
    private String errorCode;

    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "DEFAULT_ERROR";
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
