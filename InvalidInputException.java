package app;

/** Checked exception for bad user input (empty fields, negative numbers, etc.). */
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}
