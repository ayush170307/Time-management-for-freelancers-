package app;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Stops anything except digits (and a limited decimal part) from being typed or pasted into a field,
 * so negative numbers and letters can never be entered in the first place.
 */
public class NumericFilter extends DocumentFilter {

    private final int maxDecimals;

    /** @param maxDecimals 0 allows whole numbers only; 2 allows values like 12.50 */
    public NumericFilter(int maxDecimals) {
        this.maxDecimals = maxDecimals;
    }

    private boolean isValid(String text) {
        if (text.isEmpty()) {
            return true;
        }
        if (maxDecimals == 0) {
            return text.matches("\\d{1,9}");
        }
        return text.matches("\\d{0,9}(\\.\\d{0," + maxDecimals + "})?");
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        String next = new StringBuilder(current).insert(offset, string).toString();
        if (isValid(next)) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        String next = new StringBuilder(current).replace(offset, offset + length, text == null ? "" : text).toString();
        if (isValid(next)) {
            super.replace(fb, offset, length, text, attrs);
        }
    }
}
