package com.gameon.model.entity;

import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity - Core identity table for all authenticated users.
 * Maps to 'users' table in GameOnDb.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_of_user", nullable = false, length = 20)
    private UserRole typeOfUser = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * Number of moderation strikes (warnings) accrued by this user. Every user always has a
     * concrete count starting at 0. When it reaches {@link #STRIKES_FOR_BAN}, the account is
     * banned. Reinstating a user resets this to 0.
     */
    @Column(name = "warning_count", nullable = false)
    private int warningCount = 0;

    /** Timestamp the account was banned, or {@code null} if the user is not banned. */
    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    /** Id of the moderator/administrator who banned this user, or {@code null} if not banned. */
    @Column(name = "banned_by_user_id")
    private Long bannedByUserId;

    /** Reason the account was banned, or {@code null} if not banned. */
    @Column(name = "ban_reason", length = 300)
    private String banReason;

    /** Human-readable label of the most recent moderation action taken on this user. */
    @Column(name = "last_moderation_action", length = 100)
    private String lastModerationAction;

    /** Timestamp of the most recent moderation action taken on this user. */
    @Column(name = "last_moderation_at")
    private LocalDateTime lastModerationAt;

    /** Number of strikes that results in an automatic ban. */
    @Transient
    public static final int STRIKES_FOR_BAN = 3;

    /**
     * Public URL path of the user's uploaded profile picture
     * (e.g. {@code /uploads/profile-pictures/<uuid>.webp}), or {@code null} when the user
     * has not uploaded one. Only the path/URL is stored here, never the image binary.
     */
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    // ===== Relationships =====

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSportProfile> sportProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private List<GameListing> createdListings = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<GameJoiner> joinedGames = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "follower", fetch = FetchType.LAZY)
    private List<Follow> following = new ArrayList<>();

    @OneToMany(mappedBy = "followed", fetch = FetchType.LAZY)
    private List<Follow> followers = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private List<Report> reports = new ArrayList<>();

    // ===== Constructors =====

    public User() {
    }

    public User(String username, String password, UserRole typeOfUser) {
        this.username = username;
        this.password = password;
        this.typeOfUser = typeOfUser;
    }

    // ===== Getters and Setters =====

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getTypeOfUser() {
        return typeOfUser;
    }

    public void setTypeOfUser(UserRole typeOfUser) {
        this.typeOfUser = typeOfUser;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public LocalDateTime getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(LocalDateTime bannedAt) {
        this.bannedAt = bannedAt;
    }

    public Long getBannedByUserId() {
        return bannedByUserId;
    }

    public void setBannedByUserId(Long bannedByUserId) {
        this.bannedByUserId = bannedByUserId;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public String getLastModerationAction() {
        return lastModerationAction;
    }

    public void setLastModerationAction(String lastModerationAction) {
        this.lastModerationAction = lastModerationAction;
    }

    public LocalDateTime getLastModerationAt() {
        return lastModerationAt;
    }

    public void setLastModerationAt(LocalDateTime lastModerationAt) {
        this.lastModerationAt = lastModerationAt;
    }

    /** Convenience: true when this user has reached the ban threshold or is banned. */
    @Transient
    public boolean isBanned() {
        return accountStatus == AccountStatus.BANNED;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public List<UserSportProfile> getSportProfiles() {
        return sportProfiles;
    }

    public void setSportProfiles(List<UserSportProfile> sportProfiles) {
        this.sportProfiles = sportProfiles;
    }

    public List<GameListing> getCreatedListings() {
        return createdListings;
    }

    public void setCreatedListings(List<GameListing> createdListings) {
        this.createdListings = createdListings;
    }

    public List<GameJoiner> getJoinedGames() {
        return joinedGames;
    }

    public void setJoinedGames(List<GameJoiner> joinedGames) {
        this.joinedGames = joinedGames;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public List<Follow> getFollowing() {
        return following;
    }

    public void setFollowing(List<Follow> following) {
        this.following = following;
    }

    public List<Follow> getFollowers() {
        return followers;
    }

    public void setFollowers(List<Follow> followers) {
        this.followers = followers;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }
}
