package dao;

import db.DatabaseConnection;
import model.Job;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobOfferDAO {

    public boolean insert(Job j) {
        String sql = "INSERT INTO jobs (title,company,location,description,requirements,status,users_applied) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getTitle());
            ps.setString(2, j.getCompany());
            ps.setString(3, j.getLocation());
            ps.setString(4, j.getDescription());
            ps.setString(5, j.getRequirements());
            ps.setString(6, j.getStatus() != null ? j.getStatus() : "available");
            ps.setString(7, j.getUsersApplied());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean update(Job j) {
        String sql = "UPDATE jobs SET title=?,company=?,location=?,description=?,requirements=?,status=?,users_applied=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getTitle());
            ps.setString(2, j.getCompany());
            ps.setString(3, j.getLocation());
            ps.setString(4, j.getDescription());
            ps.setString(5, j.getRequirements());
            ps.setString(6, j.getStatus());
            ps.setString(7, j.getUsersApplied());
            ps.setInt(8, j.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM jobs WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Job> findAll() {
        List<Job> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM jobs")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Job> findAvailable() {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE status='available' OR status IS NULL";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Job> findByStatus(String status) {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE status=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Job findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM jobs WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean updateStatus(int jobId, String status) {
        String sql = "UPDATE jobs SET status=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean addAppliedUser(int jobId, int userId) {
        Job job = findById(jobId);
        if (job == null) return false;
        String current = job.getUsersApplied();
        String uid = String.valueOf(userId);
        if (current == null || current.isEmpty()) {
            current = uid;
        } else {
            // Check if already applied
            for (String s : current.split(",")) {
                if (s.trim().equals(uid)) return true; // already applied
            }
            current = current + "," + uid;
        }
        String sql = "UPDATE jobs SET users_applied=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, current);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean removeAppliedUser(int jobId, int userId) {
        Job job = findById(jobId);
        if (job == null) return false;
        String current = job.getUsersApplied();
        if (current == null || current.isEmpty()) return true;
        String uid = String.valueOf(userId);
        StringBuilder sb = new StringBuilder();
        for (String s : current.split(",")) {
            if (!s.trim().equals(uid)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(s.trim());
            }
        }
        String sql = "UPDATE jobs SET users_applied=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sb.length() > 0 ? sb.toString() : null);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Job map(ResultSet rs) throws SQLException {
        String status = "available";
        try { status = rs.getString("status"); } catch (SQLException ignored) {}
        if (status == null) status = "available";
        String usersApplied = null;
        try { usersApplied = rs.getString("users_applied"); } catch (SQLException ignored) {}
        return new Job(rs.getInt("id"), rs.getString("title"), rs.getString("company"),
                rs.getString("location"), rs.getString("description"),
                rs.getString("requirements"), status, usersApplied);
    }
}
