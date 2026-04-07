package service;

import dao.UserDAO;
import model.User;

public class AuthService {
    private static User currentUser = null;
    private UserDAO userDAO = new UserDAO();

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }

    public User login(String email, String password) {
        User user = userDAO.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            return user;
        }
        return null;
    }

    public boolean register(String name, String email, String password) {
        if (userDAO.findByEmail(email) != null) {
            return false;
        }
        User user = new User(0, name, email, password, "user");
        return userDAO.insert(user);
    }
}
