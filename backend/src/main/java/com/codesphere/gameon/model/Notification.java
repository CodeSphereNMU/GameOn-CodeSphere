package com.codesphere.gameon.model;

/**
 * Domain model for the dbo.notification table.
 */
public class Notification {

    private long notificationId;
    private boolean isRead;
    private String text;
    private String typeOfNotification;
    private long recipientId;
    private Long gameListingId; // nullable

    public Notification() {
    }

    public Notification(long notificationId, boolean isRead, String text,
                        String typeOfNotification, long recipientId, Long gameListingId) {
        this.notificationId = notificationId;
        this.isRead = isRead;
        this.text = text;
        this.typeOfNotification = typeOfNotification;
        this.recipientId = recipientId;
        this.gameListingId = gameListingId;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTypeOfNotification() {
        return typeOfNotification;
    }

    public void setTypeOfNotification(String typeOfNotification) {
        this.typeOfNotification = typeOfNotification;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(Long gameListingId) {
        this.gameListingId = gameListingId;
    }
}
