package app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A very small PDF writer: one A4 page, filled rectangles, lines and text in the built-in
 * Helvetica fonts. It needs no external library, so invoices can be exported as real PDFs.
 * Coordinates are in points measured from the TOP-LEFT corner of the page.
 */
public final class SimplePdf {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    private final StringBuilder content = new StringBuilder();

    public void fillRect(double x, double top, double width, double height, int r, int g, int b) {
        content.append(color(r, g, b)).append(" rg ")
                .append(num(x)).append(' ').append(num(PAGE_HEIGHT - top - height)).append(' ')
                .append(num(width)).append(' ').append(num(height)).append(" re f\n");
    }

    public void line(double x1, double top1, double x2, double top2, double lineWidth, int r, int g, int b) {
        content.append(color(r, g, b)).append(" RG ").append(num(lineWidth)).append(" w ")
                .append(num(x1)).append(' ').append(num(PAGE_HEIGHT - top1)).append(" m ")
                .append(num(x2)).append(' ').append(num(PAGE_HEIGHT - top2)).append(" l S\n");
    }

    /** Draws text with its left edge at x and its baseline at the given distance from the top. */
    public void text(String text, double x, double baselineTop, double size, boolean bold, int r, int g, int b) {
        content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(num(size)).append(" Tf ")
                .append(color(r, g, b)).append(" rg ")
                .append(num(x)).append(' ').append(num(PAGE_HEIGHT - baselineTop)).append(" Td (")
                .append(escape(text)).append(") Tj ET\n");
    }

    /** Draws text so that its right edge is at rightX. */
    public void textRight(String text, double rightX, double baselineTop, double size, boolean bold,
                          int r, int g, int b) {
        text(text, rightX - width(text, size, bold), baselineTop, size, bold, r, g, b);
    }

    /** Approximate text width in points (good enough for right-aligning numbers). */
    public static double width(String text, double size, boolean bold) {
        double total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += charWidth(text.charAt(i), bold);
        }
        return total * size;
    }

    /** Shortens text with "..." so it fits in maxWidth points. */
    public static String fit(String text, double size, boolean bold, double maxWidth) {
        String value = text == null ? "" : text;
        if (width(value, size, bold) <= maxWidth) {
            return value;
        }
        String cut = value;
        while (cut.length() > 1 && width(cut + "...", size, bold) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private static double charWidth(char c, boolean bold) {
        if (c >= '0' && c <= '9') {
            return 0.556;
        }
        if (c == ' ' || c == '.' || c == ',' || c == ':' || c == ';' || c == '\'') {
            return 0.278;
        }
        if (c == 'i' || c == 'l' || c == 'j' || c == '|') {
            return 0.222;
        }
        if (c == 'm' || c == 'w' || c == 'M' || c == 'W') {
            return 0.8;
        }
        if (Character.isUpperCase(c)) {
            return bold ? 0.72 : 0.667;
        }
        return bold ? 0.556 : 0.5;
    }

    /** Assembles the finished PDF file. */
    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        write(out, "%PDF-1.4\n");

        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
                + "] /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>\nendobj\n");

        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
        out.write(stream, 0, stream.length);
        write(out, "\nendstream\nendobj\n");

        offsets.add(out.size());
        write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        offsets.add(out.size());
        write(out, "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        int xrefStart = out.size();
        write(out, "xref\n0 7\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF\n");
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ helpers

    private static void write(ByteArrayOutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String color(int r, int g, int b) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", r / 255.0, g / 255.0, b / 255.0);
    }

    /** Escapes characters that have a special meaning inside a PDF string and drops unsupported ones. */
    private static String escape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c < 32 || c > 255) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
