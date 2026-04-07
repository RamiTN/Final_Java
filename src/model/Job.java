package model;

public class Job {
    private int id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String requirements;
    private String link;

    public Job() {}

    public Job(int id, String title, String company, String location,
               String description, String requirements, String link) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.requirements = requirements;
        this.link = link;
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
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    @Override
    public String toString() { return title + " at " + company; }
}
