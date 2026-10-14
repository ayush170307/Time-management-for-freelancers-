package app;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Exports timesheets and earnings reports as CSV files.
 * Excel, Google Sheets and LibreOffice all open CSV directly.
 */
public final class ReportExporter {

    private ReportExporter() {
    }

    public static void exportTimesheet(Component parent, List<TimeEntry> entries) {
        if (entries.isEmpty()) {
            Ui.error(parent, "There is no logged time to export yet.");
            return;
        }
        File file = chooseFile(parent, "timesheet.csv");
        if (file == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Project,Start,End,Hours\r\n");
        double total = 0;
        for (TimeEntry t : entries) {
            total += t.getHoursWorked();
            sb.append(csv(t.getStartTime().toLocalDate().toString())).append(',')
                    .append(csv(t.getProjectName())).append(',')
                    .append(csv(t.getStartTime().toLocalTime().withNano(0).toString())).append(',')
                    .append(csv(t.getEndTime().toLocalTime().withNano(0).toString())).append(',')
                    .append(num(t.getHoursWorked())).append("\r\n");
        }
        sb.append(",,,Total hours,").append(num(total)).append("\r\n");
        write(parent, file, sb.toString());
    }

    public static void exportEarnings(Component parent, List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            Ui.error(parent, "There are no invoices to export yet.");
            return;
        }
        File file = chooseFile(parent, "earnings-report.csv");
        if (file == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice #,Date,Freelancer,Client,Project,Type,Hours,Total,Status\r\n");
        double invoiced = 0;
        double paid = 0;
        for (Invoice inv : invoices) {
            double total = inv.calculateTotal();
            invoiced += total;
            if (inv.getStatus() == Invoice.Status.PAID) {
                paid += total;
            }
            sb.append(inv.getId()).append(',')
                    .append(csv(inv.getCreatedDate().toString())).append(',')
                    .append(csv(inv.getFreelancerName())).append(',')
                    .append(csv(inv.getClientName())).append(',')
                    .append(csv(inv.getProjectName())).append(',')
                    .append(inv.getType()).append(',')
                    .append(num(inv.getHours())).append(',')
                    .append(num(total)).append(',')
                    .append(inv.getStatus()).append("\r\n");
        }
        sb.append("\r\n,,,,,,Total invoiced,").append(num(invoiced)).append("\r\n");
        sb.append(",,,,,,Paid,").append(num(paid)).append("\r\n");
        sb.append(",,,,,,Pending,").append(num(invoiced - paid)).append("\r\n");
        write(parent, file, sb.toString());
    }

    // ------------------------------------------------------------------ helpers

    private static File chooseFile(Component parent, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save report");
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(parent,
                    file.getName() + " already exists. Replace it?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return file;
    }

    private static void write(Component parent, File file, String csvText) {
        try {
            // the leading byte-order mark makes Excel read accented characters correctly
            Files.write(file.toPath(), ("\uFEFF" + csvText).getBytes(StandardCharsets.UTF_8));
            Ui.toast(parent, "Report saved to " + file.getName());
        } catch (IOException e) {
            Ui.error(parent, "Could not save the report: " + e.getMessage());
        }
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Wraps a value in quotes when it contains a comma, quote or line break. */
    private static String csv(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
