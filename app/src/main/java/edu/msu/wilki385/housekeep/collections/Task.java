package edu.msu.wilki385.housekeep.collections;

public class Task {
    private String name;
    private String description;
    private String photo;  // Could be a URL, file path, or another type

    // Empty constructor
    public Task() { }

    // Constructor for convenience
    public Task(String name, String description, String photo) {
        this.name = name;
        this.description = description;
        this.photo = photo;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
