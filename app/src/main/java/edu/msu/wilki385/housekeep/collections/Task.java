package edu.msu.wilki385.housekeep.collections;

public class Task {
    private String id;
    private String name;
    private String description;
    private String photo;  // Could be a URL, file path, or another type

    // Empty constructor needed by Firestore
    public Task() { }

    // Constructor using id and name (description and photo remain null)
    public Task(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
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
