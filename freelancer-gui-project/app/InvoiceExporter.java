package app;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/** Saves one invoice as a styled PDF or as a plain-text receipt. */
public final class InvoiceExporter {

    private InvoiceExporter() {
    }

    public static void export(Component parent, Invoice invoice) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save invoice");
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF document (*.pdf)", "pdf");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text receipt (*.txt)", "txt");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(pdfFilter);
        chooser.addChoosableFileFilter(txtFilter);
        chooser.setFileFilter(pdfFilter);
        chooser.setSelectedFile(new File("invoice-" + invoice.getId() + ".pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        String lower = file.getName().toLowerCase();
        boolean asText;
        if (lower.endsWith(".txt")) {
            asText = true;
        } else if (lower.endsWith(".pdf")) {
            asText = false;
        } else {
            asText = chooser.getFileFilter() == txtFilter;
            file = new File(file.getParentFile(), file.getName() + (asText ? ".txt" : ".pdf"));
        }

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(parent,
                    file.getName() + " already exists. Replace it?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            byte[] data = asText
                    ? buildReceipt(invoice).getBytes(StandardCharsets.UTF_8)
                    : buildPdf(invoice);
            Files.write(file.toPath(), data);
            Ui.toast(parent, "Invoice saved to " + file.getName());
        } catch (IOException e) {
            Ui.error(parent, "Could not save the invoice: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ PDF

    private static byte[] buildPdf(Invoice inv) {
        SimplePdf pdf = new SimplePdf();
        boolean fixed = "FIXED".equals(inv.getType());
        boolean paid = inv.getStatus() == Invoice.Status.PAID;

        // header band
        pdf.fillRect(0, 0, 595, 120, 2, 132, 199);
        pdf.text("INVOICE", 44, 68, 32, true, 255, 255, 255);
        pdf.text("#" + inv.getId(), 44, 94, 14, false, 224, 242, 254);
        pdf.textRight("Date: " + inv.getCreatedDate(), 551, 68, 12, false, 255, 255, 255);

        // parties
        pdf.text("FROM", 44, 165, 9, true, 100, 116, 139);
        pdf.text(SimplePdf.fit(inv.getFreelancerName(), 14, true, 250), 44, 185, 14, true, 15, 23, 42);
        pdf.text("BILLED TO", 320, 165, 9, true, 100, 116, 139);
        pdf.text(SimplePdf.fit(inv.getClientName(), 14, true, 230), 320, 185, 14, true, 15, 23, 42);

        pdf.text("PROJECT", 44, 225, 9, true, 100, 116, 139);
        pdf.text(SimplePdf.fit(inv.getProjectName(), 14, false, 250), 44, 245, 14, false, 15, 23, 42);

        // status chip
        if (paid) {
            pdf.fillRect(320, 222, 90, 28, 209, 250, 229);
            pdf.text("PAID", 346, 241, 11, true, 5, 150, 105);
        } else {
            pdf.fillRect(320, 222, 90, 28, 254, 243, 199);
            pdf.text("PENDING", 334, 241, 11, true, 180, 83, 9);
        }

        // table header
        pdf.fillRect(44, 290, 507, 30, 241, 245, 249);
        pdf.text("DESCRIPTION", 56, 309, 9, true, 100, 116, 139);
        pdf.textRight("HOURS", 350, 309, 9, true, 100, 116, 139);
        pdf.textRight("RATE", 440, 309, 9, true, 100, 116, 139);
        pdf.textRight("AMOUNT", 539, 309, 9, true, 100, 116, 139);

        // table row
        pdf.text(fixed ? "Fixed-price work" : "Hourly work", 56, 346, 12, true, 15, 23, 42);
        pdf.text(SimplePdf.fit(inv.getProjectName(), 10, false, 200), 56, 362, 10, false, 100, 116, 139);
        pdf.textRight(money(inv.getHours()), 350, 346, 12, false, 15, 23, 42);
        pdf.textRight(fixed ? "-" : money(inv.getRate()), 440, 346, 12, false, 15, 23, 42);
        pdf.textRight(money(inv.calculateTotal()), 539, 346, 12, true, 15, 23, 42);
        pdf.line(44, 382, 551, 382, 0.8, 226, 232, 240);

        // total box
        pdf.fillRect(331, 404, 220, 76, 10, 25, 47);
        pdf.text("TOTAL DUE", 347, 430, 10, true, 186, 230, 253);
        pdf.textRight(money(inv.calculateTotal()), 535, 464, 24, true, 255, 255, 255);

        pdf.text("Thank you for your business.", 44, 440, 12, false, 100, 116, 139);

        // footer
        pdf.line(44, 790, 551, 790, 0.8, 226, 232, 240);
        pdf.text("Generated by Time Management for Freelancers", 44, 808, 9, false, 100, 116, 139);
        return pdf.toBytes();
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // ------------------------------------------------------------------ text receipt

    private static String buildReceipt(Invoice inv) {
        String line = "=========================================";
        String thin = "-----------------------------------------";
        String[] lines = {
                line,
                "INVOICE #" + inv.getId(),
                line,
                "Date:            " + inv.getCreatedDate(),
                "From:            " + inv.getFreelancerName(),
                "Billed to:       " + inv.getClientName(),
                "Project:         " + inv.getProjectName(),
                "Billing type:    " + inv.getType(),
                "Hours logged:    " + money(inv.getHours()),
                "Rate / amount:   " + money(inv.getRate()),
                thin,
                "TOTAL DUE:       " + money(inv.calculateTotal()),
                "Status:          " + inv.getStatus(),
                line,
                "Generated by Time Management for Freelancers"
        };
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }
}
