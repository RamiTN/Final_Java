package model;

public class Cv {
    private int id;
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String objective;
    private String education;
    private String experience;
    private String skills;
    private String languages;

    public Cv() {}

    public Cv(int id, int userId, String fullName, String email, String phone, String address,
              String objective, String education, String experience, String skills, String languages) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.objective = objective;
        this.education = education;
        this.experience = experience;
        this.skills = skills;
        this.languages = languages;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    @Override
    public String toString() { return fullName != null ? fullName : "CV #" + id; }
}
