package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Block entity - a user (blocker) blocks another user (blocked).
 *
 * <p>Storage is directional: (blockerUserId, blockedUserId). However, blocking is treated
 * as a <b>mutual</b> visibility and interaction restriction: if either user has blocked the
 * other, both are prevented from seeing or interacting with each other. That mutual check is
 * centralised in {@code BlockService.isBlockedEitherWay(a, b)}.</p>
 *
 * <p>Composite PK: (blockerUserId, blockedUserId). Self-referencing many-to-many on User,
 * modelled exactly like {@link Follow}.</p>
 */
@Entity
@Table(name = "blocks")
public class Block {

    @EmbeddedId
    private BlockId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("blockerUserId")
    @JoinColumn(name = "blocker_user_id")
    private User blocker;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("blockedUserId")
    @JoinColumn(name = "blocked_user_id")
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public Block() {
    }

    public Block(User blocker, User blocked) {
        this.id = new BlockId(blocker.getUserId(), blocked.getUserId());
        this.blocker = blocker;
        this.blocked = blocked;
    }

    // ===== Getters and Setters =====

    public BlockId getId() {
        return id;
    }

    public void setId(BlockId id) {
        this.id = id;
    }

    public User getBlocker() {
        return blocker;
    }

    public void setBlocker(User blocker) {
        this.blocker = blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    public void setBlocked(User blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
