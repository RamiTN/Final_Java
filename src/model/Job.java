package model;

public class Job {
    private int id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String requirements;
    private String status;        // "available" or "unavailable"
    private String usersApplied;  // comma-separated user IDs

    public Job() { this.status = "available"; }

    public Job(int id, String title, String company, String location,
               String description, String requirements, String status, String usersApplied) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.requirements = requirements;
        this.status = status != null ? status : "available";
        this.usersApplied = usersApplied;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUsersApplied() { return usersApplied; }
    public void setUsersApplied(String usersApplied) { this.usersApplied = usersApplied; }
    public boolean isAvailable() { return "available".equals(status); }

    // Keep backward compat — getLink/setLink map to nothing now
    public String getLink() { return usersApplied; }
    public void setLink(String link) { /* no-op, field removed */ }

    /** Check if a specific user has applied */
    public boolean hasUserApplied(int userId) {
        if (usersApplied == null || usersApplied.isEmpty()) return false;
        for (String id : usersApplied.split(",")) {
            if (id.trim().equals(String.valueOf(userId))) return true;
        }
        return false;
    }

    @Override
    public String toString() { return title + " at " + company; }
}
