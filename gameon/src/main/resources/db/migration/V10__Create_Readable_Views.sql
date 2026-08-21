-- Read-only views keep useful business-record IDs and show referenced names.
-- Application tables remain normalized; these views are for human inspection.

CREATE OR ALTER VIEW vw_game_listings_readable AS
SELECT
    gl.game_listing_id,
    creator.username AS creator_username,
    sport.sport_name,
    sf.format_name,
    gl.scheduled_date,
    DATEADD(MINUTE, gl.duration_minutes, gl.scheduled_date) AS ending_time,
    gl.location,
    gl.privacy_setting,
    gl.listing_status,
    CASE
        WHEN gl.listing_status = 'CONFIRMED' AND GETDATE() >= DATEADD(MINUTE, gl.duration_minutes, gl.scheduled_date)
            THEN 'AWAITING_RESULT'
        WHEN gl.listing_status = 'CONFIRMED' AND GETDATE() >= gl.scheduled_date
            THEN 'IN_PROGRESS'
        ELSE gl.listing_status
    END AS effective_status,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.game_listing_id = gl.game_listing_id
       AND gj.status IN ('ACCEPTED', 'LOCKED')) AS current_players,
    gl.skill_level,
    gl.created_at,
    gl.updated_at
FROM game_listings gl
JOIN users creator ON creator.user_id = gl.creator_id
JOIN sport_formats sf ON sf.format_id = gl.format_id
JOIN sports sport ON sport.sport_id = sf.sport_id;
GO

CREATE OR ALTER VIEW vw_active_game_listings AS
SELECT *
FROM vw_game_listings_readable
WHERE listing_status IN ('OPEN', 'CONFIRMED')
  AND ending_time > GETDATE();
GO

CREATE OR ALTER VIEW vw_join_requests_readable AS
SELECT
    jr.join_request_id,
    jr.game_listing_id,
    requester.username AS requester_username,
    creator.username AS creator_username,
    sport.sport_name,
    sf.format_name,
    listing.scheduled_date,
    listing.location,
    jr.team,
    primary_position.position_name AS primary_position_name,
    alternate_position.position_name AS alternate_position_name,
    CASE WHEN jr.invitation_id IS NULL THEN CAST(0 AS BIT) ELSE CAST(1 AS BIT) END AS came_from_invitation,
    jr.status,
    jr.created_at,
    jr.updated_at
FROM join_requests jr
JOIN game_listings listing ON listing.game_listing_id = jr.game_listing_id
JOIN users creator ON creator.user_id = listing.creator_id
JOIN users requester ON requester.user_id = jr.user_id
JOIN sport_formats sf ON sf.format_id = jr.format_id
JOIN sports sport ON sport.sport_id = sf.sport_id
LEFT JOIN positions primary_position ON primary_position.position_id = jr.primary_position_id
LEFT JOIN positions alternate_position ON alternate_position.position_id = jr.alternate_position_id;
GO

CREATE OR ALTER VIEW vw_game_participants_readable AS
SELECT
    gj.game_listing_id,
    participant.username AS participant_username,
    creator.username AS creator_username,
    CASE WHEN gj.user_id = listing.creator_id THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS is_creator,
    sport.sport_name,
    sf.format_name,
    listing.scheduled_date,
    gj.team,
    primary_position.position_name AS primary_position_name,
    alternate_position.position_name AS alternate_position_name,
    gj.status,
    gj.join_request_id,
    gj.created_at,
    gj.updated_at
FROM game_joiners gj
JOIN game_listings listing ON listing.game_listing_id = gj.game_listing_id
JOIN users creator ON creator.user_id = listing.creator_id
JOIN users participant ON participant.user_id = gj.user_id
JOIN sport_formats sf ON sf.format_id = gj.format_id
JOIN sports sport ON sport.sport_id = sf.sport_id
LEFT JOIN positions primary_position ON primary_position.position_id = gj.primary_position_id
LEFT JOIN positions alternate_position ON alternate_position.position_id = gj.alternate_position_id;
GO

CREATE OR ALTER VIEW vw_invitations_readable AS
SELECT
    invitation.invitation_id,
    invitation.game_listing_id,
    invitee.username AS invitee_username,
    creator.username AS creator_username,
    sport.sport_name,
    sf.format_name,
    listing.scheduled_date,
    invitation.status,
    invitation.created_at,
    invitation.updated_at
