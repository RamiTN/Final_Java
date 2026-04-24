package dao;

import db.DatabaseConnection;
import model.Notification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public boolean insert(Notification n) {
        String sql = "INSERT INTO notifications (user_id,admin_id,job_id,cv_id,type,message) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, n.getUserId());
            ps.setInt(2, n.getAdminId());
            ps.setInt(3, n.getJobId());
            ps.setInt(4, n.getCvId());
            ps.setString(5, n.getType());
            ps.setString(6, n.getMessage());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /** Notifications for a specific user (accepted/rejected responses) */
    public List<Notification> findByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT n.*, j.title AS job_title, c.full_name AS cv_name "
                + "FROM notifications n "
                + "LEFT JOIN jobs j ON n.job_id = j.id "
                + "LEFT JOIN cvs c ON n.cv_id = c.id "
                + "WHERE n.user_id=? AND n.type IN ('accepted','rejected') "
                + "ORDER BY n.created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapWithExtras(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Pending applications submitted by a specific user */
    public List<Notification> findApplicationsByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT n.*, j.title AS job_title, c.full_name AS cv_name "
                + "FROM notifications n "
                + "LEFT JOIN jobs j ON n.job_id = j.id "
                + "LEFT JOIN cvs c ON n.cv_id = c.id "
                + "WHERE n.user_id=? AND n.type='application' "
                + "ORDER BY n.created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapWithExtras(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** All pending applications for admin to review */
    public List<Notification> findApplicationsForAdmin() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT n.*, u.name AS user_name, j.title AS job_title, c.full_name AS cv_name "
                + "FROM notifications n "
                + "LEFT JOIN users u ON n.user_id = u.id "
                + "LEFT JOIN jobs j ON n.job_id = j.id "
                + "LEFT JOIN cvs c ON n.cv_id = c.id "
                + "WHERE n.type='application' "
                + "ORDER BY n.created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Notification n = mapWithExtras(rs);
                try { n.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
                list.add(n);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean markAsRead(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE notifications SET is_read=1 WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /** Admin responds: deletes original application, creates response notification for the user */
    public boolean respond(int applicationId, int adminId, String newType, String responseMessage,
                           int userId, int jobId, int cvId) {
        try (Connection c = DatabaseConnection.getConnection()) {
            // Delete original application
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE id=?")) {
                ps.setInt(1, applicationId);
                ps.executeUpdate();
            }
            // Create response notification for the user
            String sql = "INSERT INTO notifications (user_id,admin_id,job_id,cv_id,type,message) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, adminId);
                ps.setInt(3, jobId);
                ps.setInt(4, cvId);
                ps.setString(5, newType);
                ps.setString(6, responseMessage);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /** Delete application by user and job (for cancel) */
    public boolean deleteByUserAndJob(int userId, int jobId) {
        String sql = "DELETE FROM notifications WHERE user_id=? AND job_id=? AND type='application'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public void deleteByUserId(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE user_id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int countUnreadForUser(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0 AND type IN ('accepted','rejected')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int countPendingApplications() {
        String sql = "SELECT COUNT(*) FROM notifications WHERE type='application'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Check if user already has a pending application for a job */
    public boolean hasApplied(int userId, int jobId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id=? AND job_id=? AND type='application'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, jobId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Notification mapWithExtras(ResultSet rs) throws SQLException {
        Notification n = new Notification(
                rs.getInt("id"), rs.getInt("user_id"), rs.getInt("admin_id"),
                rs.getInt("job_id"), rs.getInt("cv_id"), rs.getString("type"),
                rs.getString("message"), rs.getInt("is_read") == 1, rs.getString("created_at")
        );
        try { n.setJobTitle(rs.getString("job_title")); } catch (SQLException ignored) {}
        try { n.setCvName(rs.getString("cv_name")); } catch (SQLException ignored) {}
        return n;
    }
}
