package app;

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
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new InvalidInputException("Please enter a valid email address.");
        }
        return email;
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
