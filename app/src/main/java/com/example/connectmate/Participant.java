package com.example.connectmate;

public class Participant {
    private String id;
    private String name;

    public Participant(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
