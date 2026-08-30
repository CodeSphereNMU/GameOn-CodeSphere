-- V12: Post Images (Social Feed image attachments)
-- Adds a normalized child table so a post can carry zero to four images.
-- Image binary data is NOT stored in the database; only server-controlled
-- file paths/references are kept here. The 4-image maximum is enforced in the
-- service layer (not via a DB CHECK constraint).

-- ============================================================
-- POST_IMAGES (Depends on: posts)
-- ============================================================
CREATE TABLE post_images (
    post_image_id   BIGINT IDENTITY(1,1) NOT NULL,
    post_id         BIGINT               NOT NULL,
    image_path      VARCHAR(255)         NOT NULL,
    display_order   INT                  NOT NULL DEFAULT 1,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_post_images PRIMARY KEY (post_image_id),
    CONSTRAINT FK_post_images_post FOREIGN KEY (post_id)
        REFERENCES posts(post_id) ON DELETE CASCADE
);
GO

-- Index to fetch a post's images quickly in display order.
CREATE INDEX IX_post_images_post ON post_images (post_id, display_order);
GO
