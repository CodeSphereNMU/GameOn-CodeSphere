package com.codesphere.gameon.model;

/**
 * Domain model for the dbo.users table.
 * Matches the existing schema: user_id, username, password, type_of_user.
 */
public class User {

    private long userId;
    private String username;
    private String password;
    private String typeOfUser;

    public User() {
    }

    public User(long userId, String username, String password, String typeOfUser) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.typeOfUser = typeOfUser;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTypeOfUser() {
        return typeOfUser;
    }

    public void setTypeOfUser(String typeOfUser) {
        this.typeOfUser = typeOfUser;
    }
}
