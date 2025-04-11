package edu.msu.wilki385.housekeep.collections;

public class User {
    private String id;
    private String name;
    private String email;

    // Constructor(s)
    public User() {
        // Empty constructor needed if using certain serialization libraries or Firebase
    }

    public User(String uid, String name, String email) {
        this.id = uid;  // Automatically generate a UUID
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
