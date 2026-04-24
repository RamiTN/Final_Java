package dao;

import db.DatabaseConnection;
import model.SupportTicket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupportTicketDAO {

    public boolean insert(SupportTicket t) {
        String sql = "INSERT INTO support_tickets (user_id, subject, message) VALUES (?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getSubject());
            ps.setString(3, t.getMessage());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<SupportTicket> findAll() {
        List<SupportTicket> list = new ArrayList<>();
        String sql = "SELECT t.*, u.name AS user_name FROM support_tickets t "
                + "LEFT JOIN users u ON t.user_id = u.id "
                + "ORDER BY CASE WHEN t.status='open' THEN 0 ELSE 1 END, t.created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                SupportTicket t = map(rs);
                try { t.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
                list.add(t);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<SupportTicket> findByUserId(int userId) {
        List<SupportTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM support_tickets WHERE user_id=? ORDER BY created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean respond(int id, String response) {
        String sql = "UPDATE support_tickets SET admin_response=?, status='closed' WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, response);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int countOpen() {
        String sql = "SELECT COUNT(*) FROM support_tickets WHERE status='open'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private SupportTicket map(ResultSet rs) throws SQLException {
        return new SupportTicket(
                rs.getInt("id"), rs.getInt("user_id"),
                rs.getString("subject"), rs.getString("message"),
                rs.getString("status"), rs.getString("admin_response"),
                rs.getString("created_at")
        );
    }
}
