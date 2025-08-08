package ru.mice333.todo_bot.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Task {

    private String title;
    private String description;
    private String priority;
    private String createdAt;
    private String username;
    private String completed;

    public Task(String title, String description, String priority, String createdAt, String username, String completed) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdAt = createdAt;
        this.username = username;
        this.completed = completed;
    }

    public Task() {

    }
}
