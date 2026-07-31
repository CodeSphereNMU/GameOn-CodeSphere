package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * MatchResult entity - Final score of a completed game.
 * Maps to 'match_results' table in GameOnDb.
 * One-to-one relationship with GameListing (unique FK constraint).
 */
@Entity
@Table(name = "match_results")
public class MatchResult extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_result_id")
    private Long matchResultId;

    @NotNull(message = "Team A score is required")
    @Min(value = 0, message = "Team A score must be 0 or higher")
    @Column(name = "team_a_score", nullable = false)
    private Integer teamAScore;

    @NotNull(message = "Team B score is required")
    @Min(value = 0, message = "Team B score must be 0 or higher")
    @Column(name = "team_b_score", nullable = false)
    private Integer teamBScore;

    @NotNull(message = "Winners must be specified")
    @Column(name = "winners", nullable = false, length = 10)
    private String winners;

    // ===== Relationships =====

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_listing_id", nullable = false, unique = true)
    private GameListing gameListing;

    // ===== Constructors =====

    public MatchResult() {
    }

    public MatchResult(GameListing gameListing, Integer teamAScore, Integer teamBScore) {
        this.gameListing = gameListing;
        this.teamAScore = teamAScore;
        this.teamBScore = teamBScore;
        this.winners = calculateWinner(teamAScore, teamBScore);
    }

    // ===== Business Methods =====

    /**
     * Determines the winning team based on scores.
     */
    public static String calculateWinner(int teamAScore, int teamBScore) {
        if (teamAScore > teamBScore) return "TEAM_A";
        if (teamBScore > teamAScore) return "TEAM_B";
        return "DRAW";
    }

    // ===== Getters and Setters =====

    public Long getMatchResultId() {
        return matchResultId;
    }

    public void setMatchResultId(Long matchResultId) {
        this.matchResultId = matchResultId;
    }

    public Integer getTeamAScore() {
        return teamAScore;
    }

    public void setTeamAScore(Integer teamAScore) {
        this.teamAScore = teamAScore;
    }

    public Integer getTeamBScore() {
        return teamBScore;
    }

    public void setTeamBScore(Integer teamBScore) {
        this.teamBScore = teamBScore;
    }

    public String getWinners() {
        return winners;
    }

    public void setWinners(String winners) {
        this.winners = winners;
    }

    public GameListing getGameListing() {
        return gameListing;
    }

    public void setGameListing(GameListing gameListing) {
        this.gameListing = gameListing;
    }
}
