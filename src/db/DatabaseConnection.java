package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:cvcreator.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "email TEXT UNIQUE NOT NULL,"
                    + "password TEXT NOT NULL,"
                    + "role TEXT DEFAULT 'user')");

            stmt.execute("CREATE TABLE IF NOT EXISTS cvs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "full_name TEXT,"
                    + "email TEXT,"
                    + "phone TEXT,"
                    + "address TEXT,"
                    + "objective TEXT,"
                    + "education TEXT,"
                    + "experience TEXT,"
                    + "skills TEXT,"
                    + "languages TEXT,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS jobs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "company TEXT,"
                    + "location TEXT,"
                    + "description TEXT,"
                    + "requirements TEXT,"
                    + "link TEXT)");

            // insert default admin if not exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE email='admin@admin.com'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (name, email, password, role) "
                        + "VALUES ('Admin', 'admin@admin.com', 'admin123', 'admin')");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
