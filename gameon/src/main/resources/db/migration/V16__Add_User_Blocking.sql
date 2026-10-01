-- ============================================================
-- V16: MUTUAL USER BLOCKING
-- ------------------------------------------------------------
-- Stores directional blocks (blocker -> blocked). Mutual visibility/interaction
-- enforcement is done in the service layer (BlockService.isBlockedEitherWay),
-- which treats BLOCK(A,B) OR BLOCK(B,A) as a two-way restriction.
--
-- Historical data (match history, reports, results, audit records) is never
-- deleted by blocking. This table only records the block relationship.
-- Modelled on the existing 'follows' table (composite PK, two FKs to users,
-- ON DELETE NO ACTION to avoid multiple cascade paths on SQL Server, no-self check).
-- ============================================================
CREATE TABLE blocks (
    blocker_user_id     BIGINT    NOT NULL,
    blocked_user_id     BIGINT    NOT NULL,
    created_at          DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_blocks PRIMARY KEY (blocker_user_id, blocked_user_id),
    CONSTRAINT FK_blocks_blocker FOREIGN KEY (blocker_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_blocks_blocked FOREIGN KEY (blocked_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_blocks_no_self CHECK (blocker_user_id != blocked_user_id)
);

-- Indexes to keep the two-directional "block wall" lookups efficient.
CREATE INDEX IX_blocks_blocker ON blocks(blocker_user_id);
CREATE INDEX IX_blocks_blocked ON blocks(blocked_user_id);
