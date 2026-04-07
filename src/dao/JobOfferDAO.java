package dao;

import db.DatabaseConnection;
import model.Job;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobOfferDAO {

    public boolean insert(Job j) {
        String sql = "INSERT INTO jobs (title,company,location,description,requirements,link) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getTitle());
            ps.setString(2, j.getCompany());
            ps.setString(3, j.getLocation());
            ps.setString(4, j.getDescription());
            ps.setString(5, j.getRequirements());
            ps.setString(6, j.getLink());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean update(Job j) {
        String sql = "UPDATE jobs SET title=?,company=?,location=?,description=?,requirements=?,link=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getTitle());
            ps.setString(2, j.getCompany());
            ps.setString(3, j.getLocation());
            ps.setString(4, j.getDescription());
            ps.setString(5, j.getRequirements());
            ps.setString(6, j.getLink());
            ps.setInt(7, j.getId());
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

    public Job findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM jobs WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private Job map(ResultSet rs) throws SQLException {
        return new Job(rs.getInt("id"), rs.getString("title"), rs.getString("company"),
                rs.getString("location"), rs.getString("description"),
                rs.getString("requirements"), rs.getString("link"));
    }
}
