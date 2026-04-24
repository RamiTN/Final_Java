package model;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String role;
    private String profilePicture;
    private String bio;

    public User() {}

    public User(int id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User(int id, String name, String email, String password, String role, String profilePicture) {
        this(id, name, email, password, role);
        this.profilePicture = profilePicture;
    }

    public User(int id, String name, String email, String password, String role, String profilePicture, String bio) {
        this(id, name, email, password, role, profilePicture);
        this.bio = bio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public boolean isAdmin() { return "admin".equals(role); }

    @Override
    public String toString() { return name + " (" + email + ")"; }
}
