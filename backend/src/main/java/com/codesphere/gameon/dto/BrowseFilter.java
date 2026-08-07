package com.codesphere.gameon.dto;

import java.time.LocalDate;

/**
 * Value object holding optional filter and pagination parameters
 * for the browse listings endpoint.
 */
public class BrowseFilter {

    private int page = 1;
    private int size = 20;
    private Long sportId;
    private String skillLevel;
    private LocalDate date;
    private boolean hideFull;

    public BrowseFilter() {
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Long getSportId() {
        return sportId;
    }

    public void setSportId(Long sportId) {
        this.sportId = sportId;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isHideFull() {
        return hideFull;
    }

    public void setHideFull(boolean hideFull) {
        this.hideFull = hideFull;
    }
}
