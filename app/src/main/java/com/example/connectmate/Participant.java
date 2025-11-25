package com.example.connectmate;

public class Participant {
    private final String id;
    private final String name;
    private final boolean host;

    public Participant(String id, String name) {
        this(id, name, false);
    }

    public Participant(String id, String name, boolean host) {
        this.id = id;
        this.name = name;
        this.host = host;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isHost() {
        return host;
    }
}
