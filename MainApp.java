package app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Program entry point. Run with:  java -cp ".;lib/*" app.MainApp */
public class MainApp {

    public static void main(String[] args) {
        Theme.install();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> SwingUtilities.invokeLater(() -> {
            String message = error instanceof AccessDeniedException
                    ? error.getMessage()
                    : "Something went wrong: " + error;
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        }));
        try {
            DBConnection.initSchema();
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(null,
                    "Database error:\n" + e.getMessage()
                            + "\n\nCheck that the H2 jar is inside the lib folder and is on the classpath.",
                    "Startup failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
