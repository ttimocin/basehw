-- =====================================================
-- Rate Limiting for Community & Direct Messages
-- =====================================================
-- This migration adds server-side rate limiting using PostgreSQL triggers
-- to prevent spam and abuse even if client-side checks are bypassed.
-- =====================================================

-- ── Community Posts Rate Limit ──────────────────────
-- Maximum 2 posts per 5 minutes (already enforced client-side)

CREATE OR REPLACE FUNCTION check_community_post_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    recent_post_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO recent_post_count
    FROM community_posts
    WHERE author_uid = NEW.author_uid
      AND created_at > NOW() - INTERVAL '5 minutes';

    IF recent_post_count >= 2 THEN
        RAISE EXCEPTION 'Rate limit exceeded: Maximum 2 posts per 5 minutes. Please wait before posting again.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trg_community_post_rate_limit ON community_posts;

CREATE TRIGGER trg_community_post_rate_limit
    BEFORE INSERT ON community_posts
    FOR EACH ROW
    EXECUTE FUNCTION check_community_post_rate_limit();


-- ── Community Comments Rate Limit ───────────────────
-- Maximum 5 comments per 1 minute

CREATE OR REPLACE FUNCTION check_community_comment_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    recent_comment_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO recent_comment_count
    FROM community_comments
    WHERE author_uid = NEW.author_uid
      AND created_at > NOW() - INTERVAL '1 minute';

    IF recent_comment_count >= 5 THEN
        RAISE EXCEPTION 'Rate limit exceeded: Maximum 5 comments per 1 minute. Please wait before commenting again.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trg_community_comment_rate_limit ON community_comments;

CREATE TRIGGER trg_community_comment_rate_limit
    BEFORE INSERT ON community_comments
    FOR EACH ROW
    EXECUTE FUNCTION check_community_comment_rate_limit();


-- ── Direct Messages Rate Limit ──────────────────────
-- Maximum 10 messages per 1 minute

CREATE OR REPLACE FUNCTION check_direct_message_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    recent_message_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO recent_message_count
    FROM messages
    WHERE firebase_uid = NEW.firebase_uid
      AND created_at > NOW() - INTERVAL '1 minute';

    IF recent_message_count >= 10 THEN
        RAISE EXCEPTION 'Rate limit exceeded: Maximum 10 messages per 1 minute. Please wait before sending another message.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trg_direct_message_rate_limit ON messages;

CREATE TRIGGER trg_direct_message_rate_limit
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION check_direct_message_rate_limit();


-- ── Verification Queries ────────────────────────────
-- Run these to verify triggers are created:
-- SELECT tgname, tgrelid::regclass FROM pg_trigger WHERE tgname LIKE 'trg_%_rate_limit';

-- Expected output:
-- tgname                                | tgrelid
-- ----------------------------------------|----------------------
-- trg_community_post_rate_limit          | community_posts
-- trg_community_comment_rate_limit       | community_comments
-- trg_direct_message_rate_limit          | messages