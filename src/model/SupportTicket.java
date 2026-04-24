package model;

public class SupportTicket {
    private int id;
    private int userId;
    private String subject;
    private String message;
    private String status;         // "open" or "closed"
    private String adminResponse;
    private String createdAt;

    // Display field (from JOIN)
    private String userName;

    public SupportTicket() { this.status = "open"; }

    public SupportTicket(int id, int userId, String subject, String message,
                         String status, String adminResponse, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.adminResponse = adminResponse;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public boolean isOpen() { return "open".equals(status); }

    @Override
    public String toString() { return subject + " (" + status + ")"; }
}
