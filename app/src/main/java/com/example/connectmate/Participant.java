package com.example.connectmate;

public class Participant {
    private final String id;
    private final String name;
    private final boolean host;
    private final String profileImageUrl;

    public Participant(String id, String name) {
        this(id, name, false, null);
    }

    public Participant(String id, String name, boolean host) {
        this(id, name, host, null);
    }

    public Participant(String id, String name, boolean host, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.profileImageUrl = profileImageUrl;
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}
