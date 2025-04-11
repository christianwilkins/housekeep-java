package edu.msu.wilki385.housekeep.collections;

import java.util.List;
public class House {
    private String id;
    private String userId;
    private String name;
    private List<Task> tasks;

    // Empty constructor
    public House() { }

    // Constructor
    public House(String hid, String name, String userId, List<Task> tasks) {
        this.id = hid;
        this.userId = userId;
        this.name = name;
        this.tasks = tasks;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
