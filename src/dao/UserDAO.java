package dao;

import db.DatabaseConnection;
import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean insert(User u) {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getRole());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean update(User u) {
        String sql = "UPDATE users SET name=?, email=?, password=?, role=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getRole());
            ps.setInt(5, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("name"),
                rs.getString("email"), rs.getString("password"), rs.getString("role"));
    }
}
