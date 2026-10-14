package app;

/** Thrown when the logged-in user tries to do something their role does not allow. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
