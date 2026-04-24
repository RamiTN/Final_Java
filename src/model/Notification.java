package model;

public class Notification {
    private int id;
    private int userId;
    private int adminId;
    private int jobId;
    private int cvId;
    private String type;      // "application", "accepted", "rejected"
    private String message;
    private boolean isRead;
    private String createdAt;

    // Extra display fields (not stored, populated by DAO joins)
    private String userName;
    private String jobTitle;
    private String cvName;

    public Notification() {}

    public Notification(int id, int userId, int adminId, int jobId, int cvId,
                        String type, String message, boolean isRead, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.adminId = adminId;
        this.jobId = jobId;
        this.cvId = cvId;
        this.type = type;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }
    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }
    public int getCvId() { return cvId; }
    public void setCvId(int cvId) { this.cvId = cvId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCvName() { return cvName; }
    public void setCvName(String cvName) { this.cvName = cvName; }

    @Override
    public String toString() {
        return type + " — " + (message != null ? message : "");
    }
}
