package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC connection manager. Uses an embedded H2 database stored in a file next to the program.
 *
 * To switch to MySQL, change the three constants below, e.g.:
 *   URL      = "jdbc:mysql://localhost:3306/freelancer_db"
 *   USER     = "root"
 *   PASSWORD = "your_password"
 * ...and put the MySQL connector jar in lib/ instead of the H2 jar.
 */
public final class DBConnection {

    private static final String URL = "jdbc:h2:./freelancer_db;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private DBConnection() {
    }

    public static Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    /** Creates all tables if they do not exist yet. Called once at startup. */
    public static void initSchema() throws DatabaseException {
        String[] statements = {
                "CREATE TABLE IF NOT EXISTS users ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "name VARCHAR(100) NOT NULL, "
                        + "email VARCHAR(100) NOT NULL UNIQUE, "
                        + "role VARCHAR(20) NOT NULL, "
                        + "password_hash VARCHAR(200) NOT NULL)",

                "CREATE TABLE IF NOT EXISTS projects ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "name VARCHAR(150) NOT NULL, "
                        + "description VARCHAR(500), "
                        + "deadline DATE NOT NULL, "
                        + "status VARCHAR(20) NOT NULL, "
                        + "freelancer_id INT NOT NULL, "
                        + "client_id INT NOT NULL, "
                        + "FOREIGN KEY (freelancer_id) REFERENCES users(id), "
                        + "FOREIGN KEY (client_id) REFERENCES users(id))",

                "CREATE TABLE IF NOT EXISTS time_entries ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "project_id INT NOT NULL, "
                        + "start_time TIMESTAMP NOT NULL, "
                        + "end_time TIMESTAMP NOT NULL, "
                        + "FOREIGN KEY (project_id) REFERENCES projects(id))",

                "CREATE TABLE IF NOT EXISTS invoices ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "project_id INT NOT NULL, "
                        + "freelancer_id INT NOT NULL, "
                        + "client_id INT NOT NULL, "
                        + "invoice_type VARCHAR(10) NOT NULL, "
                        + "hours DOUBLE NOT NULL, "
                        + "rate DOUBLE NOT NULL, "
                        + "status VARCHAR(10) NOT NULL, "
                        + "created_date DATE NOT NULL, "
                        + "FOREIGN KEY (project_id) REFERENCES projects(id), "
                        + "FOREIGN KEY (freelancer_id) REFERENCES users(id), "
                        + "FOREIGN KEY (client_id) REFERENCES users(id))",

                "CREATE TABLE IF NOT EXISTS messages ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "sender_id INT NOT NULL, "
                        + "receiver_id INT NOT NULL, "
                        + "content VARCHAR(1000) NOT NULL, "
                        + "sent_at TIMESTAMP NOT NULL, "
                        + "read_at TIMESTAMP NULL, "
                        + "FOREIGN KEY (sender_id) REFERENCES users(id), "
                        + "FOREIGN KEY (receiver_id) REFERENCES users(id))"
        };

        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
            // databases created before read receipts existed need the extra column
            try {
                st.execute("ALTER TABLE messages ADD COLUMN read_at TIMESTAMP NULL");
            } catch (SQLException alreadyThere) {
                // the column already exists: nothing to do
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not create database tables: " + e.getMessage(), e);
        }
    }
}
