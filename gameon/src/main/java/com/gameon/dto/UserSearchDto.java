package com.gameon.dto;

/**
 * DTO for user search results returned via the AJAX search endpoint.
 */
public class UserSearchDto {

    private Long userId;
    private String username;
    private String displayName;
    private String profilePictureUrl;

    public UserSearchDto() {
    }

    public UserSearchDto(Long userId, String username, String displayName, String profilePictureUrl) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.profilePictureUrl = profilePictureUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
