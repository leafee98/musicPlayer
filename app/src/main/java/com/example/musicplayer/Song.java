package com.example.musicplayer;

import java.util.Locale;

class Song {
    private Long id;
    private String path;
    private String title;
    private Long duration;
    private String artist;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPath() { return path; }
    void setPath(String path) { this.path = path; }

    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }

    Long getDuration() { return duration; }
    void setDuration(Long duration) { this.duration = duration; }
    String getDurationStr() {
        long dur = this.duration / 1000;
        long sec = dur % 60;
        long min = dur / 60;
        return String.format(Locale.ENGLISH, "%02d:%02d", min, sec);
    }

    String getArtist() { return artist; }
    void setArtist(String artist) { this.artist = artist; }
}
