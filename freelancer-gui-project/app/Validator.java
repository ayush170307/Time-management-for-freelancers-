package app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Central place for input validation. Every method throws InvalidInputException on bad data. */
public final class Validator {

    private Validator() {
    }

    public static String requireText(String value, String field) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field + " is required.");
        }
        return value.trim();
    }

    public static String requireEmail(String value) throws InvalidInputException {
        String email = requireText(value, "Email");
        if (!isValidEmail(email)) {
            throw new InvalidInputException("Please enter a valid email address.");
        }
        return email;
    }

    public static boolean isValidEmail(String value) {
        return value != null && value.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    /** Reads a yyyy-MM-dd date; returns null when the text is empty. */
    public static LocalDate parseOptionalDate(String text, String field) throws InvalidInputException {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(field + " must look like 2026-10-31 (year-month-day).");
        }
    }

    public static String requirePassword(String value) throws InvalidInputException {
        if (value == null || value.isEmpty()) {
            throw new InvalidInputException("Password is required.");
        }
        if (value.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters long.");
        }
        return value;
    }

    public static double parsePositive(String text, String field) throws InvalidInputException {
        try {
            double value = Double.parseDouble(text == null ? "" : text.trim());
            if (value <= 0) {
                throw new InvalidInputException(field + " must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a valid number.");
        }
    }

    public static int parsePositiveInt(String text, String field) throws InvalidInputException {
        try {
            int value = Integer.parseInt(text == null ? "" : text.trim());
            if (value <= 0) {
                throw new InvalidInputException(field + " must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a whole number.");
        }
    }
}