FROM invitation
JOIN game_listings listing ON listing.game_listing_id = invitation.game_listing_id
JOIN users creator ON creator.user_id = listing.creator_id
JOIN users invitee ON invitee.user_id = invitation.invitee_id
JOIN sport_formats sf ON sf.format_id = listing.format_id
JOIN sports sport ON sport.sport_id = sf.sport_id;
GO

CREATE OR ALTER VIEW vw_match_results_readable AS
SELECT
    result.match_result_id,
    result.game_listing_id,
    sport.sport_name,
    sf.format_name,
    listing.scheduled_date,
    creator.username AS creator_username,
    result.team_a_score,
    result.team_b_score,
    CASE
        WHEN result.team_a_score > result.team_b_score THEN 'TEAM_A'
        WHEN result.team_b_score > result.team_a_score THEN 'TEAM_B'
        ELSE 'DRAW'
    END AS winner,
    result.created_at,
    result.updated_at
FROM match_results result
JOIN game_listings listing ON listing.game_listing_id = result.game_listing_id
JOIN users creator ON creator.user_id = listing.creator_id
JOIN sport_formats sf ON sf.format_id = listing.format_id
JOIN sports sport ON sport.sport_id = sf.sport_id;
GO

CREATE OR ALTER VIEW vw_notifications_readable AS
SELECT
    notification.notification_id,
    recipient.username AS recipient_username,
    notification.notification_type,
    notification.text,
    notification.is_read,
    notification.read_at,
    actor.username AS actor_username,
    notification.game_listing_id,
    listing_sport.sport_name AS listing_sport_name,
    requester.username AS requester_username,
    request.status AS join_request_status,
    result.team_a_score,
    result.team_b_score,
    notification.created_at
FROM notifications notification
JOIN users recipient ON recipient.user_id = notification.recipient_id
LEFT JOIN users actor ON actor.user_id = notification.actor_user_id
LEFT JOIN game_listings listing ON listing.game_listing_id = notification.game_listing_id
LEFT JOIN sport_formats listing_format ON listing_format.format_id = listing.format_id
LEFT JOIN sports listing_sport ON listing_sport.sport_id = listing_format.sport_id
LEFT JOIN join_requests request ON request.join_request_id = notification.join_request_id
LEFT JOIN users requester ON requester.user_id = request.user_id
LEFT JOIN match_results result ON result.match_result_id = notification.match_result_id;
GO

CREATE OR ALTER VIEW vw_user_sport_profiles_readable AS
SELECT
    user_account.username,
    sport.sport_name,
    profile.skill_level,
    profile.wins,
    profile.losses,
    (SELECT COUNT(*) FROM match_results result
     JOIN game_listings listing ON listing.game_listing_id = result.game_listing_id
     JOIN game_joiners participant ON participant.game_listing_id = listing.game_listing_id
     WHERE participant.user_id = profile.user_id
       AND participant.status = 'LOCKED'
       AND result.team_a_score = result.team_b_score
       AND listing.format_id IN (
           SELECT matching_format.format_id FROM sport_formats matching_format
           WHERE matching_format.sport_id = profile.sport_id
       )) AS draws,
    (SELECT COUNT(*) FROM match_results result
     JOIN game_listings listing ON listing.game_listing_id = result.game_listing_id
     JOIN game_joiners participant ON participant.game_listing_id = listing.game_listing_id
     WHERE participant.user_id = profile.user_id
       AND participant.status = 'LOCKED'
       AND listing.format_id IN (
           SELECT matching_format.format_id FROM sport_formats matching_format
           WHERE matching_format.sport_id = profile.sport_id
       )) AS completed_games,
    profile.win_percentage,
    profile.created_at,
    profile.updated_at
FROM user_sport_profiles profile
JOIN users user_account ON user_account.user_id = profile.user_id
JOIN sports sport ON sport.sport_id = profile.sport_id;
GO

CREATE OR ALTER VIEW vw_follows_readable AS
SELECT
    follower.username AS follower_username,
    followed.username AS followed_username,
    follow.created_at
FROM follows follow
JOIN users follower ON follower.user_id = follow.follower_user_id
JOIN users followed ON followed.user_id = follow.followed_user_id;
GO

