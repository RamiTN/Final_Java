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

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "admin_id INTEGER,"
                    + "job_id INTEGER NOT NULL,"
                    + "cv_id INTEGER NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "message TEXT,"
                    + "is_read INTEGER DEFAULT 0,"
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id),"
                    + "FOREIGN KEY (job_id) REFERENCES jobs(id),"
                    + "FOREIGN KEY (cv_id) REFERENCES cvs(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS support_tickets ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "subject TEXT NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "status TEXT DEFAULT 'open',"
                    + "admin_response TEXT,"
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Safe migrations — add columns if missing
            safeAlter(stmt, "ALTER TABLE users ADD COLUMN profile_picture TEXT");
            safeAlter(stmt, "ALTER TABLE users ADD COLUMN bio TEXT");
            safeAlter(stmt, "ALTER TABLE jobs ADD COLUMN status TEXT DEFAULT 'available'");
            safeAlter(stmt, "ALTER TABLE jobs ADD COLUMN users_applied TEXT");

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

    private static void safeAlter(Statement stmt, String sql) {
        try { stmt.execute(sql); } catch (SQLException ignored) {}
    }
}
