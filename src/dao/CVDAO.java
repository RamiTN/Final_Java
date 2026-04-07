package dao;

import db.DatabaseConnection;
import model.Cv;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CVDAO {

    public boolean insert(Cv cv) {
        String sql = "INSERT INTO cvs (user_id,full_name,email,phone,address,objective,education,experience,skills,languages) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cv.getUserId());
            ps.setString(2, cv.getFullName());
            ps.setString(3, cv.getEmail());
            ps.setString(4, cv.getPhone());
            ps.setString(5, cv.getAddress());
            ps.setString(6, cv.getObjective());
            ps.setString(7, cv.getEducation());
            ps.setString(8, cv.getExperience());
            ps.setString(9, cv.getSkills());
            ps.setString(10, cv.getLanguages());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean update(Cv cv) {
        String sql = "UPDATE cvs SET full_name=?,email=?,phone=?,address=?,objective=?,education=?,experience=?,skills=?,languages=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cv.getFullName());
            ps.setString(2, cv.getEmail());
            ps.setString(3, cv.getPhone());
            ps.setString(4, cv.getAddress());
            ps.setString(5, cv.getObjective());
            ps.setString(6, cv.getEducation());
            ps.setString(7, cv.getExperience());
            ps.setString(8, cv.getSkills());
            ps.setString(9, cv.getLanguages());
            ps.setInt(10, cv.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM cvs WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Cv> findByUserId(int userId) {
        List<Cv> list = new ArrayList<>();
        String sql = "SELECT * FROM cvs WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Cv findById(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM cvs WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<Cv> findAll() {
        List<Cv> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM cvs")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Cv map(ResultSet rs) throws SQLException {
        return new Cv(rs.getInt("id"), rs.getInt("user_id"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("phone"), rs.getString("address"),
                rs.getString("objective"), rs.getString("education"), rs.getString("experience"),
                rs.getString("skills"), rs.getString("languages"));
    }
}