CREATE OR ALTER VIEW vw_posts_readable AS
SELECT
    post.post_id,
    author.username AS author_username,
    post.content,
    post.privacy_setting,
    CASE
        WHEN post.removed_at IS NULL THEN 'ACTIVE'
        WHEN post.removed_by_user_id IS NULL THEN 'REMOVED_BY_AUTHOR'
        ELSE 'REMOVED_BY_MODERATOR'
    END AS removal_status,
    post.removed_at,
    remover.username AS removed_by_username,
    (SELECT COUNT(*) FROM likes post_like WHERE post_like.post_id = post.post_id) AS like_count,
    (SELECT COUNT(*) FROM comments comment WHERE comment.post_id = post.post_id) AS comment_count,
    post.created_at,
    post.updated_at
FROM posts post
JOIN users author ON author.user_id = post.user_id
LEFT JOIN users remover ON remover.user_id = post.removed_by_user_id;
GO

CREATE OR ALTER VIEW vw_comments_readable AS
SELECT
    comment.comment_id,
    commenter.username AS commenter_username,
    comment.text,
    comment.post_id,
    post_author.username AS post_author_username,
    LEFT(post.content, 100) AS post_context,
    post.removed_at AS post_removed_at,
    comment.created_at,
    comment.updated_at
FROM comments comment
JOIN posts post ON post.post_id = comment.post_id
JOIN users post_author ON post_author.user_id = post.user_id
JOIN users commenter ON commenter.user_id = comment.user_id;
GO

CREATE OR ALTER VIEW vw_likes_readable AS
SELECT
    liked_by.username AS liked_by_username,
    post_like.post_id,
    post_author.username AS post_author_username,
    LEFT(post.content, 100) AS post_context,
    post.removed_at AS post_removed_at,
    post_like.created_at
FROM likes post_like
JOIN posts post ON post.post_id = post_like.post_id
JOIN users post_author ON post_author.user_id = post.user_id
JOIN users liked_by ON liked_by.user_id = post_like.user_id;
GO

CREATE OR ALTER VIEW vw_reports_readable AS
SELECT
    report.report_id,
    reporter.username AS reporter_username,
    reported_user.username AS reported_username,
    report.reported_post_id,
    post_author.username AS reported_post_author_username,
    post.content AS reported_post_content,
    report.report_reason,
    report.description,
    report.status,
    post.removed_at AS reported_post_removed_at,
    post_remover.username AS reported_post_removed_by_username,
    reviewer.username AS reviewed_by_username,
    report.reviewed_at,
    report.created_at,
    report.updated_at
FROM reports report
JOIN users reporter ON reporter.user_id = report.reporter_id
LEFT JOIN users reported_user ON reported_user.user_id = report.reported_user_id
LEFT JOIN posts post ON post.post_id = report.reported_post_id
LEFT JOIN users post_author ON post_author.user_id = post.user_id
LEFT JOIN users post_remover ON post_remover.user_id = post.removed_by_user_id
LEFT JOIN users reviewer ON reviewer.user_id = report.reviewed_by_user_id;
GO

CREATE OR ALTER VIEW vw_user_stats AS
SELECT
    user_account.username,
    (SELECT COUNT(*) FROM follows follow WHERE follow.followed_user_id = user_account.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows follow WHERE follow.follower_user_id = user_account.user_id) AS following_count,
    (SELECT COUNT(*) FROM game_joiners participant
     WHERE participant.user_id = user_account.user_id
       AND participant.status IN ('ACCEPTED', 'LOCKED')) AS games_joined,
    (SELECT COUNT(*) FROM match_results result
     JOIN game_joiners participant ON participant.game_listing_id = result.game_listing_id
     WHERE participant.user_id = user_account.user_id
       AND participant.status = 'LOCKED') AS completed_games,
    (SELECT COUNT(*) FROM posts post
     WHERE post.user_id = user_account.user_id AND post.removed_at IS NULL) AS active_post_count,
    (SELECT COUNT(*) FROM posts post
     WHERE post.user_id = user_account.user_id AND post.removed_at IS NOT NULL) AS removed_post_count
FROM users user_account
WHERE user_account.account_status = 'ACTIVE';
GO
