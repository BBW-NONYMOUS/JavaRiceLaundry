package laundry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;

/**
 * MySQL connection settings. The defaults match a fresh XAMPP install: MySQL on localhost:3306,
 * user {@code root}, no password. Change them here if your MySQL is set up differently.
 */
public final class Database {

    public static final String HOST = System.getProperty("laundry.db.host", "localhost:3306");
    public static final String NAME = System.getProperty("laundry.db.name", "metric_laundry");
    public static final String USER = System.getProperty("laundry.db.user", "root");
    public static final String PASSWORD = System.getProperty("laundry.db.password", "");

    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static boolean ready;

    private Database() {
    }

    /** A new connection to the system's database. Close it after use (try-with-resources). */
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + HOST + "/" + NAME + OPTIONS, USER, PASSWORD);
    }

    /**
     * Called when the app starts: connects to MySQL and creates the database and tables from
     * {@code database.sql} if they don't exist yet. Shows a message and returns false if MySQL is not reachable.
     */
    public static boolean start() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> showError(error));
        try {
            setup();
            return true;
        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the MySQL database at " + HOST + ".\n\n"
                    + "Open the XAMPP Control Panel, click Start next to MySQL, then run the app again.\n\n"
                    + "Details: " + e.getMessage(),
                    "Metric Laundry - Database", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Runs database.sql (it only creates what is missing). */
    public static synchronized void setup() throws SQLException, IOException {
        if (ready) {
            return;
        }
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/laundry/database.sql")) {
            if (in == null) {
                throw new IOException("database.sql was not found next to the classes. Clean and Build the project.");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("metric_laundry", NAME);
        }
        try (Connection server = DriverManager.getConnection("jdbc:mysql://" + HOST + "/" + OPTIONS, USER, PASSWORD);
                Statement statement = server.createStatement()) {
            for (String sql : statements(script)) {
                statement.execute(sql);
            }
        }
        ready = true;
    }

    /** Splits the script into statements, dropping "--" comment lines. */
    private static String[] statements(String script) {
        StringBuilder clean = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                clean.append(line).append('\n');
            }
        }
        return java.util.Arrays.stream(clean.toString().split(";\\s*\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private static void showError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && !(cause instanceof DataException)) {
            cause = cause.getCause();
        }
        error.printStackTrace();
        String message = cause instanceof DataException
                ? cause.getMessage() + "\n\nMake sure MySQL is running in the XAMPP Control Panel."
                : "Something went wrong: " + error;
        JOptionPane.showMessageDialog(null, message, "Metric Laundry", JOptionPane.ERROR_MESSAGE);
    }

    /** A database problem, shown to the user as a message. */
    public static class DataException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
