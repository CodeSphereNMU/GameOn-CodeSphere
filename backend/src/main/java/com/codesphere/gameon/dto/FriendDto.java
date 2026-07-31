package com.codesphere.gameon.dto;

/**
 * Response DTO for a mutual friend.
 */
public class FriendDto {

    private long userId;
    private String username;

    public FriendDto() {
    }

    public FriendDto(long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
