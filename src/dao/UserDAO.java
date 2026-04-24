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

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
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
        String sql = "INSERT INTO users (name, email, password, role, profile_picture, bio) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getProfilePicture());
            ps.setString(6, u.getBio());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean update(User u) {
        String sql = "UPDATE users SET name=?, email=?, password=?, role=?, profile_picture=?, bio=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getProfilePicture());
            ps.setString(6, u.getBio());
            ps.setInt(7, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection()) {
            // Cascade: delete notifications
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE user_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            // Cascade: delete support tickets
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM support_tickets WHERE user_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            // Cascade: delete CVs
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM cvs WHERE user_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            // Delete user
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean updateProfilePicture(int userId, String path) {
        String sql = "UPDATE users SET profile_picture=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private User map(ResultSet rs) throws SQLException {
        String pic = null;
        try { pic = rs.getString("profile_picture"); } catch (SQLException ignored) {}
        String bio = null;
        try { bio = rs.getString("bio"); } catch (SQLException ignored) {}
        return new User(rs.getInt("id"), rs.getString("name"),
                rs.getString("email"), rs.getString("password"), rs.getString("role"), pic, bio);
    }
}
